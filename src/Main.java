import ui.LoginFrame;
import db.DBConnection;
import service.SystemHealthService;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "--health-check".equalsIgnoreCase(args[0])) {
            System.out.println(new SystemHealthService().runBackendHealthCheck());
            return;
        }
        DBConnection.initializeDatabase();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
