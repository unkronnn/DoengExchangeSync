# 🔗 Doeng Exchange Sync (Discord <-> Minecraft Verification)

A seamless, reverse-flow verification system that allows players to link their Discord accounts to the Minecraft Server starting from a Discord Button.

## 🚀 Features
* **Discord-First Flow:** User clicks a button in Discord -> Gets a code -> Types command in-game.
* **Secure:** Ephemeral messages (hidden codes) and 15-minute expiration logic.
* **Performance:** Fully asynchronous database queries in the Minecraft Plugin (No lag).
* **Tech Stack:**
    * **Bot:** TypeScript, Discord.js v14, MySQL2.
    * **Plugin:** Java, Paper API 1.21.3, HikariCP, MiniMessage.

## 🛠️ Installation
1.  **Database:** Import `schema.sql`.
2.  **Bot:** Run `npm install` and `npm start`.
3.  **Plugin:** Drop the `.jar` into `/plugins` folder.
