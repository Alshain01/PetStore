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
