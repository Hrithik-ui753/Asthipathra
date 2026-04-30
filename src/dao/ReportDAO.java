package dao;

import db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {
    public Map<String, Integer> getAnalyticsCounts(int userId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Assets", count("SELECT COUNT(*) c FROM Assets WHERE owner_id = ?", userId));
        counts.put("Nominees", count("SELECT COUNT(*) c FROM Nominees WHERE user_id = ?", userId));
        counts.put("Shares", count("SELECT COUNT(*) c FROM Asset_Sharing s JOIN Assets a ON s.asset_id = a.asset_id WHERE a.owner_id = ?", userId));
        counts.put("Releases", count("SELECT COUNT(*) c FROM Release_Log r JOIN Assets a ON r.asset_id = a.asset_id WHERE a.owner_id = ?", userId));
        counts.put("Notifications", count("SELECT COUNT(*) c FROM Notifications WHERE user_id = ?", userId));
        counts.put("Audit Logs", count("SELECT COUNT(*) c FROM Audit_Log WHERE user_id = ?", userId));
        return counts;
    }

    public List<String> getAssets(int userId) {
        return collect("SELECT asset_name, asset_type, is_encrypted, created_at FROM Assets WHERE owner_id = ? ORDER BY asset_id DESC",
                userId,
                rs -> rs.getString("asset_name") + " | " + rs.getString("asset_type") + " | encrypted=" + (rs.getInt("is_encrypted") == 1) + " | " + rs.getString("created_at"));
    }

    public List<String> getNominees(int userId) {
        return collect("SELECT name, email, relation, access_level FROM Nominees WHERE user_id = ? ORDER BY nominee_id DESC",
                userId,
                rs -> rs.getString("name") + " | " + rs.getString("email") + " | " + rs.getString("relation") + " | " + rs.getString("access_level"));
    }

    public List<String> getShares(int userId) {
        return collect("SELECT a.asset_name, n.name nominee_name, s.share_percentage, s.access_type " +
                        "FROM Asset_Sharing s JOIN Assets a ON s.asset_id = a.asset_id JOIN Nominees n ON s.nominee_id = n.nominee_id " +
                        "WHERE a.owner_id = ? ORDER BY s.share_id DESC",
                userId,
                rs -> rs.getString("asset_name") + " -> " + rs.getString("nominee_name") + " | " + rs.getDouble("share_percentage") + "% | " + rs.getString("access_type"));
    }

    public List<String> getReleaseLogs(int userId) {
        return collect("SELECT a.asset_name, n.name nominee_name, r.release_time, r.status " +
                        "FROM Release_Log r JOIN Assets a ON r.asset_id = a.asset_id JOIN Nominees n ON r.nominee_id = n.nominee_id " +
                        "WHERE a.owner_id = ? ORDER BY r.release_id DESC",
                userId,
                rs -> rs.getString("asset_name") + " -> " + rs.getString("nominee_name") + " | " + rs.getString("release_time") + " | " + rs.getString("status"));
    }

    public List<String> getNotifications(int userId) {
        return collect("SELECT message, created_at, is_read FROM Notifications WHERE user_id = ? ORDER BY notification_id DESC",
                userId,
                rs -> rs.getString("created_at") + " | " + rs.getString("message") + " | read=" + (rs.getInt("is_read") == 1));
    }

    public List<String> getAuditLogs(int userId) {
        return collect("SELECT action, timestamp FROM Audit_Log WHERE user_id = ? ORDER BY log_id DESC",
                userId,
                rs -> rs.getString("timestamp") + " | " + rs.getString("action"));
    }

    public Map<String, Integer> getAuditTrendLast7Days(int userId) {
        Map<String, Integer> trend = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String sql = "SELECT strftime('%Y-%m-%d','now','-" + i + " day') d";
            try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    labels.add(rs.getString("d"));
                }
                rs.close();
            } catch (SQLException ignored) {
            }
        }
        if (labels.isEmpty()) {
            return trend;
        }
        Collections.sort(labels);
        for (String label : labels) {
            trend.put(label.substring(5), countByDate(userId, label));
        }
        return trend;
    }

    private int count(String sql, int userId) {
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            int c = rs.next() ? rs.getInt("c") : 0;
            rs.close();
            return c;
        } catch (SQLException e) {
            return 0;
        }
    }

    private int countByDate(int userId, String date) {
        String sql = "SELECT COUNT(*) c FROM Audit_Log WHERE user_id = ? AND substr(timestamp,1,10) = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            ResultSet rs = ps.executeQuery();
            int c = rs.next() ? rs.getInt("c") : 0;
            rs.close();
            return c;
        } catch (SQLException e) {
            return 0;
        }
    }

    private List<String> collect(String sql, int userId, RowMapper mapper) {
        List<String> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(mapper.map(rs));
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return rows;
    }

    @FunctionalInterface
    private interface RowMapper {
        String map(ResultSet rs) throws SQLException;
    }
}
