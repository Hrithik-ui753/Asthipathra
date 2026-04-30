package ui.pages;

import model.User;
import service.ActivityService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ActivityPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Activity Log"}, 0);
    private final JTable table = new JTable(model);

    public ActivityPanel(User user) {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_MAIN);
        ActivityService service = new ActivityService();

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UIFactory.titleLabel("Activity Logs"), BorderLayout.NORTH);

        JButton refresh = UIFactory.ghostButton("Refresh");
        refresh.addActionListener(e -> loadActivities(service, user.getUserId()));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(refresh);
        top.add(actions, BorderLayout.SOUTH);

        JPanel card = UIFactory.cardPanel();
        card.setLayout(new BorderLayout());
        UIFactory.styleTable(table);
        card.add(new JScrollPane(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);

        loadActivities(service, user.getUserId());
    }

    private void loadActivities(ActivityService service, int userId) {
        model.setRowCount(0);
        List<String> activities = service.recentActivities(userId);
        for (String a : activities) {
            model.addRow(new Object[]{a});
        }
    }
}
