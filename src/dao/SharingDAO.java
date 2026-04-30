package dao;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SharingDAO {
    public boolean createOrUpdateConsent(int assetId, int nomineeId, String status) {
        String sql = "INSERT INTO Consent(asset_id, nominee_id, status, timestamp) VALUES (?, ?, ?, datetime('now'))";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setInt(2, nomineeId);
            ps.setString(3, status);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean hasAcceptedConsent(int assetId, int nomineeId) {
        String sql = "SELECT 1 FROM Consent WHERE asset_id = ? AND nominee_id = ? AND status = 'Accepted' ORDER BY consent_id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setInt(2, nomineeId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isNomineeVerified(int nomineeId) {
        String sql = "SELECT is_verified FROM Nominees WHERE nominee_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nomineeId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt("is_verified") == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean hasDuplicateNomineeForAsset(int assetId, int nomineeId) {
        String sql = "SELECT 1 FROM Asset_Sharing WHERE asset_id = ? AND nominee_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setInt(2, nomineeId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public double getTotalSharePercentage(int assetId) {
        String sql = "SELECT COALESCE(SUM(share_percentage), 0) total FROM Asset_Sharing WHERE asset_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public int countSharedAssets(int ownerId) {
        String sql = "SELECT COUNT(DISTINCT s.asset_id) total FROM Asset_Sharing s JOIN Assets a ON s.asset_id = a.asset_id WHERE a.owner_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public boolean shareAsset(int assetId, int nomineeId, double percentage, String accessType) {
        String sql = "INSERT INTO Asset_Sharing(asset_id, nominee_id, share_percentage, access_type, condition_type) VALUES (?, ?, ?, ?, 'MANUAL_OR_INACTIVITY')";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ps.setInt(2, nomineeId);
            ps.setDouble(3, percentage);
            ps.setString(4, accessType);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean deleteSharing(int shareId) {
        String sql = "DELETE FROM Asset_Sharing WHERE share_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shareId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public java.util.List<String[]> getSharingDetailsByOwner(int ownerId) {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        String sql = "SELECT s.share_id, a.asset_name, n.name, s.share_percentage, s.access_type, " +
                "n.is_verified, COALESCE((SELECT c.status FROM Consent c WHERE c.asset_id = s.asset_id AND c.nominee_id = s.nominee_id ORDER BY c.consent_id DESC LIMIT 1), 'Pending') consent_status " +
                "FROM Asset_Sharing s " +
                "JOIN Assets a ON s.asset_id = a.asset_id " +
                "JOIN Nominees n ON s.nominee_id = n.nominee_id " +
                "WHERE a.owner_id = ? ORDER BY s.share_id DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("share_id")),
                        rs.getString("asset_name"),
                        rs.getString("name"),
                        String.valueOf(rs.getDouble("share_percentage")),
                        rs.getString("access_type"),
                        rs.getInt("is_verified") == 1 ? "Verified" : "Unverified",
                        rs.getString("consent_status")
                });
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return list;
    }
}
