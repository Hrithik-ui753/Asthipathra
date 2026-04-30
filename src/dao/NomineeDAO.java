package dao;

import db.DBConnection;
import model.Nominee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NomineeDAO {
    public boolean addNominee(Nominee nominee) {
        String sql = "INSERT INTO Nominees(name, email, relation, user_id, access_level, is_verified, verification_code) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nominee.getName());
            ps.setString(2, nominee.getEmail());
            ps.setString(3, nominee.getRelation());
            ps.setInt(4, nominee.getUserId());
            ps.setString(5, nominee.getAccessLevel());
            ps.setInt(6, nominee.isVerified() ? 1 : 0);
            ps.setString(7, nominee.getVerificationCode());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<Nominee> getNomineesByUser(int userId) {
        List<Nominee> nominees = new ArrayList<>();
        String sql = "SELECT * FROM Nominees WHERE user_id = ? ORDER BY nominee_id DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Nominee nominee = new Nominee();
                nominee.setNomineeId(rs.getInt("nominee_id"));
                nominee.setName(rs.getString("name"));
                nominee.setEmail(rs.getString("email"));
                nominee.setRelation(rs.getString("relation"));
                nominee.setUserId(rs.getInt("user_id"));
                nominee.setAccessLevel(rs.getString("access_level"));
                nominee.setVerified(rs.getInt("is_verified") == 1);
                nominee.setVerificationCode(rs.getString("verification_code"));
                nominees.add(nominee);
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return nominees;
    }

    public int countNomineesByUser(int userId) {
        String sql = "SELECT COUNT(*) total FROM Nominees WHERE user_id = ?";
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

    public boolean deleteNominee(int nomineeId, int userId) {
        String sql = "DELETE FROM Nominees WHERE nominee_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nomineeId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public Nominee getNomineeById(int nomineeId, int userId) {
        String sql = "SELECT * FROM Nominees WHERE nominee_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nomineeId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Nominee nominee = new Nominee();
                nominee.setNomineeId(rs.getInt("nominee_id"));
                nominee.setName(rs.getString("name"));
                nominee.setEmail(rs.getString("email"));
                nominee.setRelation(rs.getString("relation"));
                nominee.setUserId(rs.getInt("user_id"));
                nominee.setAccessLevel(rs.getString("access_level"));
                nominee.setVerified(rs.getInt("is_verified") == 1);
                nominee.setVerificationCode(rs.getString("verification_code"));
                return nominee;
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    public boolean verifyNominee(int nomineeId, int userId, String code) {
        String sql = "UPDATE Nominees SET is_verified = 1 WHERE nominee_id = ? AND user_id = ? AND verification_code = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nomineeId);
            ps.setInt(2, userId);
            ps.setString(3, code);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}
