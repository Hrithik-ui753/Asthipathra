package ui.pages;

import model.SecurityAlert;
import model.User;
import service.SecurityAlertService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SecurityAlertsPanel extends JPanel {
    private final SecurityAlertService securityAlertService = new SecurityAlertService();
    private final User user;
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Type", "Description", "Time", "Status"}, 0);
    private final JTable table = new JTable(model);

    public SecurityAlertsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_MAIN);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(UIFactory.titleLabel("Security Alerts"), BorderLayout.NORTH);
        JButton refresh = UIFactory.ghostButton("Refresh");
        refresh.addActionListener(e -> loadAlerts());
        
        JButton resolve = UIFactory.primaryButton("Resolve Alert");
        resolve.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int id = (int) model.getValueAt(row, 0);
                securityAlertService.resolveAlert(id);
                loadAlerts();
            } else {
                JOptionPane.showMessageDialog(this, "Select an alert first.");
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(resolve);
        actions.add(refresh);
        top.add(actions, BorderLayout.SOUTH);

        JPanel tableCard = UIFactory.cardPanel();
        tableCard.setLayout(new BorderLayout());
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        UIFactory.styleTable(table);

        add(top, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        loadAlerts();
    }

    private void loadAlerts() {
        model.setRowCount(0);
        List<SecurityAlert> alerts = securityAlertService.getAlerts(user.getUserId());
        for (SecurityAlert alert : alerts) {
            model.addRow(new Object[]{
                    alert.getAlertId(),
                    alert.getActivityType(),
                    alert.getDescription(),
                    alert.getTimestamp(),
                    alert.getStatus()
            });
        }
    }
}
