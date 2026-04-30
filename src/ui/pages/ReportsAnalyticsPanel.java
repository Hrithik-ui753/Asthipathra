package ui.pages;

import model.User;
import service.ReportService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportsAnalyticsPanel extends JPanel {
    private final User user;
    private final ReportService reportService = new ReportService();
    private final AnalyticsBarChartPanel barChartPanel = new AnalyticsBarChartPanel();
    private final AuditTrendPanel trendPanel = new AuditTrendPanel();
    private final JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 10, 10));
    private final DefaultTableModel detailModel = new DefaultTableModel(new Object[]{"Category", "Details"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable detailTable = new JTable(detailModel);

    public ReportsAnalyticsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_MAIN);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(UIFactory.titleLabel("Reports & Analytics"), BorderLayout.NORTH);
        JButton refreshBtn = UIFactory.ghostButton("Refresh Analytics");
        JButton downloadBtn = UIFactory.primaryButton("Download Full Report PDF");
        refreshBtn.addActionListener(e -> loadData());
        downloadBtn.addActionListener(e -> exportPdf());
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(downloadBtn);
        top.add(actions, BorderLayout.SOUTH);

        kpiPanel.setOpaque(false);

        UIFactory.styleTable(detailTable);
        JPanel detailsCard = UIFactory.cardPanel();
        detailsCard.setLayout(new BorderLayout());
        detailsCard.add(new JScrollPane(detailTable), BorderLayout.CENTER);

        JPanel chartsWrap = new JPanel(new GridLayout(1, 2, 10, 10));
        chartsWrap.setOpaque(false);
        JPanel barCard = UIFactory.cardPanel();
        barCard.setLayout(new BorderLayout());
        barCard.add(barChartPanel, BorderLayout.CENTER);
        JPanel trendCard = UIFactory.cardPanel();
        trendCard.setLayout(new BorderLayout());
        trendCard.add(trendPanel, BorderLayout.CENTER);
        chartsWrap.add(barCard);
        chartsWrap.add(trendCard);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartsWrap, detailsCard);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.45);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(kpiPanel, BorderLayout.NORTH);
        center.add(splitPane, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        loadData();
    }

    private void loadData() {
        Map<String, Integer> analytics = reportService.analytics(user.getUserId());
        Map<String, Integer> trend = reportService.auditTrend(user.getUserId());
        Map<String, List<String>> details = reportService.fullDetails(user.getUserId());
        barChartPanel.setData(analytics);
        trendPanel.setData(trend);
        renderKpis(analytics);

        detailModel.setRowCount(0);
        for (Map.Entry<String, List<String>> section : details.entrySet()) {
            if (section.getValue().isEmpty()) {
                detailModel.addRow(new Object[]{section.getKey(), "No records"});
            } else {
                for (String row : section.getValue()) {
                    detailModel.addRow(new Object[]{section.getKey(), row});
                }
            }
        }
    }

    private void renderKpis(Map<String, Integer> analytics) {
        kpiPanel.removeAll();
        kpiPanel.add(UIFactory.statusCard("Assets", String.valueOf(analytics.getOrDefault("Assets", 0)), new Color(47, 84, 235)));
        kpiPanel.add(UIFactory.statusCard("Nominees", String.valueOf(analytics.getOrDefault("Nominees", 0)), new Color(22, 163, 74)));
        kpiPanel.add(UIFactory.statusCard("Shares", String.valueOf(analytics.getOrDefault("Shares", 0)), new Color(245, 158, 11)));
        kpiPanel.add(UIFactory.statusCard("Releases", String.valueOf(analytics.getOrDefault("Releases", 0)), new Color(220, 38, 38)));
        kpiPanel.revalidate();
        kpiPanel.repaint();
    }

    private void exportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("Asthipathra_Report_" + user.getUsername() + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        try {
            Path output = chooser.getSelectedFile().toPath();
            if (!output.toString().toLowerCase().endsWith(".pdf")) {
                output = Path.of(output.toString() + ".pdf");
            }
            reportService.exportFullReport(user, output);
            JOptionPane.showMessageDialog(this, "PDF exported successfully:\n" + output);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export PDF: " + ex.getMessage());
        }
    }

    private static class AnalyticsBarChartPanel extends JPanel {
        private Map<String, Integer> data = new LinkedHashMap<>();
        private final Color[] colors = new Color[]{
                new Color(47, 84, 235),
                new Color(22, 163, 74),
                new Color(245, 158, 11),
                new Color(220, 38, 38),
                new Color(99, 102, 241),
                new Color(20, 184, 166)
        };

        public AnalyticsBarChartPanel() {
            setPreferredSize(new Dimension(700, 250));
            setBackground(Color.WHITE);
        }

        public void setData(Map<String, Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int baseY = height - 35;
            int left = 50;
            int barWidth = Math.max(30, (width - 80) / Math.max(1, data.size()) - 14);
            int max = 1;
            for (int value : data.values()) max = Math.max(max, value);

            int i = 0;
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                int x = left + i * (barWidth + 14);
                int barHeight = (int) ((height - 80) * (e.getValue() / (double) max));
                int y = baseY - barHeight;
                g2.setColor(colors[i % colors.length]);
                g2.fillRoundRect(x, y, barWidth, barHeight, 10, 10);
                g2.setColor(new Color(31, 41, 55));
                g2.drawString(String.valueOf(e.getValue()), x + (barWidth / 2) - 8, y - 6);
                g2.drawString(e.getKey(), x, baseY + 18);
                i++;
            }
            g2.setColor(new Color(148, 163, 184));
            g2.drawLine(40, baseY, width - 20, baseY);
            g2.dispose();
        }
    }

    private static class AuditTrendPanel extends JPanel {
        private Map<String, Integer> data = new LinkedHashMap<>();

        public AuditTrendPanel() {
            setPreferredSize(new Dimension(700, 250));
            setBackground(Color.WHITE);
        }

        public void setData(Map<String, Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int left = 40;
            int top = 25;
            int bottom = height - 35;
            int right = width - 25;
            int plotW = right - left;
            int plotH = bottom - top;

            int max = 1;
            for (int value : data.values()) max = Math.max(max, value);

            int n = data.size();
            int idx = 0;
            int prevX = -1, prevY = -1;
            g2.setColor(new Color(59, 130, 246));
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                int x = left + (int) ((idx / (double) Math.max(1, n - 1)) * plotW);
                int y = bottom - (int) ((e.getValue() / (double) max) * plotH);
                if (prevX >= 0) {
                    g2.drawLine(prevX, prevY, x, y);
                }
                g2.fillOval(x - 4, y - 4, 8, 8);
                g2.setColor(new Color(15, 23, 42));
                g2.drawString(String.valueOf(e.getValue()), x - 5, y - 8);
                g2.drawString(e.getKey(), x - 15, bottom + 16);
                g2.setColor(new Color(59, 130, 246));
                prevX = x;
                prevY = y;
                idx++;
            }
            g2.setColor(new Color(148, 163, 184));
            g2.drawRect(left, top, plotW, plotH);
            g2.setColor(new Color(30, 41, 59));
            g2.drawString("Last 7 days audit activity trend", left, 16);
            g2.dispose();
        }
    }
}
