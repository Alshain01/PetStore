package io.github.alshain01.petstore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public enum Message {
    CONSOLE_ERROR, COMMAND_ERROR, PLAYER_ERROR, VAULT_ERROR, PRICE_ERROR, OWNER_ERROR, TRANSACTION_ERROR,
    TAME_TIMEOUT, RELEASE_TIMEOUT,

    GIVE_TIMEOUT, GIVE_CANCEL, GIVE_SET, GIVE_INSTRUCTION,
    CLAIM_TIMEOUT, CLAIM_NOTIFY, CLAIM_INSTRUCTION,
    SELL_TIMEOUT, SELL_CANCEL, SELL_SET, SELL_INSTRUCTION,
    BUY_TIMEOUT, BUY_NOTIFY_OWNER, BUY_NOTIFY_RECEIVER, BUY_LOW_FUNDS, BUY_INSTRUCTION,
    CANCEL_TIMEOUT, CANCEL_INSTRUCTION,
    TRANSFER_TIMEOUT, TRANSFER_NOTIFY_OWNER, TRANSFER_NOTIFY_RECEIVER, TRANSFER_INSTRUCTION,
    UPDATE_AVAILABLE, UPDATE_DOWNLOADED;

    public String get() {
        String message = ((PetStore)Bukkit.getPluginManager().getPlugin("PetStore")).message.getConfig().getString("Message." + this.toString());
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
