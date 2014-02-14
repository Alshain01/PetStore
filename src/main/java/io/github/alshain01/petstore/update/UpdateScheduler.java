package io.github.alshain01.petstore.update;

import io.github.alshain01.petstore.Message;
import io.github.alshain01.petstore.update.Updater.UpdateType;
import io.github.alshain01.petstore.update.Updater.UpdateResult;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public class UpdateScheduler extends BukkitRunnable{
    private final UpdateType type;
    private final String key;
    private final File file;

    private Updater updater = null;

    public UpdateScheduler(File file, ConfigurationSection config) {
        this.file = file;
        this.type = config.getBoolean("Download") ? Updater.UpdateType.DEFAULT : Updater.UpdateType.NO_DOWNLOAD;
        this.key = config.getString("ServerModsAPIKey");
    }

    UpdateResult getResult() {
        return updater.getResult();
    }

    @Override
    public void run() {
        final int PLUGIN_ID = 70808;

        // Update script
        final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PetStore");
        updater = new Updater(plugin, PLUGIN_ID, file, type, key, type == UpdateType.DEFAULT);

        if (updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
            Bukkit.getServer().getConsoleSender()
                    .sendMessage("[PetStore] " + Message.UPDATE_AVAILABLE.get());
        } else if (updater.getResult() == UpdateResult.SUCCESS) {
            Bukkit.getServer().getConsoleSender()
                    .sendMessage("[PetStore] "	+ Message.UPDATE_DOWNLOADED.get());
        }
    }
}
