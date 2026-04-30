package service;

import dao.ActivityDAO;
import dao.AssetDAO;
import dao.NomineeDAO;
import dao.UserDAO;
import db.DBConnection;
import model.Asset;
import model.Nominee;
import model.User;
import util.BCryptUtil;
import util.DateTimeUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class SystemHealthService {
    public String runBackendHealthCheck() {
        StringBuilder report = new StringBuilder();
        report.append("Asthipathra Backend Health Check\n");
        report.append("--------------------------------\n");

        try {
            DBConnection.initializeDatabase();
            report.append("[OK] Database initialized\n");
        } catch (Exception e) {
            report.append("[FAIL] Database initialization: ").append(e.getMessage()).append("\n");
            return report.toString();
        }

        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) c FROM sqlite_master WHERE type='table'");
            int tableCount = rs.next() ? rs.getInt("c") : 0;
            rs.close();
            report.append("[OK] SQLite connection active. Tables present: ").append(tableCount).append("\n");
        } catch (Exception e) {
            report.append("[FAIL] SQLite connection test: ").append(e.getMessage()).append("\n");
            return report.toString();
        }

        UserDAO userDAO = new UserDAO();
        AssetDAO assetDAO = new AssetDAO();
        NomineeDAO nomineeDAO = new NomineeDAO();
        ActivityDAO activityDAO = new ActivityDAO();
        AssetService assetService = new AssetService();
        NomineeService nomineeService = new NomineeService();
        SharingService sharingService = new SharingService();
        VerificationService verificationService = new VerificationService();
        ReleaseService releaseService = new ReleaseService();

        String stamp = DateTimeUtil.now().replace(" ", "_").replace(":", "");
        String uname = "health_" + stamp;
        String email = uname + "@asthipathra.local";

        try {
            User u = new User();
            u.setUsername(uname);
            u.setEmail(email);
            u.setPasswordHash(BCryptUtil.hashPassword("Health@123"));
            u.setRoleId(1);
            boolean created = userDAO.registerUser(u);
            report.append(created ? "[OK] UserDAO register\n" : "[FAIL] UserDAO register\n");

            User dbUser = userDAO.findByUsername(uname);
            if (dbUser == null) {
                report.append("[FAIL] UserDAO lookup after register\n");
                return report.toString();
            }
            report.append("[OK] UserDAO lookup\n");

            boolean assetCreated = assetService.addAsset("Health Asset", "DOCUMENT", dbUser.getUserId(), false, "C:/health/file.txt", "12345");
            report.append(assetCreated ? "[OK] AssetDAO add\n" : "[FAIL] AssetDAO add\n");

            List<Asset> assets = assetDAO.getAssetsByOwner(dbUser.getUserId());
            report.append(!assets.isEmpty() ? "[OK] AssetDAO list\n" : "[FAIL] AssetDAO list\n");

            boolean nomineeCreated = nomineeService.addNominee("Health Nominee", "nominee_" + stamp + "@asthipathra.local", "Sibling", dbUser.getUserId(), "READ");
            report.append(nomineeCreated ? "[OK] NomineeDAO add\n" : "[FAIL] NomineeDAO add\n");

            List<Nominee> nominees = nomineeDAO.getNomineesByUser(dbUser.getUserId());
            report.append(!nominees.isEmpty() ? "[OK] NomineeDAO list\n" : "[FAIL] NomineeDAO list\n");

            if (!assets.isEmpty() && !nominees.isEmpty()) {
                Asset latestAsset = assets.get(0);
                Nominee latestNominee = nominees.get(0);
                boolean nomineeVerified = nomineeService.verifyNominee(latestNominee.getNomineeId(), dbUser.getUserId(), latestNominee.getVerificationCode());
                report.append(nomineeVerified ? "[OK] Nominee verification\n" : "[FAIL] Nominee verification\n");

                boolean consentAccepted = sharingService.acceptConsent(dbUser.getUserId(), latestAsset.getAssetId(), latestNominee.getNomineeId());
                report.append(consentAccepted ? "[OK] Consent accepted\n" : "[FAIL] Consent accepted\n");

                boolean shared = sharingService.shareAsset(dbUser.getUserId(), latestAsset.getAssetId(), latestNominee.getNomineeId(), 100.0, "READ");
                report.append(shared ? "[OK] SharingDAO insert\n" : "[FAIL] SharingDAO insert\n");

                if (sharingService.canLockAsset(latestAsset.getAssetId())) {
                    assetService.lockAsset(latestAsset.getAssetId(), dbUser.getUserId());
                }

                boolean questionAdded = verificationService.addOwnerSecurityQuestion(dbUser.getUserId(), "Favorite color?", "blue");
                report.append(questionAdded ? "[OK] Owner security question\n" : "[FAIL] Owner security question\n");

                int releaseRows = releaseService.triggerManualRelease(dbUser.getUserId(), latestAsset.getAssetId());
                report.append(releaseRows > 0 ? "[OK] ReleaseDAO manual release\n" : "[WARN] ReleaseDAO manual release had 0 rows\n");

                List<String[]> ownerQuestions = verificationService.getOwnerSecurityQuestions(dbUser.getUserId());
                if (!ownerQuestions.isEmpty()) {
                    int qId = Integer.parseInt(ownerQuestions.get(0)[0]);
                    boolean accessOk = releaseService.requestSecureAssetAccess(
                            dbUser.getUserId(),
                            latestNominee.getNomineeId(),
                            latestAsset.getAssetId(),
                            qId,
                            "blue",
                            "12345"
                    );
                    report.append(accessOk ? "[OK] Secure access + access log\n" : "[FAIL] Secure access + access log\n");
                } else {
                    report.append("[WARN] Secure access skipped (no owner questions)\n");
                }
            } else {
                report.append("[WARN] Sharing/Release skipped due to missing test data\n");
            }

            activityDAO.logAudit(dbUser.getUserId(), "Health check audit log", DateTimeUtil.now());
            activityDAO.addNotification(dbUser.getUserId(), "Health check notification", DateTimeUtil.now());
            report.append("[OK] ActivityDAO audit + notification\n");
        } catch (Exception e) {
            report.append("[FAIL] DAO/service chain test: ").append(e.getMessage()).append("\n");
            return report.toString();
        }

        report.append("\nFinal Status: BACKEND CHECK COMPLETED\n");
        return report.toString();
    }
}
