/*
 * Copyright (c) 2/16/14 1:44 PM Kevin Seiden. All rights reserved.
 *
 *  This works is licensed under the Creative Commons Attribution-NonCommercial 3.0
 *
 *  You are Free to:
 *     to Share: to copy, distribute and transmit the work
 *     to Remix: to adapt the work
 *
 *  Under the following conditions:
 *     Attribution: You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
 *     Non-commercial: You may not use this work for commercial purposes.
 *
 *  With the understanding that:
 *     Waiver: Any of the above conditions can be waived if you get permission from the copyright holder.
 *     Public Domain: Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
 *     Other Rights: In no way are any of the following rights affected by the license:
 *         Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
 *         The author's moral rights;
 *         Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.
 *
 *  Notice: For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 *  http://creativecommons.org/licenses/by-nc/3.0/
 */

package io.github.alshain01.petstore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
                sender.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "PetStore Reloaded");
                return true;
            case SAVE:
                plugin.writeData();
                sender.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "PetStore Data Saved");
                return true;
            default:
                break;
        }

        // All of the remaining commands require an animal entity to be identified
        if(!(sender instanceof Player)) {
            sender.sendMessage(getHelp(sender));
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
                    plugin.commandQueue.put(pID, action);
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
                plugin.commandQueue.put(pID, action);
                plugin.sellQueue.put(pID, price);
                player.sendMessage(Message.CLICK_INSTRUCTION.get().replaceAll("\\{Action\\}", action.getMessage().toLowerCase()));
                new TimeoutTask(plugin, action, player).runTaskLater(plugin, plugin.timeout);
                return true;
            default:
                player.sendMessage(getHelp(player));
                return true;
        }
    }

    private String getHelp(Permissible sender) {
        if(sender instanceof ConsoleCommandSender) {
            return "/petstore <save | reload>";
        }

        StringBuilder helpText = new StringBuilder("/petstore <");
        boolean first = true;
        for(PluginCommandType a : PluginCommandType.values()) {
            if(a.isHidden()) { continue; }
            if(a.hasPermission(sender)) {
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
