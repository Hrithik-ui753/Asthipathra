package ui.pages;

import model.Nominee;
import model.User;
import service.NomineeService;
import service.VerificationService;
import ui.UIFactory;
import ui.UITheme;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class NomineePanel extends JPanel {
    private final NomineeService nomineeService = new NomineeService();
    private final VerificationService verificationService = new VerificationService();
    private final User user;
    
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Name", "Email", "Relation", "Access", "Verified"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    
    private final DefaultTableModel vModel = new DefaultTableModel(new Object[]{"ID", "Verification Question"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable vTable = new JTable(vModel);

    private final JTextField name = UIFactory.inputField();
    private final JTextField email = UIFactory.inputField();
    private final JTextField relation = UIFactory.inputField();
    private final JTextField access = UIFactory.inputField();
    private final JTextField verifyCodeField = UIFactory.inputField();

    private final JTextField vQuestion = UIFactory.inputField();
    private final JTextField vAnswer = UIFactory.inputField();

    public NomineePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(15, 15));
        setBackground(UITheme.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.BODY);

        // Tab 1: Nominee Management
        JPanel mainTab = new JPanel(new BorderLayout(10, 10));
        mainTab.setOpaque(false);
        
        JPanel form = UIFactory.cardPanel();
        form.setLayout(new GridLayout(0, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Register New Nominee"));
        form.add(new JLabel("Name")); form.add(name);
        form.add(new JLabel("Email")); form.add(email);
        form.add(new JLabel("Relation")); form.add(relation);
        form.add(new JLabel("Access Level")); form.add(access);

        JButton add = UIFactory.primaryButton("Add Nominee");
        JButton delete = UIFactory.ghostButton("Delete Selected");
        JButton showCode = UIFactory.ghostButton("Show Verification Code");
        JButton verifyNominee = UIFactory.primaryButton("Verify Selected");
        add.addActionListener(e -> addNominee());
        delete.addActionListener(e -> deleteNominee());
        showCode.addActionListener(e -> showVerificationCode());
        verifyNominee.addActionListener(e -> verifyNominee());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(add); actions.add(delete); actions.add(showCode); actions.add(verifyNominee);
        actions.add(new JLabel("Code:"));
        verifyCodeField.setPreferredSize(new Dimension(120, 30));
        actions.add(verifyCodeField);

        JPanel tableCard = UIFactory.cardPanel();
        tableCard.setLayout(new BorderLayout());
        UIFactory.styleTable(table);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setOpaque(false);
        leftPanel.add(form, BorderLayout.NORTH);
        leftPanel.add(actions, BorderLayout.CENTER);

        mainTab.add(leftPanel, BorderLayout.WEST);
        mainTab.add(tableCard, BorderLayout.CENTER);

        // Tab 2: Verification Questions
        JPanel verifyTab = new JPanel(new BorderLayout(15, 15));
        verifyTab.setOpaque(false);

        JPanel vForm = UIFactory.cardPanel();
        vForm.setLayout(new GridLayout(0, 1, 8, 8));
        vForm.setPreferredSize(new Dimension(300, 0));
        vForm.setBorder(BorderFactory.createTitledBorder("Setup Identity Verification"));
        
        vForm.add(new JLabel("1. Select Nominee from Table"));
        vForm.add(new JLabel("2. Enter Security Question"));
        vForm.add(vQuestion);
        vForm.add(new JLabel("3. Set Correct Answer"));
        vForm.add(vAnswer);
        
        JButton addV = UIFactory.primaryButton("Add Question");
        addV.addActionListener(e -> addVerification());
        vForm.add(addV);

        JPanel vTableCard = UIFactory.cardPanel();
        vTableCard.setLayout(new BorderLayout(10, 10));
        vTableCard.add(new JLabel("Configured Questions for Selected Nominee"), BorderLayout.NORTH);
        UIFactory.styleTable(vTable);
        vTableCard.add(new JScrollPane(vTable), BorderLayout.CENTER);
        
        JButton delV = UIFactory.ghostButton("Remove Question");
        delV.addActionListener(e -> deleteVerification());
        vTableCard.add(delV, BorderLayout.SOUTH);

        verifyTab.add(vForm, BorderLayout.WEST);
        verifyTab.add(vTableCard, BorderLayout.CENTER);

        tabs.addTab("General Management", mainTab);
        tabs.addTab("Identity Verification Layer", verifyTab);
        
        // Listener to load questions when nominee is selected
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadQuestions();
        });

        add(UIFactory.titleLabel("Nominee & Verification Management"), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        load();
    }

    private void addNominee() {
        String n = name.getText() != null ? name.getText().trim() : "";
        String e = email.getText() != null ? email.getText().trim() : "";
        String r = relation.getText() != null ? relation.getText().trim() : "";
        String a = access.getText() != null ? access.getText().trim() : "";

        if (n.isEmpty() || e.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nominee name and email are required.");
            return;
        }
        if (r.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Relation is required.");
            return;
        }
        if (a.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Access level is required (e.g., READ/DOWNLOAD/MANAGE).");
            return;
        }

        boolean ok = nomineeService.addNominee(n, e, r, user.getUserId(), a);
        if (ok) {
            name.setText(""); email.setText(""); relation.setText(""); access.setText("");
            load();
        } else {
            JOptionPane.showMessageDialog(this, "Nominee add failed. Check your inputs/DB columns.");
        }
    }

    private void deleteNominee() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) model.getValueAt(row, 0);
        if (nomineeService.deleteNominee(id, user.getUserId())) load();
    }

    private void addVerification() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a nominee first.");
            return;
        }
        int nomId = (int) model.getValueAt(row, 0);
        if (verificationService.addSecurityQuestion(nomId, vQuestion.getText(), vAnswer.getText())) {
            vQuestion.setText(""); vAnswer.setText("");
            loadQuestions();
        }
    }

    private void deleteVerification() {
        int row = vTable.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(vModel.getValueAt(row, 0).toString());
        if (verificationService.deleteQuestion(id)) loadQuestions();
    }

    private void load() {
        model.setRowCount(0);
        for (Nominee n : nomineeService.getNominees(user.getUserId())) {
            model.addRow(new Object[]{n.getNomineeId(), n.getName(), n.getEmail(), n.getRelation(), n.getAccessLevel(), n.isVerified() ? "Yes" : "No"});
        }
    }

    private void showVerificationCode() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int nomineeId = (int) model.getValueAt(row, 0);
        String code = nomineeService.getVerificationCodeForNominee(nomineeId, user.getUserId());
        JOptionPane.showMessageDialog(this, code == null ? "Code unavailable." : "Verification code: " + code);
    }

    private void verifyNominee() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int nomineeId = (int) model.getValueAt(row, 0);
        boolean ok = nomineeService.verifyNominee(nomineeId, user.getUserId(), verifyCodeField.getText().trim());
        JOptionPane.showMessageDialog(this, ok ? "Nominee verified." : "Invalid verification code.");
        if (ok) {
            verifyCodeField.setText("");
            load();
        }
    }

    private void loadQuestions() {
        vModel.setRowCount(0);
        int row = table.getSelectedRow();
        if (row < 0) return;
        int nomId = (int) model.getValueAt(row, 0);
        for (String[] q : verificationService.getQuestions(nomId)) {
            vModel.addRow(q);
        }
    }
}
