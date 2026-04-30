PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS Roles (
    role_id INTEGER PRIMARY KEY AUTOINCREMENT,
    role_name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role_id INTEGER NOT NULL,
    is_verified INTEGER NOT NULL DEFAULT 1,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked INTEGER NOT NULL DEFAULT 0,
    last_login TEXT,
    two_factor_enabled INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);

CREATE TABLE IF NOT EXISTS Two_Factor_Auth (
    auth_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    otp_code TEXT NOT NULL,
    expiry_time TEXT NOT NULL,
    is_verified INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS User_Devices (
    device_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    device_name TEXT NOT NULL,
    ip_address TEXT,
    location TEXT,
    last_used TEXT,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Assets (
    asset_id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_name TEXT NOT NULL,
    asset_type TEXT NOT NULL,
    owner_id INTEGER NOT NULL,
    is_encrypted INTEGER NOT NULL DEFAULT 0,
    asset_pin_hash TEXT,
    is_locked INTEGER NOT NULL DEFAULT 0,
    encryption_key TEXT,
    file_url TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Nominees (
    nominee_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    relation TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    access_level TEXT NOT NULL,
    is_verified INTEGER NOT NULL DEFAULT 0,
    verification_code TEXT,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Asset_Sharing (
    share_id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id INTEGER NOT NULL,
    nominee_id INTEGER NOT NULL,
    share_percentage REAL NOT NULL CHECK (share_percentage >= 0 AND share_percentage <= 100),
    access_type TEXT NOT NULL,
    condition_type TEXT,
    FOREIGN KEY (asset_id) REFERENCES Assets(asset_id),
    FOREIGN KEY (nominee_id) REFERENCES Nominees(nominee_id)
);

CREATE TABLE IF NOT EXISTS Release_Conditions (
    condition_id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id INTEGER NOT NULL,
    condition_type TEXT NOT NULL,
    condition_value TEXT NOT NULL,
    FOREIGN KEY (asset_id) REFERENCES Assets(asset_id)
);

CREATE TABLE IF NOT EXISTS Consent (
    consent_id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id INTEGER NOT NULL,
    nominee_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (asset_id) REFERENCES Assets(asset_id),
    FOREIGN KEY (nominee_id) REFERENCES Nominees(nominee_id)
);

CREATE TABLE IF NOT EXISTS Release_Log (
    release_id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id INTEGER NOT NULL,
    nominee_id INTEGER NOT NULL,
    release_time TEXT NOT NULL,
    status TEXT NOT NULL,
    FOREIGN KEY (asset_id) REFERENCES Assets(asset_id),
    FOREIGN KEY (nominee_id) REFERENCES Nominees(nominee_id)
);

CREATE TABLE IF NOT EXISTS Login_History (
    login_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    login_time TEXT NOT NULL,
    ip_address TEXT,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS User_Sessions (
    session_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    login_time TEXT NOT NULL,
    logout_time TEXT,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Audit_Log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    action TEXT NOT NULL,
    ip_address TEXT,
    device_info TEXT,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Security_Alerts (
    alert_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    activity_type TEXT NOT NULL,
    description TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    status TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Asset_Access_Log (
    access_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    asset_id INTEGER NOT NULL,
    access_type TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (asset_id) REFERENCES Assets(asset_id)
);

CREATE TABLE IF NOT EXISTS Dead_Man_Switch (
    user_id INTEGER PRIMARY KEY,
    last_check_in TEXT NOT NULL,
    check_in_interval INTEGER NOT NULL DEFAULT 180,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Security_Questions (
    question_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    question TEXT NOT NULL,
    answer_hash TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS User_Points (
    user_id INTEGER PRIMARY KEY,
    points INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS User_Trust_Score (
    user_id INTEGER PRIMARY KEY,
    score INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Notifications (
    notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    message TEXT NOT NULL,
    is_read INTEGER NOT NULL DEFAULT 0,
    priority TEXT NOT NULL DEFAULT 'MEDIUM',
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Nominee_Verification (
    verification_id INTEGER PRIMARY KEY AUTOINCREMENT,
    nominee_id INTEGER NOT NULL,
    question TEXT NOT NULL,
    answer_hash TEXT NOT NULL,
    FOREIGN KEY (nominee_id) REFERENCES Nominees(nominee_id)
);
