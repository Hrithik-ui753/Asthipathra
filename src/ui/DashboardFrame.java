package ui;

import model.User;
import service.AutoReleaseScheduler;
import ui.pages.ActivityPanel;
import ui.pages.AccessPanel;
import ui.pages.AssetPanel;
import ui.pages.HomePanel;
import ui.pages.NomineePanel;
import ui.pages.NotificationPanel;
import ui.pages.ReleasePanel;
import ui.pages.ReportsAnalyticsPanel;
import ui.pages.SecurityAlertsPanel;
import ui.pages.SharingPanel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class DashboardFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final List<JButton> navButtons = new ArrayList<>();
    private final AutoReleaseScheduler autoReleaseScheduler = new AutoReleaseScheduler();

    public DashboardFrame(User user) {
        setTitle("Asthipathra Dashboard - " + user.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        JPanel menu = new JPanel(new GridLayout(0, 1, 6, 6));
        menu.setPreferredSize(new Dimension(220, 700));
        menu.setBackground(new Color(29, 45, 68));
        menu.setBorder(new javax.swing.border.EmptyBorder(12, 10, 12, 10));

        JLabel branding = new JLabel("Asthipathra");
        branding.setForeground(Color.WHITE);
        branding.setFont(UITheme.SUBTITLE);
        menu.add(branding);

        UIFactory.GradientHeaderPanel header = new UIFactory.GradientHeaderPanel();
        header.setLayout(new BorderLayout());
        JLabel headerText = new JLabel("Secure Digital Asset Management Dashboard");
        headerText.setForeground(Color.WHITE);
        headerText.setFont(UITheme.SUBTITLE);
        header.add(headerText, BorderLayout.WEST);

        addPage("HOME", new HomePanel(user, this::showPage));
        addPage("ASSETS", new AssetPanel(user));
        addPage("NOMINEES", new NomineePanel(user));
        addPage("SHARING", new SharingPanel(user));
        addPage("RELEASE", new ReleasePanel(user));
        addPage("ACCESS", new AccessPanel(user));
        addPage("NOTIFICATIONS", new NotificationPanel(user));
        addPage("ACTIVITY", new ActivityPanel(user));
        addPage("SECURITY", new SecurityAlertsPanel(user));
        addPage("REPORTS", new ReportsAnalyticsPanel(user));

        JButton homeBtn = navButton("Home", "HOME", "🏠");
        menu.add(homeBtn);
        menu.add(navButton("Asset Management", "ASSETS", "📦"));
        menu.add(navButton("Nominees", "NOMINEES", "👥"));
        menu.add(navButton("Asset Sharing", "SHARING", "🔗"));
        menu.add(navButton("Release", "RELEASE", "🔄"));
        menu.add(navButton("Access Unlock", "ACCESS", "🔓"));
        
        service.ActivityService activityService = new service.ActivityService();
        int unread = activityService.getUnreadNotificationCount(user.getUserId());
        String notifText = unread > 0 ? "Notifications (" + unread + ")" : "Notifications";
        menu.add(navButton(notifText, "NOTIFICATIONS", "🔔"));
        
        menu.add(navButton("Activity Logs", "ACTIVITY", "📜"));
        menu.add(navButton("Security Alerts", "SECURITY", "🛡️"));
        menu.add(navButton("Reports & Analytics", "REPORTS", "📊"));

        JButton logout = navButton("Logout", "HOME", "🚪");
        logout.addActionListener(e -> {
            autoReleaseScheduler.stop();
            new LoginFrame().setVisible(true);
            dispose();
        });
        menu.add(logout);

        root.add(menu, BorderLayout.WEST);
        JPanel centerWrap = new JPanel(new BorderLayout(10, 10));
        centerWrap.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));
        centerWrap.setBackground(UITheme.BG_MAIN);
        centerWrap.add(header, BorderLayout.NORTH);
        centerWrap.add(content, BorderLayout.CENTER);
        root.add(centerWrap, BorderLayout.CENTER);
        setContentPane(root);

        setActiveNav(homeBtn);
        autoReleaseScheduler.start();
    }

    private void addPage(String name, JPanel panel) {
        content.add(panel, name);
    }

    private JButton navButton(String text, String pageKey, String icon) {
        JButton b = UIFactory.sidebarButton(text, icon);
        b.setActionCommand(pageKey);
        navButtons.add(b);
        b.addActionListener(e -> {
            showPage(pageKey);
        });
        return b;
    }

    private void showPage(String pageKey) {
        cardLayout.show(content, pageKey);
        for (JButton button : navButtons) {
            if (pageKey.equals(button.getActionCommand())) {
                setActiveNav(button);
                return;
            }
        }
    }

    private void setActiveNav(JButton active) {
        for (JButton button : navButtons) {
            UIFactory.setSidebarSelected(button, button == active);
        }
    }
}
