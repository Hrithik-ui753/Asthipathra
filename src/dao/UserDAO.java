package dao;

import db.DBConnection;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public boolean registerUser(User user) {
        String sql = "INSERT INTO Users(username, email, password_hash, role_id, failed_attempts, account_locked, last_login) VALUES (?, ?, ?, ?, 0, 0, NULL)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setInt(4, user.getRoleId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isUsernameOrEmailTaken(String username, String email) {
        String sql = "SELECT 1 FROM Users WHERE username = ? OR email = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();
            boolean taken = rs.next();
            rs.close();
            return taken;
        } catch (SQLException e) {
            return true; // fail safe
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapUser(rs);
                rs.close();
                return user;
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    public void updateLoginSuccess(int userId, String lastLogin) {
        String sql = "UPDATE Users SET failed_attempts = 0, account_locked = 0, last_login = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lastLogin);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void updateLoginFailure(int userId, int failedAttempts, boolean lockAccount) {
        String sql = "UPDATE Users SET failed_attempts = ?, account_locked = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, failedAttempts);
            ps.setInt(2, lockAccount ? 1 : 0);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRoleId(rs.getInt("role_id"));
        user.setFailedAttempts(rs.getInt("failed_attempts"));
        user.setAccountLocked(rs.getInt("account_locked") == 1);
        user.setLastLogin(rs.getString("last_login"));
        return user;
    }
}
