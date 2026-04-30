package service;

import dao.ActivityDAO;
import dao.AssetDAO;
import dao.ReleaseDAO;
import dao.SharingDAO;
import util.DateTimeUtil;

import java.util.List;

public class ReleaseService {
    private final ReleaseDAO releaseDAO = new ReleaseDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();
    private final AssetDAO assetDAO = new AssetDAO();
    private final SharingDAO sharingDAO = new SharingDAO();
    private final dao.VerificationDAO verificationDAO = new dao.VerificationDAO();

    public List<String[]> getConditionsStructured(int userId) {
        return releaseDAO.getReleaseConditionsStructured(userId);
    }
    
    public boolean verifyNomineeIdentity(int releaseId, String answer) {
        int nomineeId = releaseDAO.getNomineeIdByReleaseId(releaseId);
        if (nomineeId == -1) return false;
        
        List<String[]> questions = verificationDAO.getQuestionsByNominee(nomineeId);
        for (String[] q : questions) {
            int vId = Integer.parseInt(q[0]);
            if (verificationDAO.verifyAnswer(vId, answer)) {
                return releaseDAO.updateReleaseStatus(releaseId, "RELEASED_VERIFIED");
            }
        }
        return false;
    }

    public boolean verifyNomineeIdentity(int releaseId, int verificationId, String answer) {
        if (verificationDAO.verifyAnswer(verificationId, answer)) {
            return releaseDAO.updateReleaseStatus(releaseId, "RELEASED_VERIFIED");
        }
        return false;
    }

    public boolean addCondition(int assetId, String type, String value) {
        return releaseDAO.addCondition(assetId, type, value);
    }

    public boolean deleteCondition(int conditionId) {
        return releaseDAO.deleteCondition(conditionId);
    }

    public int triggerManualRelease(int userId, int assetId) {
        List<Integer> nominees = releaseDAO.getNomineesForAsset(assetId);
        int affected = 0;
        for (Integer nomId : nominees) {
            boolean hasQuestions = !verificationDAO.getQuestionsByNominee(nomId).isEmpty();
            String status = hasQuestions ? "PENDING_VERIFICATION" : "RELEASED";
            if (releaseDAO.logRelease(assetId, nomId, status)) {
                affected++;
            }
        }
        activityDAO.logAudit(userId, "Manual release triggered for asset: " + assetId + ". Records: " + affected, DateTimeUtil.now());
        activityDAO.addNotification(userId, "Asset release triggered for asset ID " + assetId, DateTimeUtil.now());
        return affected;
    }

    public void autoReleaseByInactivity(int userId, String lastLogin) {
        if (lastLogin == null || lastLogin.isBlank()) {
            return;
        }
        activityDAO.logAudit(userId, "Checked inactivity based release against last login: " + lastLogin, DateTimeUtil.now());
    }

    public int runInactivityAutoReleaseSweep() {
        int released = releaseDAO.triggerInactivityAutoRelease();
        if (released > 0) {
            for (Integer ownerId : releaseDAO.getOwnersEligibleForInactivityRelease()) {
                activityDAO.addNotification(ownerId, "Auto release executed due to inactivity condition.", DateTimeUtil.now());
                activityDAO.logAudit(ownerId, "Auto release sweep executed", DateTimeUtil.now());
                activityDAO.addSecurityAlert(ownerId, "INACTIVITY_RELEASE", "Assets auto-released after inactivity threshold", DateTimeUtil.now(), "OPEN");
            }
        }
        return released;
    }

    public List<String[]> getReleaseLogs(int userId) {
        return releaseDAO.getReleaseLogs(userId);
    }

    public boolean requestSecureAssetAccess(int nomineeUserId, int nomineeId, int assetId, int ownerQuestionId, String ownerAnswer, String assetPin) {
        String reason = getSecureAssetAccessFailureReason(nomineeUserId, nomineeId, assetId, ownerQuestionId, ownerAnswer, assetPin);
        if (reason != null) return false;
        activityDAO.logAssetAccess(nomineeUserId, assetId, "SECURE_RELEASE_ACCESS", DateTimeUtil.now());
        return true;
    }

    public String getSecureAssetAccessFailureReason(int nomineeUserId, int nomineeId, int assetId, int ownerQuestionId, String ownerAnswer, String assetPin) {
        if (!sharingDAO.isNomineeVerified(nomineeId)) return "Access denied: nominee not verified.";
        if (!sharingDAO.hasAcceptedConsent(assetId, nomineeId)) return "Access denied: consent not accepted.";
        if (!releaseDAO.isReleaseTriggered(assetId, nomineeId)) return "Access denied: release not triggered yet.";
        if (!verificationDAO.verifyOwnerSecurityAnswer(ownerQuestionId, ownerAnswer)) return "Access denied: owner security answer incorrect.";
        if (!assetDAO.verifyAssetPin(assetId, assetPin)) return "Access denied: PIN incorrect or not set for this asset.";
        return null;
    }

    public String getEligibilityStatus(int nomineeId, int assetId) {
        if (!sharingDAO.isNomineeVerified(nomineeId)) {
            return "Blocked: nominee not verified";
        }
        if (!sharingDAO.hasAcceptedConsent(assetId, nomineeId)) {
            return "Blocked: consent not accepted";
        }
        if (!releaseDAO.isReleaseTriggered(assetId, nomineeId)) {
            return "Blocked: release not triggered";
        }
        return "Eligible for challenge";
    }

    public java.util.List<String[]> getOwnerQuestionsForAsset(int assetId) {
        int ownerId = releaseDAO.getOwnerIdForAsset(assetId);
        if (ownerId <= 0) {
            return java.util.Collections.emptyList();
        }
        return verificationDAO.getOwnerSecurityQuestions(ownerId);
    }
}
