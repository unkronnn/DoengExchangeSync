package doeng.linkedDoeng;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Plugin Class - Discord-Minecraft Linking Plugin
 *
 * Fitur:
 * - Verifikasi player menggunakan kode dari Discord
 * - Menggunakan HikariCP untuk connection pooling
 * - Semua operasi database dilakukan secara asynchronous
 * - Menggunakan Kyori Adventure API untuk formatting
 */
public final class LinkedDoeng extends JavaPlugin {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // Plugin startup logic

        // 1. Save config.yml jika belum ada
        saveDefaultConfig();

        // 2. Inisialisasi Database Manager dengan HikariCP
        databaseManager = new DatabaseManager(this);
        boolean dbInitialized = databaseManager.initialize();

        if (!dbInitialized) {
            getLogger().severe("Gagal menginisialisasi database! Plugin dimatikan.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Register Command /link
        getCommand("link").setExecutor(new LinkCommand(this, databaseManager));

        getLogger().info("Plugin LinkedDoeng berhasil diaktifkan!");
        getLogger().info("Command /link telah terdaftar.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Plugin LinkedDoeng dimatikan.");
    }

    /**
     * Mendapatkan Database Manager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
