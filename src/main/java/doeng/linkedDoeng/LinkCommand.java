package doeng.linkedDoeng;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Command handler untuk /link <kode>
 * Semua operasi database dilakukan secara asynchronous
 */
public class LinkCommand implements CommandExecutor {

    private final LinkedDoeng plugin;
    private final DatabaseManager databaseManager;

    public LinkCommand(LinkedDoeng plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // Validasi: Command hanya bisa dijalankan oleh player
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Command ini hanya bisa dijalankan oleh player!")
                    .color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        // Validasi: Jika argumen kosong
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /link <kode>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        String verificationCode = args[0];

        // Kirim pesan bahwa proses verifikasi sedang berjalan
        player.sendMessage(Component.text("Memproses kode verifikasi...")
                .color(NamedTextColor.GRAY));

        // Jalankan verifikasi secara asynchronous
        processVerificationAsync(player, verificationCode);

        return true;
    }

    /**
     * Proses verifikasi secara asynchronous agar tidak lag server
     */
    private void processVerificationAsync(Player player, String verificationCode) {
        String playerUuid = player.getUniqueId().toString();
        String playerName = player.getName();

        // Step 1: Cek apakah player sudah terlink
        databaseManager.isAlreadyLinked(playerUuid).thenAccept(isLinked -> {
            if (isLinked) {
                // Player sudah terlink
                plugin.getServer().getScheduler().runTask(plugin,
                    () -> player.sendMessage(Component.text("Akun Minecraft Anda sudah terlink dengan Discord!")
                            .color(NamedTextColor.RED)));
                return;
            }

            // Step 2: Verifikasi kode
            databaseManager.verifyCode(verificationCode).thenAccept(verificationData -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (verificationData == null) {
                        // Kode tidak valid / expired / status bukan WAITING
                        player.sendMessage(Component.text("[!] Kode verifikasi tidak valid atau sudah kadaluarsa.")
                                .color(NamedTextColor.RED));
                    } else {
                        // Step 3: Kode valid, update database
                        updatePlayerLink(player, verificationData, playerUuid, playerName);
                    }
                });
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Error during verification: " + ex.getMessage());
            plugin.getServer().getScheduler().runTask(plugin,
                () -> player.sendMessage(Component.text("Terjadi kesalahan saat memproses verifikasi.")
                        .color(NamedTextColor.RED)));
            return null;
        });
    }

    /**
     * Update data player di database setelah verifikasi berhasil
     */
    private void updatePlayerLink(Player player, DatabaseManager.VerificationData verificationData,
                                  String playerUuid, String playerName) {

        databaseManager.updatePlayerData(verificationData.id, playerUuid, playerName)
            .thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sendSuccessMessage(player, verificationData.discordUsername);
                    } else {
                        player.sendMessage(Component.text("Gagal mengupdate data player.")
                                .color(NamedTextColor.RED));
                    }
                });
            })
            .exceptionally(ex -> {
                plugin.getLogger().severe("Error updating player data: " + ex.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                    () -> player.sendMessage(Component.text("Terjadi kesalahan saat mengupdate data.")
                            .color(NamedTextColor.RED)));
                return null;
            });
    }

    /**
     * Kirim pesan sukses dengan format MiniMessage yang ditentukan
     */
    private void sendSuccessMessage(Player player, String discordUsername) {
        player.sendMessage(Component.text()
                .append(Component.text("---------------------------------------------", NamedTextColor.DARK_GRAY)
                        .decorate(TextDecoration.STRIKETHROUGH))
                .append(Component.newline())
                .append(Component.text("VERIFIKASI BERHASIL!", NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Akun Minecraft Anda kini terhubung dengan Discord:", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("@" + discordUsername, NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Sekarang Anda bisa melakukan Top-up Gems.", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("---------------------------------------------", NamedTextColor.DARK_GRAY)
                        .decorate(TextDecoration.STRIKETHROUGH)));
    }
}
