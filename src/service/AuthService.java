package service;

import dao.ActivityDAO;
import dao.UserDAO;
import model.User;
import util.BCryptUtil;
import util.DateTimeUtil;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final ActivityDAO activityDAO = new ActivityDAO();

    public String register(String username, String email, String password) {
        if (username == null || username.isBlank() || email == null || email.isBlank() || password == null || password.length() < 6) {
            return "Invalid input. Password must be at least 6 characters.";
        }
        if (userDAO.isUsernameOrEmailTaken(username.trim(), email.trim())) {
            return "Registration failed: Username or email already exists.";
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(BCryptUtil.hashPassword(password));
        user.setRoleId(1);
        boolean ok = userDAO.registerUser(user);
        return ok ? "Registration successful." : "Registration failed. Please try again later.";
    }

    public LoginResult login(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user == null) {
            return new LoginResult(false, "User not found.", null);
        }
        if (user.isAccountLocked()) {
            return new LoginResult(false, "Account locked after multiple failed attempts.", null);
        }

        boolean valid = BCryptUtil.checkPassword(password, user.getPasswordHash());
        if (!valid) {
            int newCount = user.getFailedAttempts() + 1;
            boolean lock = newCount >= 5;
            userDAO.updateLoginFailure(user.getUserId(), newCount, lock);
            if (newCount >= 3) {
                activityDAO.addSecurityAlert(
                        user.getUserId(),
                        "FAILED_LOGIN",
                        lock ? "Account locked after repeated failed logins." : "Multiple failed login attempts detected.",
                        DateTimeUtil.now(),
                        lock ? "CRITICAL" : "OPEN"
                );
            }
            return new LoginResult(false, lock ? "Account locked (5 failed attempts)." : "Invalid password.", null);
        }

        String now = DateTimeUtil.now();
        userDAO.updateLoginSuccess(user.getUserId(), now);
        activityDAO.logLoginHistory(user.getUserId(), now, "127.0.0.1");
        activityDAO.logAudit(user.getUserId(), "User login successful", now);
        return new LoginResult(true, "Login successful.", userDAO.findByUsername(username));
    }

    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final User user;

        public LoginResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }
}
