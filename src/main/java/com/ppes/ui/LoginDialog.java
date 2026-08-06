package com.ppes.ui;

import com.ppes.db.Db;
import com.ppes.db.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Modal login screen shown before the main window. Checks the "users" table
 * (username + password_hash) via PasswordUtil.
 *
 * Because the "users" table itself can only be managed from inside this app,
 * a "Skip Login (Setup Mode)" button is always available as an escape hatch —
 * without it, a fresh database with zero users would permanently lock
 * everyone out.
 */
public class LoginDialog extends JDialog {

    public static class LoginResult {
        public final boolean success;
        public final String username;
        public final String role;
        public final boolean setupMode;

        LoginResult(boolean success, String username, String role, boolean setupMode) {
            this.success = success;
            this.username = username;
            this.role = role;
            this.setupMode = setupMode;
        }
    }

    private LoginResult result; // stays null if the window is just closed (= exit app)

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel messageLabel = new JLabel(" ");

    private LoginDialog() {
        super((Frame) null, "PPES - Login", true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(26, 30, 22, 30));

        JLabel title = new JLabel("PPES লগইন");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 21f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Pharmaceuticals Packaging Management");
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(title);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(20));

        content.add(formRow("Username:", usernameField));
        content.add(Box.createVerticalStrut(8));
        content.add(formRow("Password:", passwordField));
        content.add(Box.createVerticalStrut(10));

        messageLabel.setForeground(new Color(0xE5484D));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(messageLabel);
        content.add(Box.createVerticalStrut(6));

        JButton loginBtn = new JButton("Login");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> attemptLogin());

        JButton skipBtn = new JButton("Skip Login (Setup Mode)");
        skipBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        skipBtn.addActionListener(e -> {
            result = new LoginResult(true, "setup", "Admin", true);
            dispose();
        });

        JLabel skipHint = new JLabel(
                "<html><div style='text-align:center; width:260px;'>"
                        + "প্রথমবার ব্যবহার করছেন, বা Users টেবিল এখনো খালি? উপরের বাটনে ক্লিক করুন।"
                        + "</div></html>");
        skipHint.setFont(skipHint.getFont().deriveFont(10.5f));
        skipHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(loginBtn);
        content.add(Box.createVerticalStrut(14));
        content.add(new JSeparator());
        content.add(Box.createVerticalStrut(10));
        content.add(skipBtn);
        content.add(Box.createVerticalStrut(4));
        content.add(skipHint);

        getRootPane().setDefaultButton(loginBtn);
        setContentPane(content);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel formRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(90, 24));
        row.add(l, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(320, 30));
        return row;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("ইউজারনেম ও পাসওয়ার্ড দিন।");
            return;
        }

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT username, password_hash, role FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    messageLabel.setText("ভুল ইউজারনেম অথবা পাসওয়ার্ড।");
                    return;
                }
                String storedHash = rs.getString("password_hash");
                if (!PasswordUtil.verify(password, storedHash)) {
                    messageLabel.setText("ভুল ইউজারনেম অথবা পাসওয়ার্ড।");
                    return;
                }
                String role = rs.getString("role");
                result = new LoginResult(true, username, role == null ? "" : role, false);
                dispose();
            }
        } catch (SQLException e) {
            messageLabel.setText("<html>ডাটাবেজ কানেকশন সমস্যা।</html>");
        }
    }

    /** Shows the dialog and blocks until it's closed. Returns null if the user closed the window without logging in. */
    public static LoginResult showLoginDialog() {
        LoginDialog dlg = new LoginDialog();
        dlg.setVisible(true);
        return dlg.result;
    }
}
