package dao;

import db.DBConnection;
import util.BCryptUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VerificationDAO {
    public boolean addQuestion(int nomineeId, String question, String answer) {
        String sql = "INSERT INTO Nominee_Verification(nominee_id, question, answer_hash) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nomineeId);
            ps.setString(2, question);
            ps.setString(3, BCryptUtil.hashPassword(answer));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<String[]> getQuestionsByNominee(int nomineeId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT verification_id, question FROM Nominee_Verification WHERE nominee_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nomineeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{String.valueOf(rs.getInt("verification_id")), rs.getString("question")});
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return list;
    }

    public boolean deleteQuestion(int id) {
        String sql = "DELETE FROM Nominee_Verification WHERE verification_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean verifyAnswer(int verificationId, String plainAnswer) {
        String sql = "SELECT answer_hash FROM Nominee_Verification WHERE verification_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, verificationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return BCryptUtil.checkPassword(plainAnswer, rs.getString("answer_hash"));
            }
            rs.close();
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean addOwnerSecurityQuestion(int userId, String question, String answer) {
        String sql = "INSERT INTO Security_Questions(user_id, question, answer_hash) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, question);
            ps.setString(3, BCryptUtil.hashPassword(answer));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public List<String[]> getOwnerSecurityQuestions(int userId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT question_id, question FROM Security_Questions WHERE user_id = ? ORDER BY question_id DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{String.valueOf(rs.getInt("question_id")), rs.getString("question")});
            }
        } catch (SQLException ignored) {
        }
        return list;
    }

    public boolean verifyOwnerSecurityAnswer(int questionId, String plainAnswer) {
        String sql = "SELECT answer_hash FROM Security_Questions WHERE question_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return BCryptUtil.checkPassword(plainAnswer, rs.getString("answer_hash"));
            }
        } catch (SQLException ignored) {
        }
        return false;
    }
}
