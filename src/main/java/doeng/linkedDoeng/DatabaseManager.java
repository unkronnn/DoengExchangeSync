package doeng.linkedDoeng;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * DatabaseManager menggunakan HikariCP untuk connection pooling
 * Semua operasi database dilakukan secara asynchronous
 */
public class DatabaseManager {

    private final LinkedDoeng plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(LinkedDoeng plugin) {
        this.plugin = plugin;
    }

    /**
     * Inisialisasi koneksi database dengan HikariCP
     */
    public boolean initialize() {
        FileConfiguration config = plugin.getConfig();

        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "minecraft");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "");

        int poolSize = config.getInt("database.pool-size", 10);
        long connectionTimeout = config.getLong("database.connection-timeout", 30000);
        long maxLifetime = config.getLong("database.max-lifetime", 1800000);

        // Konfigurasi HikariCP
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);

        // Optimasi performa
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("Database connection pool initialized successfully!");
            plugin.getLogger().info("Pool size: " + poolSize);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database connection pool!");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Mendapatkan koneksi dari pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is closed or not initialized");
        }
        return dataSource.getConnection();
    }

    /**
     * Menutup koneksi database pool
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }

    /**
     * Cek apakah player sudah terlink dengan status LINKED
     * @param uuid UUID player
     * @return CompletableFuture<Boolean> true jika sudah terlink
     */
    public CompletableFuture<Boolean> isAlreadyLinked(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id FROM player_verification WHERE minecraft_uuid = ? AND status = 'LINKED' LIMIT 1";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking if player is already linked: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Verifikasi kode dan ambil data verifikasi
     * @param verificationCode Kode verifikasi dari Discord
     * @return CompletableFuture<VerificationData> data verifikasi atau null jika tidak valid
     */
    public CompletableFuture<VerificationData> verifyCode(String verificationCode) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, discord_username, status, expires_at " +
                        "FROM player_verification " +
                        "WHERE verification_code = ? " +
                        "AND status = 'WAITING' " +
                        "AND expires_at > NOW() " +
                        "LIMIT 1";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, verificationCode);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        VerificationData data = new VerificationData();
                        data.id = rs.getInt("id");
                        data.discordUsername = rs.getString("discord_username");
                        data.status = rs.getString("status");
                        return data;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error verifying code: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Update data player setelah verifikasi berhasil
     * @param verificationId ID verifikasi di database
     * @param minecraftUuid UUID player Minecraft
     * @param minecraftIgn In-game name player
     * @return CompletableFuture<Boolean> true jika berhasil
     */
    public CompletableFuture<Boolean> updatePlayerData(int verificationId, String minecraftUuid, String minecraftIgn) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE player_verification " +
                        "SET minecraft_uuid = ?, " +
                        "minecraft_ign = ?, " +
                        "status = 'LINKED', " +
                        "verification_code = NULL " +
                        "WHERE id = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, minecraftUuid);
                stmt.setString(2, minecraftIgn);
                stmt.setInt(3, verificationId);

                int rowsUpdated = stmt.executeUpdate();
                return rowsUpdated > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating player data: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Data class untuk menyimpan hasil verifikasi
     */
    public static class VerificationData {
        public int id;
        public String discordUsername;
        public String status;
    }
}
