package io.github.alshain01.petstore;

import org.bukkit.permissions.Permissible;

public enum PluginCommandType {
    TRANSFER(2, "transfer <player>"), SELL(2, "sell <price>"),
    RELEASE(1, "release"), GIVE(1, "give"), CANCEL(1, "cancel"),
    TAME(1, "tame"), RELOAD(1, "reload"), SAVE(1, "save");

    private final int totalArgs;
    private final String help;

    PluginCommandType(int minArgs, String help) {
        totalArgs = minArgs;
        this.help = help;
    }

    public String getHelp() {
        return "/petstore " + help;
    }

    public int getTotalArgs() {
        return totalArgs;
    }

    public boolean hasPermission(Permissible sender) {
        return sender.hasPermission("petstore." + this.toString().toLowerCase());
    }
}
