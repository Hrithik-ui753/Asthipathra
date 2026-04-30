package service;

import dao.ReportDAO;
import model.User;
import util.PDFReportUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final ReportDAO reportDAO = new ReportDAO();

    public Map<String, Integer> analytics(int userId) {
        return reportDAO.getAnalyticsCounts(userId);
    }

    public Map<String, Integer> auditTrend(int userId) {
        return reportDAO.getAuditTrendLast7Days(userId);
    }

    public Map<String, List<String>> fullDetails(int userId) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("Assets", reportDAO.getAssets(userId));
        sections.put("Nominees", reportDAO.getNominees(userId));
        sections.put("Asset Sharing", reportDAO.getShares(userId));
        sections.put("Release Log", reportDAO.getReleaseLogs(userId));
        sections.put("Notifications", reportDAO.getNotifications(userId));
        sections.put("Audit Log", reportDAO.getAuditLogs(userId));
        return sections;
    }

    public Path exportFullReport(User user, Path outputPath) throws IOException {
        PDFReportUtil.writeReport(outputPath, user.getUsername(), analytics(user.getUserId()), fullDetails(user.getUserId()));
        return outputPath;
    }
}
