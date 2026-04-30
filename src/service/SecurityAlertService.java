package service;

import dao.ActivityDAO;
import model.SecurityAlert;

import java.util.List;

public class SecurityAlertService {
    private final ActivityDAO activityDAO = new ActivityDAO();

    public List<SecurityAlert> getAlerts(int userId) {
        return activityDAO.getSecurityAlerts(userId);
    }

    public void resolveAlert(int alertId) {
        activityDAO.resolveAlert(alertId);
    }
}
