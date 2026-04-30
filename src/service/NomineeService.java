package service;

import dao.ActivityDAO;
import dao.NomineeDAO;
import model.Nominee;
import util.DateTimeUtil;

import java.util.List;

public class NomineeService {
    private final NomineeDAO nomineeDAO = new NomineeDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();

    public boolean addNominee(String name, String email, String relation, int userId, String accessLevel) {
        Nominee nominee = new Nominee();
        nominee.setName(name);
        nominee.setEmail(email);
        nominee.setRelation(relation);
        nominee.setUserId(userId);
        nominee.setAccessLevel(accessLevel);
        nominee.setVerified(false);
        nominee.setVerificationCode(generateVerificationCode());
        boolean ok = nomineeDAO.addNominee(nominee);
        if (ok) {
            activityDAO.logAudit(userId, "Nominee added: " + name + " (verification required)", DateTimeUtil.now());
            activityDAO.addNotification(userId, "Verification code for " + name + ": " + nominee.getVerificationCode(), DateTimeUtil.now());
            activityDAO.addPoints(userId, 10);
        }
        return ok;
    }

    public List<Nominee> getNominees(int userId) {
        return nomineeDAO.getNomineesByUser(userId);
    }

    public int countNominees(int userId) {
        return nomineeDAO.countNomineesByUser(userId);
    }

    public boolean deleteNominee(int nomineeId, int userId) {
        boolean ok = nomineeDAO.deleteNominee(nomineeId, userId);
        if (ok) {
            activityDAO.logAudit(userId, "Nominee deleted: ID " + nomineeId, DateTimeUtil.now());
        }
        return ok;
    }

    public boolean verifyNominee(int nomineeId, int userId, String verificationCode) {
        boolean ok = nomineeDAO.verifyNominee(nomineeId, userId, verificationCode);
        if (ok) {
            activityDAO.logAudit(userId, "Nominee verified: ID " + nomineeId, DateTimeUtil.now());
            activityDAO.addPoints(userId, 15);
        }
        return ok;
    }

    public String getVerificationCodeForNominee(int nomineeId, int userId) {
        Nominee nominee = nomineeDAO.getNomineeById(nomineeId, userId);
        return nominee == null ? null : nominee.getVerificationCode();
    }

    private String generateVerificationCode() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}
