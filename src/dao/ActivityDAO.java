package dao;

import db.DBConnection;
import model.Notification;
import model.SecurityAlert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityDAO {
    public void logLoginHistory(int userId, String time, String ipAddress) {
        String sql = "INSERT INTO Login_History(user_id, login_time, ip_address) VALUES (?, ?, ?)";
        executeSimpleInsert(sql, userId, time, ipAddress);
    }

    public void logAudit(int userId, String action, String time) {
        String sql = "INSERT INTO Audit_Log(user_id, action, ip_address, device_info, timestamp) VALUES (?, ?, '127.0.0.1', 'Desktop', ?)";
        executeSimpleInsert(sql, userId, action, time);
    }

    public void logAssetAccess(int userId, int assetId, String accessType, String time) {
        String sql = "INSERT INTO Asset_Access_Log(user_id, asset_id, access_type, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, assetId);
            ps.setString(3, accessType);
            ps.setString(4, time);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void addNotification(int userId, String message, String time) {
        String sql = "INSERT INTO Notifications(user_id, message, is_read, priority, created_at) VALUES (?, ?, 0, 'MEDIUM', ?)";
        executeSimpleInsert(sql, userId, message, time);
    }

    public List<Notification> getNotifications(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM Notifications WHERE user_id = ? ORDER BY notification_id DESC LIMIT 20";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification n = new Notification();
                n.setNotificationId(rs.getInt("notification_id"));
                n.setUserId(rs.getInt("user_id"));
                n.setMessage(rs.getString("message"));
                n.setRead(rs.getInt("is_read") == 1);
                n.setCreatedAt(rs.getString("created_at"));
                list.add(n);
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return list;
    }

    public int getUnreadNotificationCount(int userId) {
        String sql = "SELECT COUNT(*) total FROM Notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt("total") : 0;
            rs.close();
            return count;
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<String> getRecentAuditActivities(int userId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT action, timestamp FROM Audit_Log WHERE user_id = ? ORDER BY log_id DESC LIMIT 10";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("timestamp") + " - " + rs.getString("action"));
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return list;
    }

    public void addSecurityAlert(int userId, String activityType, String description, String timestamp, String status) {
        String sql = "INSERT INTO Security_Alerts(user_id, activity_type, description, timestamp, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, activityType);
            ps.setString(3, description);
            ps.setString(4, timestamp);
            ps.setString(5, status);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public List<SecurityAlert> getSecurityAlerts(int userId) {
        List<SecurityAlert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM Security_Alerts WHERE user_id = ? ORDER BY alert_id DESC LIMIT 50";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SecurityAlert alert = new SecurityAlert();
                alert.setAlertId(rs.getInt("alert_id"));
                alert.setUserId(rs.getInt("user_id"));
                alert.setActivityType(rs.getString("activity_type"));
                alert.setDescription(rs.getString("description"));
                alert.setTimestamp(rs.getString("timestamp"));
                alert.setStatus(rs.getString("status"));
                alerts.add(alert);
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return alerts;
    }

    public void markAsRead(int notificationId) {
        String sql = "UPDATE Notifications SET is_read = 1 WHERE notification_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void resolveAlert(int alertId) {
        String sql = "UPDATE Security_Alerts SET status = 'RESOLVED' WHERE alert_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alertId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void addPoints(int userId, int points) {
        String initSql = "INSERT OR IGNORE INTO User_Points(user_id, points) VALUES (?, 0)";
        String updateSql = "UPDATE User_Points SET points = points + ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement psInit = conn.prepareStatement(initSql);
             PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
            psInit.setInt(1, userId);
            psInit.executeUpdate();
            psUpdate.setInt(1, points);
            psUpdate.setInt(2, userId);
            psUpdate.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public int getPoints(int userId) {
        String sql = "SELECT points FROM User_Points WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("points") : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    public void upsertTrustScore(int userId, int score) {
        String sql = "INSERT INTO User_Trust_Score(user_id, score) VALUES (?, ?) " +
                "ON CONFLICT(user_id) DO UPDATE SET score = excluded.score";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, score);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public int getTrustScore(int userId) {
        String sql = "SELECT score FROM User_Trust_Score WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("score") : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    private void executeSimpleInsert(String sql, int userId, String value2, String value3) {
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, value2);
            ps.setString(3, value3);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}
