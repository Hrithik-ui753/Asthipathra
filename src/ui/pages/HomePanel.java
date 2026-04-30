package ui.pages;

import model.Notification;
import model.User;
import service.ActivityService;
import service.AssetService;
import service.NomineeService;
import service.SharingService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.function.Consumer;

public class HomePanel extends JPanel {
    public HomePanel(User user, Consumer<String> onNavigate) {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_MAIN);

        AssetService assetService = new AssetService();
        NomineeService nomineeService = new NomineeService();
        ActivityService activityService = new ActivityService();
        SharingService sharingService = new SharingService();

        JLabel welcome = new JLabel("Welcome, " + user.getUsername() + "!");
        welcome.setFont(UITheme.TITLE);

        String lastLogin = user.getLastLogin();
        String status = "Inactive";
        if (lastLogin != null && !lastLogin.isEmpty()) {
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(lastLogin, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (dt.isAfter(java.time.LocalDateTime.now().minusDays(30))) {
                    status = "Active";
                }
            } catch (Exception ignored) { }
        }
        
        JLabel loginLabel = new JLabel("Last Login: " + (lastLogin != null ? lastLogin : "First Login") + " | Status: " + status);
        loginLabel.setFont(UITheme.BODY);
        if ("Active".equals(status)) loginLabel.setForeground(new Color(22, 163, 74));
        else loginLabel.setForeground(Color.RED);
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(welcome, BorderLayout.NORTH);
        headerPanel.add(loginLabel, BorderLayout.SOUTH);

        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 10));
        stats.setOpaque(false);
        JPanel aCard = UIFactory.statusCard("Total Assets", String.valueOf(assetService.countAssets(user.getUserId())), new Color(47, 84, 235));
        JPanel nCard = UIFactory.statusCard("Total Nominees", String.valueOf(nomineeService.countNominees(user.getUserId())), new Color(22, 163, 74));
        JPanel sCard = UIFactory.statusCard("Shared Assets", String.valueOf(sharingService.countSharedAssets(user.getUserId())), new Color(245, 158, 11));
        int trustScore = activityService.getTrustScore(user.getUserId());
        int points = activityService.getPoints(user.getUserId());
        JPanel hCard = UIFactory.statusCard("Trust Score", trustScore + " (Pts: " + points + ")", new Color(139, 92, 246));
        
        stats.add(aCard);
        stats.add(nCard);
        stats.add(sCard);
        stats.add(hCard);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(headerPanel, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);

        // Quick Actions
        JPanel quickActions = UIFactory.cardPanel();
        quickActions.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        quickActions.add(new JLabel("Quick Actions:"));
        
        JButton btnAddAsset = UIFactory.ghostButton("Add New Asset");
        JButton btnAddNominee = UIFactory.ghostButton("Manage Nominees");
        JButton btnViewLogs = UIFactory.ghostButton("Security Logs");
        btnAddAsset.addActionListener(e -> onNavigate.accept("ASSETS"));
        btnAddNominee.addActionListener(e -> onNavigate.accept("NOMINEES"));
        btnViewLogs.addActionListener(e -> onNavigate.accept("SECURITY"));
        
        quickActions.add(btnAddAsset);
        quickActions.add(btnAddNominee);
        quickActions.add(btnViewLogs);
        
        top.add(quickActions, BorderLayout.SOUTH);

        JPanel feedCard = UIFactory.cardPanel();
        feedCard.setLayout(new BorderLayout());
        
        String[] columns = {"Type", "Details", "Timestamp"};
        DefaultTableModel feedModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable feedTable = new JTable(feedModel);
        UIFactory.styleTable(feedTable);

        // Load activities
        for (String log : activityService.recentActivities(user.getUserId())) {
            String[] parts = log.split(" - ", 2);
            if (parts.length == 2) {
                feedModel.addRow(new Object[]{"ACTIVITY", parts[1], parts[0]});
            } else {
                feedModel.addRow(new Object[]{"ACTIVITY", log, ""});
            }
        }

        // Load notifications
        for (Notification n : activityService.notifications(user.getUserId())) {
            feedModel.addRow(new Object[]{"NOTIFICATION", n.getMessage(), n.getCreatedAt()});
        }

        feedCard.add(new JScrollPane(feedTable), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(feedCard, BorderLayout.CENTER);
    }
}
