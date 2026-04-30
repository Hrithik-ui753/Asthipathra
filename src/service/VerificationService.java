package service;

import dao.VerificationDAO;
import java.util.List;

public class VerificationService {
    private final VerificationDAO verificationDAO = new VerificationDAO();

    public boolean addSecurityQuestion(int nomineeId, String question, String answer) {
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) return false;
        return verificationDAO.addQuestion(nomineeId, question, answer);
    }

    public List<String[]> getQuestions(int nomineeId) {
        return verificationDAO.getQuestionsByNominee(nomineeId);
    }

    public boolean deleteQuestion(int id) {
        return verificationDAO.deleteQuestion(id);
    }

    public boolean checkIdentity(int verificationId, String answer) {
        return verificationDAO.verifyAnswer(verificationId, answer);
    }

    public boolean addOwnerSecurityQuestion(int userId, String question, String singleWordAnswer) {
        if (question == null || question.isBlank() || singleWordAnswer == null || singleWordAnswer.isBlank()) {
            return false;
        }
        String normalized = singleWordAnswer.trim();
        if (normalized.contains(" ")) {
            return false;
        }
        return verificationDAO.addOwnerSecurityQuestion(userId, question.trim(), normalized.toLowerCase());
    }

    public List<String[]> getOwnerSecurityQuestions(int userId) {
        return verificationDAO.getOwnerSecurityQuestions(userId);
    }

    public boolean verifyOwnerSecurityAnswer(int questionId, String answer) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        return verificationDAO.verifyOwnerSecurityAnswer(questionId, answer.trim().toLowerCase());
    }
}
