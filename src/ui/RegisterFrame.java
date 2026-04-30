package ui;

import service.AuthService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

public class RegisterFrame extends JFrame {
    private final JTextField usernameField = UIFactory.inputField();
    private final JTextField emailField = UIFactory.inputField();
    private final JPasswordField passwordField = new JPasswordField();
    private final AuthService authService = new AuthService();

    public RegisterFrame() {
        setTitle("Asthipathra - Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(480, 360));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(new javax.swing.border.EmptyBorder(24, 24, 24, 24));

        JLabel title = UIFactory.titleLabel("Create Asthipathra Account");
        title.setHorizontalAlignment(JLabel.CENTER);

        JPanel form = UIFactory.cardPanel();
        form.setLayout(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Password:"));
        passwordField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(205, 215, 230)),
                new javax.swing.border.EmptyBorder(8, 10, 8, 10)
        ));
        form.add(passwordField);

        JButton registerBtn = UIFactory.primaryButton("Register");
        JButton backBtn = UIFactory.ghostButton("Back to Login");
        registerBtn.addActionListener(e -> register());
        backBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        JPanel buttons = new JPanel();
        buttons.add(registerBtn);
        buttons.add(backBtn);

        root.add(title, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void register() {
        String msg = authService.register(
                usernameField.getText(),
                emailField.getText(),
                new String(passwordField.getPassword())
        );
        JOptionPane.showMessageDialog(this, msg);
        if (msg.startsWith("Registration successful")) {
            new LoginFrame().setVisible(true);
            dispose();
        }
    }
}
