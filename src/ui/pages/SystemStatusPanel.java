package ui.pages;

import service.SystemHealthService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

public class SystemStatusPanel extends JPanel {
    private final JLabel dbStatus = statusLabel("Database: Not checked", new Color(100, 116, 139));
    private final JLabel daoStatus = statusLabel("DAO Chain: Not checked", new Color(100, 116, 139));
    private final JLabel releaseStatus = statusLabel("Release Engine: Not checked", new Color(100, 116, 139));
    private final JTextArea output = new JTextArea();

    public SystemStatusPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_MAIN);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(UIFactory.titleLabel("System Health Status"), BorderLayout.NORTH);
        JButton runCheck = UIFactory.primaryButton("Run Health Check");
        runCheck.addActionListener(e -> runHealthCheck());
        JPanel runWrap = new JPanel(new BorderLayout());
        runWrap.setOpaque(false);
        runWrap.add(runCheck, BorderLayout.WEST);
        top.add(runWrap, BorderLayout.SOUTH);

        JPanel blocks = new JPanel(new GridLayout(1, 3, 10, 10));
        blocks.setOpaque(false);
        blocks.add(dbStatus);
        blocks.add(daoStatus);
        blocks.add(releaseStatus);

        output.setEditable(false);
        output.setFont(UITheme.BODY);
        output.setBorder(new javax.swing.border.EmptyBorder(8, 8, 8, 8));
        output.setBackground(Color.WHITE);
        output.setText("Click 'Run Health Check' to verify database, JDBC, DAOs and release flow.");

        JPanel outputCard = UIFactory.cardPanel();
        outputCard.setLayout(new BorderLayout());
        outputCard.add(new JScrollPane(output), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(blocks, BorderLayout.NORTH);
        center.add(outputCard, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    private JLabel statusLabel(String text, Color bg) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setOpaque(true);
        label.setBackground(bg);
        label.setForeground(Color.WHITE);
        label.setBorder(new javax.swing.border.EmptyBorder(12, 8, 12, 8));
        return label;
    }

    private void runHealthCheck() {
        String report = new SystemHealthService().runBackendHealthCheck();
        output.setText(report);
        setStatusFromReport(report);
    }

    private void setStatusFromReport(String report) {
        boolean ok = report.contains("Final Status: BACKEND CHECK COMPLETED") && !report.contains("[FAIL]");
        dbStatus.setText(ok ? "Database: Healthy" : "Database: Issue");
        daoStatus.setText(ok ? "DAO Chain: Healthy" : "DAO Chain: Issue");
        releaseStatus.setText(ok ? "Release Engine: Healthy" : "Release Engine: Issue");

        Color green = new Color(22, 163, 74);
        Color red = new Color(220, 38, 38);
        dbStatus.setBackground(ok ? green : red);
        daoStatus.setBackground(ok ? green : red);
        releaseStatus.setBackground(ok ? green : red);
    }
}
