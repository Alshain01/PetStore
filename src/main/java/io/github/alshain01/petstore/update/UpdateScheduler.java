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

package io.github.alshain01.petstore.update;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class UpdateScheduler extends BukkitRunnable{
    private final Updater.UpdateType type;
    private final File file, dataFolder;
    private final String version, updateFolder, key;
    private final List<String> authors;
    private final Logger logger;

    private Updater updater = null;

    public UpdateScheduler(Plugin plugin, File file, ConfigurationSection config) {
        // Move everything out of bukkit so we can run async
        this.file = file;
        this.type = config.getBoolean("Download") ? Updater.UpdateType.DEFAULT : Updater.UpdateType.NO_DOWNLOAD;
        this.key = config.getString("ServerModsAPIKey");
        this.dataFolder = plugin.getDataFolder();
        this.version = plugin.getDescription().getVersion();
        this.updateFolder = plugin.getServer().getUpdateFolder();
        this.authors = plugin.getDescription().getAuthors();
        this.logger = plugin.getLogger();
    }

    protected Updater.UpdateResult getResult() {
        return updater.getResult();
    }

    @Override
    public void run() {
        final int PLUGIN_ID = 70808;
        updater = new Updater(authors, dataFolder, updateFolder, version, logger, PLUGIN_ID, file, type, key, true);

        if (updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE) {
            logger.info("The version of PetStore that this server is running is out of date. "
                    + "Please consider updating to the latest version at dev.bukkit.org/bukkit-plugins/petstore/.");
        } else if (updater.getResult() == Updater.UpdateResult.SUCCESS) {
            logger.info("An update to PetStore has been downloaded and will be installed when the server is reloaded.");
        }
    }
}
