package io.github.alshain01.petstore;

import org.bukkit.ChatColor;

enum Message {
    CONSOLE_ERROR, COMMAND_ERROR, PLAYER_ERROR, VAULT_ERROR, PRICE_ERROR,
    OWNER_ERROR, TRANSACTION_ERROR, TAME_ERROR, TIMEOUT,
    CLICK_INSTRUCTION, BUY_INSTRUCTION, CLAIM_INSTRUCTION,
    GIVE_CANCEL, SELL_CANCEL,
    GIVE_SET, SELL_SET,
    CLAIM_NOTIFY, BUY_NOTIFY_OWNER, BUY_NOTIFY_RECEIVER,
    TRANSFER_NOTIFY_OWNER, TRANSFER_NOTIFY_RECEIVER, BUY_LOW_FUNDS,
    UPDATE_AVAILABLE, UPDATE_DOWNLOADED,
    TAME, RELEASE, GIVE, CLAIM, SELL, BUY, CANCEL, TRANSFER;

    public String get() {
        String message = PetStore.message.getConfig().getString("Message." + this.toString());
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
