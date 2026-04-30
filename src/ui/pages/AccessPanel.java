package ui.pages;

import model.Asset;
import model.Nominee;
import model.User;
import service.AssetService;
import service.NomineeService;
import service.ReleaseService;
import service.VerificationService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

public class AccessPanel extends JPanel {
    private final User user;
    private final VerificationService verificationService = new VerificationService();
    private final ReleaseService releaseService = new ReleaseService();
    private final AssetService assetService = new AssetService();
    private final NomineeService nomineeService = new NomineeService();

    private final JTextField questionField = UIFactory.inputField();
    private final JTextField answerField = UIFactory.inputField();
    private final DefaultTableModel ownerQuestionModel = new DefaultTableModel(new Object[]{"ID", "Question"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable ownerQuestionTable = new JTable(ownerQuestionModel);

    private final JComboBox<Nominee> nomineeCombo = new JComboBox<>();
    private final JComboBox<Asset> assetCombo = new JComboBox<>();
    private final JComboBox<String[]> questionCombo = new JComboBox<>();
    private final JTextField nomineeAnswerField = UIFactory.inputField();
    private final JTextField pinField = UIFactory.inputField();
    private final JLabel eligibilityLabel = new JLabel("Eligibility: not checked");
    private final JLabel readinessLabel = new JLabel("Readiness: question ?, pin ?, release ?");
    private JButton unlockBtn;

    public AccessPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(12, 12));
        setBackground(UITheme.BG_MAIN);

        add(UIFactory.titleLabel("Secure Access Challenge"), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 12));
        center.setOpaque(false);
        center.add(createQuestionSetupPanel());
        center.add(createChallengePanel());

        add(center, BorderLayout.CENTER);
        loadOwnerQuestions();
        loadNomineesAndAssets();
    }

    private JPanel createQuestionSetupPanel() {
        JPanel panel = UIFactory.cardPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Owner Question Setup (single-word answer)"));

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setOpaque(false);
        form.add(new JLabel("Question"));
        form.add(questionField);
        form.add(new JLabel("Answer (single word)"));
        form.add(answerField);

        JButton addQuestionBtn = UIFactory.primaryButton("Save Question");
        addQuestionBtn.addActionListener(e -> saveOwnerQuestion());
        form.add(addQuestionBtn);

        UIFactory.styleTable(ownerQuestionTable);
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(ownerQuestionTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChallengePanel() {
        JPanel panel = UIFactory.cardPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Nominee Access Unlock"));

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.setOpaque(false);
        form.add(new JLabel("Nominee"));
        form.add(nomineeCombo);
        form.add(new JLabel("Asset"));
        form.add(assetCombo);
        nomineeCombo.addActionListener(e -> {
            refreshReadinessChecklist();
            updateUnlockEnabled();
        });
        assetCombo.addActionListener(e -> {
            Asset a = (Asset) assetCombo.getSelectedItem();
            if (a != null) {
                loadQuestionsForAsset(a.getAssetId());
            }
            refreshReadinessChecklist();
            updateUnlockEnabled();
        });

        JButton checkEligibilityBtn = UIFactory.ghostButton("Check Eligibility");
        checkEligibilityBtn.addActionListener(e -> checkEligibility());
        form.add(checkEligibilityBtn);

        eligibilityLabel.setOpaque(true);
        eligibilityLabel.setBackground(new Color(241, 245, 249));
        eligibilityLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(eligibilityLabel);
        readinessLabel.setOpaque(true);
        readinessLabel.setBackground(new Color(241, 245, 249));
        readinessLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(readinessLabel);

        form.add(new JLabel("Owner Question"));
        questionCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String[]) {
                    setText(((String[]) value)[1]);
                }
                return this;
            }
        });
        form.add(questionCombo);
        form.add(new JLabel("Owner Question Answer (single word)"));
        form.add(nomineeAnswerField);
        form.add(new JLabel("Asset PIN (5-digit)"));
        form.add(pinField);

        unlockBtn = UIFactory.primaryButton("Final Unlock");
        unlockBtn.setEnabled(false);
        unlockBtn.addActionListener(e -> unlockAccess());
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.add(unlockBtn);
        form.add(actionRow);

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    private void saveOwnerQuestion() {
        String q = questionField.getText().trim();
        String a = answerField.getText().trim();
        boolean ok = verificationService.addOwnerSecurityQuestion(user.getUserId(), q, a);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Question save failed. Use non-empty question and single-word answer.");
            return;
        }
        questionField.setText("");
        answerField.setText("");
        loadOwnerQuestions();
        if (assetCombo.getSelectedItem() != null) {
            Asset selectedAsset = (Asset) assetCombo.getSelectedItem();
            loadQuestionsForAsset(selectedAsset.getAssetId());
        }
        refreshReadinessChecklist();
        updateUnlockEnabled();
        JOptionPane.showMessageDialog(this, "Owner security question saved.");
    }

    private void checkEligibility() {
        Nominee nominee = (Nominee) nomineeCombo.getSelectedItem();
        Asset asset = (Asset) assetCombo.getSelectedItem();
        if (nominee == null || asset == null) {
            return;
        }
        String status = releaseService.getEligibilityStatus(nominee.getNomineeId(), asset.getAssetId());
        eligibilityLabel.setText("Eligibility: " + status);
        loadQuestionsForAsset(asset.getAssetId());
        refreshReadinessChecklist();
        updateUnlockEnabled();
    }

    private void unlockAccess() {
        Nominee nominee = (Nominee) nomineeCombo.getSelectedItem();
        Asset asset = (Asset) assetCombo.getSelectedItem();
        String[] q = (String[]) questionCombo.getSelectedItem();
        if (nominee == null || asset == null || q == null) {
            JOptionPane.showMessageDialog(this, "Select nominee, asset and question first.");
            return;
        }
        int questionId = Integer.parseInt(q[0]);
        String failureReason = releaseService.getSecureAssetAccessFailureReason(
                user.getUserId(),
                nominee.getNomineeId(),
                asset.getAssetId(),
                questionId,
                nomineeAnswerField.getText().trim(),
                pinField.getText().trim()
        );
        if (failureReason != null) {
            JOptionPane.showMessageDialog(this, failureReason);
            return;
        }

        boolean unlocked = releaseService.requestSecureAssetAccess(
                user.getUserId(),
                nominee.getNomineeId(),
                asset.getAssetId(),
                questionId,
                nomineeAnswerField.getText().trim(),
                pinField.getText().trim()
        );

        if (!unlocked) {
            JOptionPane.showMessageDialog(this, "Access denied.");
            return;
        }

        eligibilityLabel.setText("Eligibility: challenge passed and access granted.");
        nomineeAnswerField.setText("");
        pinField.setText("");
        updateUnlockEnabled();

        // Attempt to open the asset file after successful secure unlock
        String path = asset.getFileUrl();
        if (path == null || path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Access granted, but asset has no file URL to open.");
            return;
        }
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(path));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Access granted, but failed to open file: " + ex.getMessage());
        }
    }

    private void loadOwnerQuestions() {
        ownerQuestionModel.setRowCount(0);
        List<String[]> rows = verificationService.getOwnerSecurityQuestions(user.getUserId());
        for (String[] row : rows) {
            ownerQuestionModel.addRow(row);
        }
    }

    private void loadQuestionsForAsset(int assetId) {
        questionCombo.removeAllItems();
        List<String[]> rows = releaseService.getOwnerQuestionsForAsset(assetId);
        for (String[] row : rows) {
            questionCombo.addItem(row);
        }
        updateUnlockEnabled();
    }

    private void loadNomineesAndAssets() {
        nomineeCombo.removeAllItems();
        assetCombo.removeAllItems();

        for (Nominee nominee : nomineeService.getNominees(user.getUserId())) {
            nomineeCombo.addItem(nominee);
        }
        for (Asset asset : assetService.getAssets(user.getUserId())) {
            assetCombo.addItem(asset);
        }

        nomineeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Nominee) {
                    Nominee n = (Nominee) value;
                    setText(n.getName() + " (" + n.getRelation() + ")");
                }
                return this;
            }
        });

        assetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Asset) {
                    setText(((Asset) value).getAssetName());
                }
                return this;
            }
        });
        refreshReadinessChecklist();
        if (assetCombo.getSelectedItem() != null) {
            Asset a = (Asset) assetCombo.getSelectedItem();
            loadQuestionsForAsset(a.getAssetId());
        }
        updateUnlockEnabled();
    }

    private void refreshReadinessChecklist() {
        Nominee nominee = (Nominee) nomineeCombo.getSelectedItem();
        Asset asset = (Asset) assetCombo.getSelectedItem();
        if (nominee == null || asset == null) {
            readinessLabel.setText("Readiness: select nominee + asset");
            readinessLabel.setBackground(new Color(241, 245, 249));
            return;
        }
        int questionCount = releaseService.getOwnerQuestionsForAsset(asset.getAssetId()).size();
        boolean pinSet = asset.getAssetPinHash() != null && !asset.getAssetPinHash().isBlank();
        String eligibility = releaseService.getEligibilityStatus(nominee.getNomineeId(), asset.getAssetId());
        boolean releaseReady = "Eligible for challenge".equals(eligibility);

        String questionStatus = questionCount > 0 ? "Questions: OK (" + questionCount + ")" : "Questions: Missing";
        String pinStatus = pinSet ? "PIN: Set" : "PIN: Missing";
        String releaseStatus = releaseReady ? "Release: Ready" : "Release: Not ready";
        readinessLabel.setText("Readiness -> " + questionStatus + " | " + pinStatus + " | " + releaseStatus);
        readinessLabel.setBackground((questionCount > 0 && pinSet && releaseReady) ? new Color(220, 252, 231) : new Color(254, 242, 242));
    }

    private void updateUnlockEnabled() {
        if (unlockBtn == null) return;
        Nominee nominee = (Nominee) nomineeCombo.getSelectedItem();
        Asset asset = (Asset) assetCombo.getSelectedItem();
        Object q = questionCombo.getSelectedItem();
        String answer = nomineeAnswerField.getText() != null ? nomineeAnswerField.getText().trim() : "";
        String pin = pinField.getText() != null ? pinField.getText().trim() : "";
        boolean pinOk = pin.matches("\\d{5}");
        boolean enabled = nominee != null && asset != null && q != null && !answer.isEmpty() && pinOk;
        unlockBtn.setEnabled(enabled);
    }
}
