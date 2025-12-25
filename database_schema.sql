-- ============================================
-- Database Schema untuk LinkedDiscordMC Plugin
-- ============================================
-- Table: player_verification
-- Menyimpan data verifikasi linking Discord-Minecraft

CREATE TABLE IF NOT EXISTS `player_verification` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `discord_id` VARCHAR(255) NOT NULL,
    `discord_username` VARCHAR(255) NOT NULL,
    `verification_code` VARCHAR(255) DEFAULT NULL,
    `minecraft_uuid` VARCHAR(255) DEFAULT NULL,
    `minecraft_ign` VARCHAR(255) DEFAULT NULL,
    `status` ENUM('WAITING', 'LINKED') DEFAULT 'WAITING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NULL DEFAULT NULL,
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    INDEX `idx_verification_code` (`verification_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_minecraft_uuid` (`minecraft_uuid`),
    INDEX `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Contoh Query untuk Discord Bot (PHP/Node.js/Python)
-- ============================================

-- 1. Membuat kode verifikasi baru saat player request di Discord
-- INSERT INTO player_verification (discord_id, discord_username, verification_code, expires_at)
-- VALUES ('123456789', 'DiscordUser#1234', 'ABC123', DATE_ADD(NOW(), INTERVAL 15 MINUTE));

-- 2. Cek apakah Discord user sudah terlink
-- SELECT * FROM player_verification WHERE discord_id = '123456789' AND status = 'LINKED';

-- 3. Mendapatkan data player yang sudah terlink
-- SELECT * FROM player_verification WHERE status = 'LINKED' AND minecraft_uuid IS NOT NULL;
