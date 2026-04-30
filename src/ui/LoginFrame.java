package ui;

import model.User;
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

public class LoginFrame extends JFrame {
    private final JTextField usernameField = UIFactory.inputField();
    private final JPasswordField passwordField = new JPasswordField();
    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("Asthipathra - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(480, 320));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UITheme.BG_MAIN);
        root.setBorder(new javax.swing.border.EmptyBorder(24, 24, 24, 24));

        JLabel title = UIFactory.titleLabel("Asthipathra");
        title.setHorizontalAlignment(JLabel.CENTER);
        JLabel subtitle = new JLabel("Secure Digital Asset Management", JLabel.CENTER);
        subtitle.setForeground(UITheme.MUTED_TEXT);

        JPanel form = UIFactory.cardPanel();
        form.setLayout(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Password:"));
        passwordField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new Color(205, 215, 230)),
                new javax.swing.border.EmptyBorder(8, 10, 8, 10)
        ));
        form.add(passwordField);

        JButton loginBtn = UIFactory.primaryButton("Login");
        JButton registerBtn = UIFactory.ghostButton("Register");
        loginBtn.addActionListener(e -> login());
        registerBtn.addActionListener(e -> {
            new RegisterFrame().setVisible(true);
            dispose();
        });

        JPanel buttons = new JPanel();
        buttons.add(loginBtn);
        buttons.add(registerBtn);

        JPanel head = new JPanel(new GridLayout(0, 1));
        head.setOpaque(false);
        head.add(title);
        head.add(subtitle);

        root.add(head, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void login() {
        AuthService.LoginResult result = authService.login(usernameField.getText(), new String(passwordField.getPassword()));
        JOptionPane.showMessageDialog(this, result.getMessage());
        if (result.isSuccess()) {
            User user = result.getUser();
            new DashboardFrame(user).setVisible(true);
            dispose();
        }
    }
}
