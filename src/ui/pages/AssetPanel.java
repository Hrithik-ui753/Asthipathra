package ui.pages;

import model.Asset;
import model.User;
import service.AssetService;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import ui.UIFactory;
import ui.UITheme;

public class AssetPanel extends JPanel {
    private final AssetService assetService = new AssetService();
    private final User user;
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Type", "Lock", "Nominees", "File"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JTextField nameField = UIFactory.inputField();
    private final JTextField typeField = UIFactory.inputField();
    private final JTextField fileField = UIFactory.inputField();
    private final JTextField pinField = UIFactory.inputField();
    private final JCheckBox encryptedBox = new JCheckBox("Encrypt File URL");
    
    private final JTextField searchField = UIFactory.inputField();
    private final javax.swing.JComboBox<String> typeFilter = new javax.swing.JComboBox<>(new String[]{"All Types", "Document", "Image", "Video", "Financial", "Other"});

    public AssetPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BG_MAIN);
        JLabel title = UIFactory.titleLabel("Asset Management");

        JPanel form = UIFactory.cardPanel();
        form.setLayout(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Asset Name"));
        form.add(nameField);
        form.add(new JLabel("Asset Type"));
        form.add(typeField);
        form.add(new JLabel("File Path/URL"));
        form.add(fileField);
        form.add(new JLabel("Asset PIN (5 digits)"));
        form.add(pinField);
        form.add(new JLabel("Security"));
        form.add(encryptedBox);

        JButton addBtn = UIFactory.primaryButton("Add Asset");
        JButton deleteBtn = UIFactory.ghostButton("Delete Selected");
        JButton openFileBtn = UIFactory.ghostButton("Open Selected File");
        addBtn.addActionListener(e -> addAsset());
        deleteBtn.addActionListener(e -> deleteAsset());
        openFileBtn.addActionListener(e -> openFile());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.add(addBtn);
        actions.add(openFileBtn);
        actions.add(deleteBtn);
        top.add(actions, BorderLayout.SOUTH);

        JPanel tableCard = UIFactory.cardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));
        
        JPanel filterPanel = new JPanel(new BorderLayout(10, 0));
        filterPanel.setOpaque(false);
        searchField.setPreferredSize(new java.awt.Dimension(200, 30));
        searchField.setToolTipText("Search by asset name...");
        JButton searchBtn = UIFactory.primaryButton("Search");
        searchBtn.addActionListener(e -> loadAssets());
        
        JPanel searchBox = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        searchBox.setOpaque(false);
        searchBox.add(new JLabel("Search:"));
        searchBox.add(searchField);
        searchBox.add(new JLabel("Type:"));
        searchBox.add(typeFilter);
        searchBox.add(searchBtn);
        
        filterPanel.add(searchBox, BorderLayout.WEST);
        
        tableCard.add(filterPanel, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);
        UIFactory.styleTable(table);
        loadAssets();
    }

    private void addAsset() {
        String pin = pinField.getText() != null ? pinField.getText().trim() : "";
        if (nameField.getText().trim().isEmpty() || typeField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Asset name and type are required.");
            return;
        }
        if (fileField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "File path/URL is required.");
            return;
        }
        if (!pin.matches("\\d{5}")) {
            JOptionPane.showMessageDialog(this, "Asset PIN must be exactly 5 digits.");
            return;
        }
        boolean ok = assetService.addAsset(
                nameField.getText(),
                typeField.getText(),
                user.getUserId(),
                encryptedBox.isSelected(),
                fileField.getText(),
                pin
        );
        if (ok) {
            JOptionPane.showMessageDialog(this, "Asset added");
            pinField.setText("");
            loadAssets();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add asset. Check DB schema and inputs.");
        }
    }

    private void deleteAsset() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this asset?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        
        int assetId = (int) tableModel.getValueAt(row, 0);
        boolean ok = assetService.deleteAsset(assetId, user.getUserId());
        JOptionPane.showMessageDialog(this, ok ? "Deleted" : "Delete failed");
        loadAssets();
    }
    
    private void openFile() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an asset to open.");
            return;
        }
        String path = (String) tableModel.getValueAt(row, 5);
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(path));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to open file: " + ex.getMessage());
        }
    }

    private void loadAssets() {
        tableModel.setRowCount(0);
        String query = searchField.getText();
        String type = (String) typeFilter.getSelectedItem();
        List<Asset> assets = assetService.searchAssets(user.getUserId(), query, type);
        for (Asset a : assets) {
            String name = a.isEncrypted() ? a.getAssetName() + " [Encrypted 🔒]" : a.getAssetName();
            int nominees = assetService.getNomineeCountForAsset(a.getAssetId());
            tableModel.addRow(new Object[]{a.getAssetId(), name, a.getAssetType(), a.isLocked() ? "Locked" : "Open", nominees, a.getFileUrl()});
        }
    }
}
