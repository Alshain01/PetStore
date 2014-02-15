package io.github.alshain01.petstore;

import org.bukkit.permissions.Permissible;

enum PluginCommandType {
    TRANSFER(2, "transfer <player>", true), GIVE(1, "give", true), SELL(2, "sell <price>", true),
    TAME(1, "tame", true), RELEASE(1, "release", true), CANCEL(1, "cancel", true),
    RELOAD(1, "reload", false), SAVE(1, "save", false),

    // These aren't actually command actions, but needed for timeout setup
    // Command Executor will ignore them and reply with help
    BUY(0, "buy", false), CLAIM(0, "claim", false);

    private final int totalArgs;
    private final String help;
    private final boolean show;

    PluginCommandType(int minArgs, String help, boolean show) {
        totalArgs = minArgs;
        this.help = help;
        this.show = show;
    }

    public String getMessage() {
        return Message.valueOf(this.toString()).get();
    }

    public boolean isHidden() {
        return !show;
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
