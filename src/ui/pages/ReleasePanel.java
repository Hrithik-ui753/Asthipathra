package ui.pages;

import model.Asset;
import model.User;
import service.AssetService;
import service.ReleaseService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ReleasePanel extends JPanel {
    private final ReleaseService releaseService = new ReleaseService();
    private final AssetService assetService = new AssetService();
    private final service.VerificationService verificationService = new service.VerificationService();
    private final User user;
    
    private final DefaultTableModel conditionModel = new DefaultTableModel(new Object[]{"ID", "Asset", "Type", "Value"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable conditionTable = new JTable(conditionModel);
    
    private final DefaultTableModel logModel = new DefaultTableModel(new Object[]{"ID", "Asset", "Nominee", "Time", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable logTable = new JTable(logModel);
    
    private final JComboBox<Asset> assetCombo = new JComboBox<>();
    private final JComboBox<String> condTypeCombo = new JComboBox<>(new String[]{"INACTIVITY", "DATE", "MANUAL"});
    private final JTextField condValueField = new JTextField(10);

    public ReleasePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = UIFactory.titleLabel("Asset Release Management");
        header.add(title, BorderLayout.WEST);
        
        JButton refreshBtn = UIFactory.ghostButton("Refresh Data");
        refreshBtn.addActionListener(e -> refreshData());
        header.add(refreshBtn, BorderLayout.EAST);

        // Control Panel
        JPanel controls = new JPanel(new GridLayout(1, 2, 20, 0));
        controls.setOpaque(false);

        // Manual Trigger Panel
        JPanel manualCard = UIFactory.cardPanel();
        manualCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        manualCard.setBorder(BorderFactory.createTitledBorder("Manual Asset Release"));
        
        loadAssets();
        JButton trigger = UIFactory.primaryButton("Trigger Manual Release");
        trigger.addActionListener(e -> {
            Asset selected = (Asset) assetCombo.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Please select an asset.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to release '" + selected.getAssetName() + "' to all its nominees now?", "Confirm Release", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int released = releaseService.triggerManualRelease(user.getUserId(), selected.getAssetId());
                if (released > 0) {
                    JOptionPane.showMessageDialog(this, "Successfully released to " + released + " nominee(s).", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "No nominees found for this asset. Please share it first.", "Information", JOptionPane.INFORMATION_MESSAGE);
                }
                refreshData();
            }
        });

        manualCard.add(new JLabel("Asset:"));
        manualCard.add(assetCombo);
        manualCard.add(trigger);

        // Add Condition Panel
        JPanel addCondCard = UIFactory.cardPanel();
        addCondCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        addCondCard.setBorder(BorderFactory.createTitledBorder("Add Release Condition"));
        
        JButton addCondBtn = UIFactory.primaryButton("Add Condition");
        addCondBtn.addActionListener(e -> {
            Asset selected = (Asset) assetCombo.getSelectedItem();
            String type = (String) condTypeCombo.getSelectedItem();
            String val = condValueField.getText().trim();
            
            if (selected == null || val.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select asset and enter value.");
                return;
            }
            
            if (releaseService.addCondition(selected.getAssetId(), type, val)) {
                condValueField.setText("");
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add condition.");
            }
        });

        addCondCard.add(new JLabel("Type:"));
        addCondCard.add(condTypeCombo);
        addCondCard.add(new JLabel("Value:"));
        addCondCard.add(condValueField);
        addCondCard.add(addCondBtn);

        controls.add(manualCard);
        controls.add(addCondCard);

        // Tables Section
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        tablesPanel.setOpaque(false);

        // Conditions Table
        JPanel condCard = UIFactory.cardPanel();
        condCard.setLayout(new BorderLayout(5, 5));
        condCard.add(new JLabel("Active Release Conditions"), BorderLayout.NORTH);
        UIFactory.styleTable(conditionTable);
        condCard.add(new JScrollPane(conditionTable), BorderLayout.CENTER);
        
        JButton deleteCondBtn = UIFactory.ghostButton("Delete Selected Condition");
        deleteCondBtn.addActionListener(e -> {
            int row = conditionTable.getSelectedRow();
            if (row < 0) return;
            int id = Integer.parseInt(conditionModel.getValueAt(row, 0).toString());
            if (releaseService.deleteCondition(id)) {
                refreshData();
            }
        });
        condCard.add(deleteCondBtn, BorderLayout.SOUTH);

        // Logs Table
        JPanel logCard = UIFactory.cardPanel();
        logCard.setLayout(new BorderLayout(5, 5));
        logCard.add(new JLabel("Recent Release History"), BorderLayout.NORTH);
        UIFactory.styleTable(logTable);
        logCard.add(new JScrollPane(logTable), BorderLayout.CENTER);
        
        JButton verifyBtn = UIFactory.primaryButton("Verify Identity (Challenge)");
        verifyBtn.addActionListener(e -> {
            int row = logTable.getSelectedRow();
            if (row < 0) return;
            String status = logModel.getValueAt(row, 4).toString();
            if (!status.equals("PENDING_VERIFICATION")) {
                JOptionPane.showMessageDialog(this, "Identity check not required for this status.");
                return;
            }
            int relId = Integer.parseInt(logModel.getValueAt(row, 0).toString());
            // In a real app, the nominee would do this. Here we simulate for the owner.
            // We need the nominee_id to get questions. For now, let's just trigger verification.
            // Simplified: we'll fetch the first question for that nominee.
            // This requires a service method to get nominee id from release id.
            JOptionPane.showMessageDialog(this, "Simulating Nominee Challenge...\nVerification Layer Active.");
            String ans = JOptionPane.showInputDialog(this, "Security Challenge: Enter the answer to the nominee's security question:");
            if (ans != null && !ans.isEmpty()) {
                // To keep it simple, we check against all questions for that release
                // We'll add a helper in ReleaseService
                if (releaseService.verifyNomineeIdentity(relId, ans)) {
                    JOptionPane.showMessageDialog(this, "Verification SUCCESS. Identity Confirmed.");
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Verification FAILED. Access Denied.");
                }
            }
        });
        logCard.add(verifyBtn, BorderLayout.SOUTH);

        tablesPanel.add(condCard);
        tablesPanel.add(logCard);

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 20));
        centerWrapper.setOpaque(false);
        centerWrapper.add(controls, BorderLayout.NORTH);
        centerWrapper.add(tablesPanel, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(centerWrapper, BorderLayout.CENTER);
        refreshData();
    }

    private void loadAssets() {
        assetCombo.removeAllItems();
        for (Asset a : assetService.getAssets(user.getUserId())) {
            assetCombo.addItem(a);
        }
        assetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Asset) setText(((Asset) value).getAssetName());
                return this;
            }
        });
    }

    private void refreshData() {
        conditionModel.setRowCount(0);
        for (String[] row : releaseService.getConditionsStructured(user.getUserId())) {
            conditionModel.addRow(row);
        }
        
        logModel.setRowCount(0);
        for (String[] log : releaseService.getReleaseLogs(user.getUserId())) {
            logModel.addRow(log);
        }
    }
}
