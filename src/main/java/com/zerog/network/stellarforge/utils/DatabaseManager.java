package com.zerog.network.stellarforge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.Instant;

/**
 * Manages the embedded SQLite database used for:
 *  - Operation history (last 500 ops)
 *  - Mod cache (search results, metadata)
 *  - Profile metadata (last-used timestamps, favourite flag)
 *
 * Database file: config/stellarforge.db
 *
 * Thread-safety: all public methods are synchronized.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_PATH = "config/stellarforge.db";
    private static final int SCHEMA_VERSION = 1;

    private static volatile DatabaseManager instance;

    private Connection conn;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) synchronized (DatabaseManager.class) {
            if (instance == null) instance = new DatabaseManager();
        }
        return instance;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public synchronized void open() {
        if (conn != null) return;
        try {
            new File("config").mkdirs();
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            conn.setAutoCommit(true);
            applySchema();
            log.info("Database opened: {}", DB_PATH);
        } catch (Exception e) {
            log.error("Failed to open database – running without persistence", e);
            conn = null;
        }
    }

    public synchronized void close() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignore) {}
            conn = null;
        }
    }

    public synchronized boolean isAvailable() { return conn != null; }

    // ── Operation History ─────────────────────────────────────────────────────

    /**
     * Persist a completed operation record.
     */
    public synchronized void saveOperation(ProgressManager.Operation op) {
        if (conn == null) return;
        try {
            String sql = "INSERT OR REPLACE INTO operation_history " +
                         "(id, name, status, status_msg, percent, start_ms, end_ms) " +
                         "VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, op.id);
                ps.setString(2, op.name);
                ps.setString(3, op.status.name());
                ps.setString(4, op.statusMsg);
                ps.setInt   (5, op.percent);
                ps.setLong  (6, op.startTime.toEpochMilli());
                ps.setLong  (7, op.endTime != null ? op.endTime.toEpochMilli() : Instant.now().toEpochMilli());
                ps.executeUpdate();
            }
            // Trim to last 500 rows
            conn.createStatement().execute(
                "DELETE FROM operation_history WHERE id NOT IN " +
                "(SELECT id FROM operation_history ORDER BY start_ms DESC LIMIT 500)");
        } catch (SQLException e) {
            log.warn("Could not save operation history", e);
        }
    }

    /**
     * Load summaries of the last {@code limit} operations (newest first).
     */
    public synchronized ResultSet loadOperationHistory(int limit) {
        if (conn == null) return null;
        try {
            String sql = "SELECT * FROM operation_history ORDER BY start_ms DESC LIMIT ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            return ps.executeQuery();
        } catch (SQLException e) {
            log.warn("Could not load operation history", e);
            return null;
        }
    }

    // ── Profile metadata ──────────────────────────────────────────────────────

    public synchronized void setProfileLastUsed(String profileId, long epochMs) {
        if (conn == null) return;
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO profile_meta (profile_id, last_used_ms) VALUES (?,?)")) {
                ps.setString(1, profileId);
                ps.setLong  (2, epochMs);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.warn("Could not update profile last-used", e);
        }
    }

    public synchronized long getProfileLastUsed(String profileId) {
        if (conn == null) return 0L;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT last_used_ms FROM profile_meta WHERE profile_id = ?")) {
            ps.setString(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            return 0L;
        }
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    private void applySchema() throws SQLException {
        Statement st = conn.createStatement();
        st.execute("PRAGMA journal_mode=WAL");
        st.execute("PRAGMA synchronous=NORMAL");

        st.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY)");
        ResultSet vrs = st.executeQuery("SELECT version FROM schema_version");
        int current = vrs.next() ? vrs.getInt(1) : 0;
        if (current >= SCHEMA_VERSION) return;

        // v1 tables
        st.execute(
            "CREATE TABLE IF NOT EXISTS operation_history (" +
            "  id         TEXT PRIMARY KEY," +
            "  name       TEXT," +
            "  status     TEXT," +
            "  status_msg TEXT," +
            "  percent    INTEGER," +
            "  start_ms   INTEGER," +
            "  end_ms     INTEGER" +
            ")");
        st.execute(
            "CREATE TABLE IF NOT EXISTS profile_meta (" +
            "  profile_id  TEXT PRIMARY KEY," +
            "  last_used_ms INTEGER" +
            ")");
        st.execute(
            "CREATE TABLE IF NOT EXISTS mod_cache (" +
            "  cache_key   TEXT PRIMARY KEY," +
            "  json_data   TEXT," +
            "  fetched_ms  INTEGER" +
            ")");
        st.execute(
            "CREATE INDEX IF NOT EXISTS idx_op_start ON operation_history(start_ms)");

        st.execute("INSERT OR REPLACE INTO schema_version(version) VALUES(" + SCHEMA_VERSION + ")");
        log.info("Database schema v{} applied", SCHEMA_VERSION);
    }
}

