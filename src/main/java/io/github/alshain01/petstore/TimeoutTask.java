package io.github.alshain01.petstore;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

final class TimeoutTask extends BukkitRunnable{
    private final PetStore plugin;
    private final PluginCommandType action;
    private final Player player;

    TimeoutTask(PetStore plugin, PluginCommandType action, Player player) {
        this.plugin = plugin;
        this.action = action;
        this.player = player;
    }

    public void run() {
        if(action.equals(PluginCommandType.CLAIM)) {
            if(plugin.claimQueue.remove(player.getUniqueId()) == null) {
                return; // No message
            }
        } else if(action.equals(PluginCommandType.BUY)) {
            if(plugin.buyQueue.remove(player.getUniqueId()) == null) {
                return; // No Message
            }
        } else if(action.equals(plugin.commandQueue.get(player.getUniqueId()))) {
            plugin.commandQueue.remove(player.getUniqueId());
        } else {
            return;
        }
        plugin.commandQueue.remove(player.getUniqueId());
        player.sendMessage(Message.TIMEOUT.get().replaceAll("\\{Action\\}", action.getMessage()));
    }
}
