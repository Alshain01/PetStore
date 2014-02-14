package io.github.alshain01.petstore;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.UUID;

class PluginCommand implements CommandExecutor {
    private final PetStore plugin;

    PluginCommand(PetStore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(getHelp(sender));
            return true;
        }

        final PluginCommandType action = getAction(args[0], sender);
        if(action == null) { return true; }

        // Check that there are enough arguments
        if(args.length < action.getTotalArgs()) {
            sender.sendMessage(action.getHelp());
            return true;
        }

        // Console actions
        switch(action) {
            case RELOAD:
                plugin.reload();
                return true;
            case SAVE:
                plugin.writeData();
                return true;
            default:
                break;
        }

        // All of the remaining commands require an animal entity to be identified
        if(!(sender instanceof Player)) {
            sender.sendMessage(Message.CONSOLE_ERROR.get());
            return true;
        }

        final Player player = (Player) sender;
        final UUID pID = player.getUniqueId();


        // Check the permissions
        if(!action.hasPermission(player)) {
            player.sendMessage(Message.COMMAND_ERROR.get());
            return true;
        }

        // Perform the command
        switch (action) {
            case GIVE:
            case TAME:
            case RELEASE:
            case CANCEL:
                plugin.commandQueue.put(pID, action);
                player.sendMessage(Message.CLICK_INSTRUCTION.get().replaceAll("\\{Action\\}", action.getMessage().toLowerCase()));
                new TimeoutTask(plugin, action, player).runTaskLater(plugin, plugin.timeout);
                return true;
            case TRANSFER:
                Player receiver = Bukkit.getServer().getPlayer(args[1]);
                if (receiver == null) {
                    player.sendMessage(Message.PLAYER_ERROR.get());
                } else {
                    plugin.transferQueue.put(pID, receiver.getName());
                    player.sendMessage(Message.CLICK_INSTRUCTION.get().replaceAll("\\{Action\\}", action.getMessage().toLowerCase()));
                    new TimeoutTask(plugin, action, player).runTaskLater(plugin, plugin.timeout);
                }
                return true;
            case SELL:
                if(!PetStore.isEconomy()) {
                    player.sendMessage(Message.VAULT_ERROR.get());
                    return true;
                }

                Double price;
                try {
                    price = Double.valueOf(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Message.PRICE_ERROR.get());
                    return true;
                }
                plugin.sellQueue.put(pID, price);
                player.sendMessage(Message.CLICK_INSTRUCTION.get().replaceAll("\\{Action\\}", action.getMessage().toLowerCase()));
                new TimeoutTask(plugin, action, player).runTaskLater(plugin, plugin.timeout);
                return true;
            default:
                player.sendMessage(getHelp(player));
                return true;
        }
    }

    private String getHelp(Permissible player) {
        if(player instanceof ConsoleCommandSender) {
            return "/petstore <save | reload>";
        }

        StringBuilder helpText = new StringBuilder("/petstore <");
        boolean first = true;
        for(PluginCommandType a : PluginCommandType.values()) {
            if(a.hasPermission(player)) {
                if(a != PluginCommandType.SELL || PetStore.isEconomy()) {
                    if(!first) { helpText.append(" | "); }
                    helpText.append(a.toString().toLowerCase());
                    first = false;
                }
            }
        }
        return helpText.append(">").toString();
    }

    private PluginCommandType getAction(String action, CommandSender sender) {
        try {
            return PluginCommandType.valueOf(action.toUpperCase());
        } catch(IllegalArgumentException e) {
            sender.sendMessage(getHelp(sender));
            return null;
        }
    }
}
