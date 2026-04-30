package dao;

import db.DBConnection;
import util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReleaseDAO {
    public boolean addCondition(int assetId, String type, String value) {
        String sql = "INSERT INTO Release_Conditions(asset_id, condition_type, condition_value) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setString(2, type);
            ps.setString(3, value);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean deleteCondition(int conditionId) {
        String sql = "DELETE FROM Release_Conditions WHERE condition_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conditionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<String[]> getReleaseConditionsStructured(int ownerId) {
        List<String[]> data = new ArrayList<>();
        String sql = "SELECT rc.condition_id, a.asset_name, rc.condition_type, rc.condition_value " +
                "FROM Release_Conditions rc JOIN Assets a ON rc.asset_id = a.asset_id WHERE a.owner_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new String[]{
                        String.valueOf(rs.getInt("condition_id")),
                        rs.getString("asset_name"),
                        rs.getString("condition_type"),
                        rs.getString("condition_value")
                });
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return data;
    }

    public void addDefaultInactivityCondition(int assetId) {
        String sql = "INSERT INTO Release_Conditions(asset_id, condition_type, condition_value) VALUES (?, 'INACTIVITY', '180')";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public List<Integer> getNomineesForAsset(int assetId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT nominee_id FROM Asset_Sharing WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getInt("nominee_id"));
        } catch (SQLException ignored) {}
        return list;
    }

    public boolean logRelease(int assetId, int nomineeId, String status) {
        String sql = "INSERT INTO Release_Log(asset_id, nominee_id, release_time, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setInt(2, nomineeId);
            ps.setString(3, DateTimeUtil.now());
            ps.setString(4, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean updateReleaseStatus(int releaseId, String status) {
        String sql = "UPDATE Release_Log SET status = ? WHERE release_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, releaseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public int triggerManualReleaseByAsset(int assetId) {
        int affected = 0;
        String fetch = "SELECT nominee_id FROM Asset_Sharing WHERE asset_id = ?";
        String log = "INSERT INTO Release_Log(asset_id, nominee_id, release_time, status) VALUES (?, ?, ?, 'RELEASED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement psFetch = conn.prepareStatement(fetch);
             PreparedStatement psLog = conn.prepareStatement(log)) {
            psFetch.setInt(1, assetId);
            ResultSet rs = psFetch.executeQuery();
            while (rs.next()) {
                psLog.setInt(1, assetId);
                psLog.setInt(2, rs.getInt("nominee_id"));
                psLog.setString(3, DateTimeUtil.now());
                affected += psLog.executeUpdate();
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return affected;
    }

    public int getNomineeIdByReleaseId(int releaseId) {
        String sql = "SELECT nominee_id FROM Release_Log WHERE release_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, releaseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("nominee_id");
        } catch (SQLException ignored) {}
        return -1;
    }

    public int triggerInactivityAutoRelease() {
        // We update the sub-query to handle verification status
        String sql = "INSERT INTO Release_Log(asset_id, nominee_id, release_time, status) " +
                "SELECT s.asset_id, s.nominee_id, ?, " +
                "CASE WHEN EXISTS (SELECT 1 FROM Nominee_Verification nv WHERE nv.nominee_id = s.nominee_id) " +
                "THEN 'PENDING_VERIFICATION' ELSE 'AUTO_RELEASED_INACTIVITY' END " +
                "FROM Asset_Sharing s " +
                "JOIN Assets a ON s.asset_id = a.asset_id " +
                "JOIN Release_Conditions rc ON rc.asset_id = a.asset_id AND rc.condition_type = 'INACTIVITY' " +
                "JOIN Users u ON a.owner_id = u.user_id " +
                "WHERE CAST(rc.condition_value AS INTEGER) <= CAST((julianday('now') - julianday(COALESCE(u.last_login, '1970-01-01 00:00:00'))) AS INTEGER) " +
                "AND NOT EXISTS ( " +
                "SELECT 1 FROM Release_Log rl WHERE rl.asset_id = s.asset_id AND rl.nominee_id = s.nominee_id " +
                ")";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateTimeUtil.now());
            return ps.executeUpdate();
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<Integer> getOwnersEligibleForInactivityRelease() {
        List<Integer> owners = new ArrayList<>();
        String sql = "SELECT DISTINCT a.owner_id FROM Assets a " +
                "JOIN Release_Conditions rc ON rc.asset_id = a.asset_id AND rc.condition_type = 'INACTIVITY' " +
                "JOIN Users u ON a.owner_id = u.user_id " +
                "WHERE CAST(rc.condition_value AS INTEGER) <= CAST((julianday('now') - julianday(COALESCE(u.last_login, '1970-01-01 00:00:00'))) AS INTEGER)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                owners.add(rs.getInt("owner_id"));
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return owners;
    }

    public List<String[]> getReleaseLogs(int ownerId) {
        List<String[]> logs = new ArrayList<>();
        String sql = "SELECT rl.release_id, a.asset_name, n.name, rl.release_time, rl.status " +
                "FROM Release_Log rl " +
                "JOIN Assets a ON rl.asset_id = a.asset_id " +
                "JOIN Nominees n ON rl.nominee_id = n.nominee_id " +
                "WHERE a.owner_id = ? " +
                "ORDER BY rl.release_id DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(new String[]{
                        String.valueOf(rs.getInt("release_id")),
                        rs.getString("asset_name"),
                        rs.getString("name"),
                        rs.getString("release_time"),
                        rs.getString("status")
                });
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return logs;
    }

    public boolean isReleaseTriggered(int assetId, int nomineeId) {
        // "PENDING_VERIFICATION" still means the release conditions were triggered; the nominee can proceed to the
        // secure PIN + owner-question access challenge defined by the new inheritance model.
        String sql = "SELECT 1 FROM Release_Log WHERE asset_id = ? AND nominee_id = ? AND status IN ('RELEASED', 'RELEASED_VERIFIED', 'AUTO_RELEASED_INACTIVITY', 'PENDING_VERIFICATION')";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setInt(2, nomineeId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public int getOwnerIdForAsset(int assetId) {
        String sql = "SELECT owner_id FROM Assets WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("owner_id");
            }
        } catch (SQLException ignored) {
        }
        return -1;
    }
}
