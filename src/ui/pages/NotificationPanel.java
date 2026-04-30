package ui.pages;

import model.Notification;
import model.User;
import service.ActivityService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class NotificationPanel extends JPanel {
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Message", "Time", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final ActivityService service = new ActivityService();
    private final User user;

    public NotificationPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_MAIN);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UIFactory.titleLabel("Notifications"), BorderLayout.NORTH);

        JButton refresh = UIFactory.ghostButton("Refresh");
        refresh.addActionListener(e -> loadNotifications());
        
        JButton markRead = UIFactory.primaryButton("Mark as Read");
        markRead.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int id = (int) model.getValueAt(row, 0);
                service.markAsRead(id);
                loadNotifications();
            } else {
                JOptionPane.showMessageDialog(this, "Select a notification first.");
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(markRead);
        actions.add(refresh);
        top.add(actions, BorderLayout.SOUTH);

        JPanel card = UIFactory.cardPanel();
        card.setLayout(new BorderLayout());
        UIFactory.styleTable(table);
        card.add(new JScrollPane(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);

        loadNotifications();
    }

    private void loadNotifications() {
        model.setRowCount(0);
        List<Notification> notifications = service.notifications(user.getUserId());
        for (Notification n : notifications) {
            model.addRow(new Object[]{
                    n.getNotificationId(),
                    n.getMessage(),
                    n.getCreatedAt(),
                    n.isRead() ? "Read" : "Unread"
            });
        }
    }
}
