package ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UIFactory {
    private UIFactory() {
    }

    public static JPanel paddedPanel(int t, int l, int b, int r) {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(t, l, b, r));
        panel.setBackground(UITheme.BG_MAIN);
        return panel;
    }

    public static JPanel cardPanel() {
        JPanel card = new RoundedPanel(18);
        card.setBackground(UITheme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 240)),
                new EmptyBorder(16, 16, 16, 16))
        );
        return card;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(UITheme.ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 14, 10, 14));
        addHoverEffect(button, UITheme.ACCENT, new Color(36, 74, 214));
        return button;
    }

    public static JButton ghostButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(UITheme.ACCENT_DARK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 236)));
        button.setPreferredSize(new Dimension(120, 36));
        addHoverEffect(button, Color.WHITE, new Color(241, 246, 255));
        return button;
    }

    public static JButton sidebarButton(String text, String icon) {
        JButton button = new JButton(icon + "  " + text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(new Color(41, 58, 85));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBorder(new EmptyBorder(10, 12, 10, 10));
        addHoverEffect(button, new Color(41, 58, 85), new Color(55, 74, 106));
        return button;
    }

    public static void setSidebarSelected(JButton button, boolean selected) {
        button.setBackground(selected ? UITheme.ACCENT : new Color(41, 58, 85));
        button.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 14));
        button.putClientProperty("sidebar.selected", selected);
    }

    private static void addHoverEffect(JButton button, Color normal, Color hover) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Object selected = button.getClientProperty("sidebar.selected");
                if (!(selected instanceof Boolean) || !((Boolean) selected)) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Object selected = button.getClientProperty("sidebar.selected");
                if (!(selected instanceof Boolean) || !((Boolean) selected)) {
                    button.setBackground(normal);
                }
            }
        });
    }

    public static JTextField inputField() {
        JTextField field = new JTextField();
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 215, 230)),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    public static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.TITLE);
        label.setForeground(UITheme.ACCENT_DARK);
        return label;
    }

    public static JPanel wrapNorth(JPanel child) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(child, BorderLayout.NORTH);
        return panel;
    }

    public static JPanel statusCard(String title, String value, Color color) {
        JPanel card = new RoundedPanel(18);
        card.setLayout(new BorderLayout());
        card.setBackground(color);
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(28);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(234, 238, 246));
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(47, 84, 235));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    public static class RoundedPanel extends JPanel {
        private final int arc;

        public RoundedPanel(int arc) {
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class GradientHeaderPanel extends JPanel {
        public GradientHeaderPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(12, 16, 12, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, UITheme.ACCENT, getWidth(), getHeight(), UITheme.ACCENT_DARK);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
