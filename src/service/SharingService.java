package service;

import dao.ActivityDAO;
import dao.SharingDAO;
import util.DateTimeUtil;

public class SharingService {
    private final SharingDAO sharingDAO = new SharingDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();

    public boolean requestConsent(int userId, int assetId, int nomineeId) {
        boolean ok = sharingDAO.createOrUpdateConsent(assetId, nomineeId, "Pending");
        if (ok) {
            activityDAO.logAudit(userId, "Consent requested for asset " + assetId + " nominee " + nomineeId, DateTimeUtil.now());
        }
        return ok;
    }

    public boolean acceptConsent(int userId, int assetId, int nomineeId) {
        boolean ok = sharingDAO.createOrUpdateConsent(assetId, nomineeId, "Accepted");
        if (ok) {
            activityDAO.logAudit(userId, "Consent accepted for asset " + assetId + " nominee " + nomineeId, DateTimeUtil.now());
        }
        return ok;
    }

    public String validateShareEligibility(int assetId, int nomineeId, double percentage) {
        if (!sharingDAO.isNomineeVerified(nomineeId)) {
            return "Nominee must be verified before sharing.";
        }
        if (!sharingDAO.hasAcceptedConsent(assetId, nomineeId)) {
            return "Consent must be accepted before sharing.";
        }
        if (sharingDAO.hasDuplicateNomineeForAsset(assetId, nomineeId)) {
            return "Duplicate nominee assignment is not allowed.";
        }
        double total = sharingDAO.getTotalSharePercentage(assetId);
        if (total + percentage > 100.0) {
            return "Total share percentage cannot exceed 100%.";
        }
        return null;
    }

    public boolean shareAsset(int userId, int assetId, int nomineeId, double percentage, String accessType) {
        String validation = validateShareEligibility(assetId, nomineeId, percentage);
        if (validation != null) {
            activityDAO.addSecurityAlert(userId, "SHARE_BLOCKED", validation, DateTimeUtil.now(), "OPEN");
            return false;
        }
        boolean ok = sharingDAO.shareAsset(assetId, nomineeId, percentage, accessType);
        if (ok) {
            activityDAO.logAudit(userId, "Asset shared. Asset ID: " + assetId + " Nominee ID: " + nomineeId, DateTimeUtil.now());
            activityDAO.addNotification(userId, "Asset shared successfully.", DateTimeUtil.now());
            activityDAO.addPoints(userId, 20);
        }
        return ok;
    }

    public boolean revokeShare(int shareId) {
        return sharingDAO.deleteSharing(shareId);
    }

    public java.util.List<String[]> getSharingDetails(int userId) {
        return sharingDAO.getSharingDetailsByOwner(userId);
    }

    public int countSharedAssets(int userId) {
        return sharingDAO.countSharedAssets(userId);
    }

    public boolean canLockAsset(int assetId) {
        double total = sharingDAO.getTotalSharePercentage(assetId);
        return Math.abs(total - 100.0) < 0.0001;
    }
}
