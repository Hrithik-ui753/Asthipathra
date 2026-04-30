package service;

import dao.ActivityDAO;
import dao.AssetDAO;
import dao.NomineeDAO;
import dao.SharingDAO;
import model.Notification;

import java.util.List;

public class ActivityService {
    private final ActivityDAO activityDAO = new ActivityDAO();
    private final AssetDAO assetDAO = new AssetDAO();
    private final NomineeDAO nomineeDAO = new NomineeDAO();
    private final SharingDAO sharingDAO = new SharingDAO();

    public List<String> recentActivities(int userId) {
        return activityDAO.getRecentAuditActivities(userId);
    }

    public List<Notification> notifications(int userId) {
        return activityDAO.getNotifications(userId);
    }

    public int getUnreadNotificationCount(int userId) {
        return activityDAO.getUnreadNotificationCount(userId);
    }

    public void markAsRead(int notificationId) {
        activityDAO.markAsRead(notificationId);
    }

    public int getPoints(int userId) {
        return activityDAO.getPoints(userId);
    }

    public int getTrustScore(int userId) {
        int assets = assetDAO.countAssetsByOwner(userId);
        int nominees = nomineeDAO.countNomineesByUser(userId);
        int shared = sharingDAO.countSharedAssets(userId);
        int points = activityDAO.getPoints(userId);
        int score = Math.min(100, (assets * 10) + (nominees * 15) + (shared * 20) + (points / 10));
        activityDAO.upsertTrustScore(userId, score);
        return score;
    }
}
