package db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final String DB_URL = "jdbc:sqlite:asthipathra.db";
    private static final String[] SCHEMA_CANDIDATES = {
            "src/main/resources/schema.sql",
            "schema.sql"
    };

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);
            stmt.execute("PRAGMA foreign_keys = ON");
            String schemaSql = Files.readString(resolveSchemaPath());
            for (String sql : schemaSql.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            runMigrations(conn);
            ensureDefaultRole(conn);
            conn.commit();
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    private static void ensureDefaultRole(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT OR IGNORE INTO Roles(role_id, role_name) VALUES (1, 'USER')");
            stmt.execute("INSERT OR IGNORE INTO Roles(role_id, role_name) VALUES (2, 'ADMIN')");
        }
    }

    private static void runMigrations(Connection conn) throws SQLException {
        ensureColumn(conn, "Assets", "asset_pin_hash", "TEXT");
        ensureColumn(conn, "Assets", "is_locked", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(conn, "Nominees", "is_verified", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(conn, "Nominees", "verification_code", "TEXT");
    }

    private static void ensureColumn(Connection conn, String table, String column, String type) throws SQLException {
        String pragma = "PRAGMA table_info(" + table + ")";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(pragma)) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type)) {
            ps.executeUpdate();
        }
    }

    private static Path resolveSchemaPath() {
        for (String candidate : SCHEMA_CANDIDATES) {
            Path p = Path.of(candidate);
            if (Files.exists(p)) {
                return p;
            }
        }
        throw new RuntimeException("schema.sql not found. Checked: src/main/resources/schema.sql and schema.sql");
    }
}
