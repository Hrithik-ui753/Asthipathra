package ui.pages;

import model.Asset;
import model.Nominee;
import model.User;
import service.AssetService;
import service.NomineeService;
import service.SharingService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SharingPanel extends JPanel {
    private final JComboBox<Asset> assetCombo = new JComboBox<>();
    private final JComboBox<Nominee> nomineeCombo = new JComboBox<>();
    private final JTextField percentageField = UIFactory.inputField();
    private final JComboBox<String> accessTypeCombo = new JComboBox<>(new String[]{"READ", "DOWNLOAD", "MANAGE"});
    private final DefaultTableModel shareModel = new DefaultTableModel(new Object[]{"ID", "Asset", "Nominee", "%", "Access", "Verified", "Consent"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable shareTable = new JTable(shareModel);
    private final SharingService sharingService = new SharingService();

    public SharingPanel(User user) {
        AssetService assetService = new AssetService();
        NomineeService nomineeService = new NomineeService();

        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);
        top.add(UIFactory.titleLabel("Asset Sharing & Permissions"), BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(350, 0));

        JPanel form = UIFactory.cardPanel();
        form.setLayout(new GridLayout(0, 1, 8, 12));
        form.setBorder(BorderFactory.createTitledBorder("Share New Asset"));
        
        loadData(assetService, nomineeService, user.getUserId());

        form.add(new JLabel("Select Asset"));
        form.add(assetCombo);
        form.add(new JLabel("Select Nominee"));
        form.add(nomineeCombo);
        form.add(new JLabel("Share Percentage (0-100)"));
        form.add(percentageField);
        form.add(new JLabel("Access Type"));
        form.add(accessTypeCombo);

        JButton requestConsentBtn = UIFactory.ghostButton("Request Consent");
        requestConsentBtn.addActionListener(e -> {
            Asset a = (Asset) assetCombo.getSelectedItem();
            Nominee n = (Nominee) nomineeCombo.getSelectedItem();
            if (a == null || n == null) return;
            boolean ok = sharingService.requestConsent(user.getUserId(), a.getAssetId(), n.getNomineeId());
            JOptionPane.showMessageDialog(this, ok ? "Consent set to Pending." : "Failed to create consent.");
        });
        form.add(requestConsentBtn);

        JButton acceptConsentBtn = UIFactory.ghostButton("Mark Consent Accepted");
        acceptConsentBtn.addActionListener(e -> {
            Asset a = (Asset) assetCombo.getSelectedItem();
            Nominee n = (Nominee) nomineeCombo.getSelectedItem();
            if (a == null || n == null) return;
            boolean ok = sharingService.acceptConsent(user.getUserId(), a.getAssetId(), n.getNomineeId());
            JOptionPane.showMessageDialog(this, ok ? "Consent accepted." : "Failed to update consent.");
        });
        form.add(acceptConsentBtn);

        JButton shareBtn = UIFactory.primaryButton("Confirm Share");
        shareBtn.addActionListener(e -> {
            Asset selectedAsset = (Asset) assetCombo.getSelectedItem();
            Nominee selectedNominee = (Nominee) nomineeCombo.getSelectedItem();
            
            if (selectedAsset == null || selectedNominee == null) {
                JOptionPane.showMessageDialog(this, "Please select both an asset and a nominee.");
                return;
            }

            try {
                double pct = Double.parseDouble(percentageField.getText());
                boolean ok = sharingService.shareAsset(
                        user.getUserId(),
                        selectedAsset.getAssetId(),
                        selectedNominee.getNomineeId(),
                        pct,
                        (String) accessTypeCombo.getSelectedItem()
                );
                if (ok) {
                    if (sharingService.canLockAsset(selectedAsset.getAssetId())) {
                        assetService.lockAsset(selectedAsset.getAssetId(), user.getUserId());
                    }
                    JOptionPane.showMessageDialog(this, "Asset shared successfully.");
                    percentageField.setText("");
                    refreshShares(user.getUserId());
                } else {
                    String reason = sharingService.validateShareEligibility(selectedAsset.getAssetId(), selectedNominee.getNomineeId(), pct);
                    JOptionPane.showMessageDialog(this, reason == null ? "Asset sharing failed." : reason);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid numeric percentage.");
            }
        });
        form.add(shareBtn);
        leftPanel.add(form, BorderLayout.NORTH);

        // Right Panel - Table
        JPanel rightPanel = UIFactory.cardPanel();
        rightPanel.setLayout(new BorderLayout(10, 10));
        rightPanel.add(new JLabel("Active Asset Shares"), BorderLayout.NORTH);
        
        UIFactory.styleTable(shareTable);
        rightPanel.add(new JScrollPane(shareTable), BorderLayout.CENTER);
        
        JButton deleteShareBtn = UIFactory.ghostButton("Revoke Selected Access");
        deleteShareBtn.addActionListener(e -> {
            int row = shareTable.getSelectedRow();
            if (row < 0) return;
            int shareId = (int) shareModel.getValueAt(row, 0);
            if (sharingService.revokeShare(shareId)) {
                refreshShares(user.getUserId());
            }
        });
        rightPanel.add(deleteShareBtn, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        
        refreshShares(user.getUserId());
    }

    private void refreshShares(int userId) {
        shareModel.setRowCount(0);
        for (String[] row : sharingService.getSharingDetails(userId)) {
            // Need to map the string array to table objects. 
            // getSharingDetails returns {share_id, asset_name, nominee_name, percentage, access_type}
            shareModel.addRow(new Object[]{
                Integer.parseInt(row[0]), row[1], row[2], row[3] + "%", row[4], row[5], row[6]
            });
        }
    }

    private void loadData(AssetService as, NomineeService ns, int userId) {
        assetCombo.removeAllItems();
        nomineeCombo.removeAllItems();
        
        List<Asset> assets = as.getAssets(userId);
        for (Asset a : assets) assetCombo.addItem(a);
        
        List<Nominee> nominees = ns.getNominees(userId);
        for (Nominee n : nominees) nomineeCombo.addItem(n);
        
        assetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Asset) setText(((Asset) value).getAssetName());
                return this;
            }
        });
        
        nomineeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Nominee) setText(((Nominee) value).getName() + " (" + ((Nominee) value).getRelation() + ")");
                return this;
            }
        });
    }
}
