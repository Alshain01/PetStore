package io.github.alshain01.petstore;

import io.github.alshain01.flags.Flag;
import io.github.alshain01.flags.Flags;
import io.github.alshain01.flags.ModuleYML;
import io.github.alshain01.petstore.metrics.MetricsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.alshain01.petstore.Updater.UpdateResult;

import java.util.*;

public class PetStore extends JavaPlugin {
    static CustomYML message;  // Static for enumeration access

    long timeout = 250;
    private static boolean economyEnabled = false;
    private Updater updater = null;

    Economy economy = null;
    final Map<String, Object> flags = new HashMap<String, Object>();

    Map<UUID, PluginCommandType> commandQueue = new HashMap<UUID, PluginCommandType>();
    Map<UUID, String> transferQueue = new HashMap<UUID, String>();
    Map<UUID, Double> sellQueue = new HashMap<UUID, Double>();
    Map<UUID, UUID> buyQueue = new HashMap<UUID, UUID>();
    Map<UUID, UUID> claimQueue = new HashMap<UUID, UUID>();
    Map<UUID, Double> forSale = null;
    List<UUID> forClaim = new ArrayList<UUID>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        message = new CustomYML(this, "message.yml");
        message.saveDefaultConfig();

        // Initialize economy
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Vault")) {
            final RegisteredServiceProvider<Economy> economyProvider = Bukkit
                .getServer().getServicesManager()
                .getRegistration(Economy.class);

            if (economyProvider != null) {
                economy = economyProvider.getProvider();
                economyEnabled = true;
                forSale = new HashMap<UUID, Double>();
            }
        }

        //Initialize Flags
        if(Bukkit.getServer().getPluginManager().isPluginEnabled("Flags")) {
            getLogger().info("Enabling Flags Integration");

            // Connect to the data file and register the flags
            Set<Flag> flagSet = Flags.getRegistrar().register(new ModuleYML(this, "flags.yml"), this.getName());
            for(Flag f : flagSet) {
                flags.put(f.getName(), f);
            }
            //releaseFlag = Flags.getRegistrar().getFlag("ReleasePet");
        }

        PluginManager pm = Bukkit.getPluginManager();

        if (getConfig().getBoolean("Update.Check")) {
            new UpdateScheduler().run();
            new UpdateScheduler().runTaskTimer(this, 0, 1728000); // Run every 24 hours after the first time
            getServer().getPluginManager().registerEvents(new UpdateListener(), this);
        }

        if(this.getConfig().getBoolean("Metrics.Enabled")) {
            MetricsManager.StartMetrics(this);
        }

        timeout = getConfig().getLong("CommandTimeout");
        pm.registerEvents(new AnimalListener(this), this);
        getCommand("petstore").setExecutor(new PluginCommand(this));
    }

    @Override
    public void onDisable() {
        writeData();
    }

    void reload() {
        writeData();
        commandQueue = new HashMap<UUID, PluginCommandType>();
        transferQueue = new HashMap<UUID, String>();
        sellQueue = new HashMap<UUID, Double>();
        buyQueue = new HashMap<UUID, UUID>();
        claimQueue = new HashMap<UUID, UUID>();
        forSale = null;
        forClaim = new ArrayList<UUID>();
        this.reloadConfig();
        message.reload();
        readData();
    }

    void readData() {
        CustomYML yml = new CustomYML(this, "data.yml");
        for(Object o : yml.getConfig().getList("Give")) {
            forClaim.add(UUID.fromString((String) o));
        }
        if(PetStore.isEconomy() && yml.getConfig().isConfigurationSection("Sales")) {
            Set<String> keys = yml.getConfig().getConfigurationSection("Sales").getKeys(false);
            for(String k : keys) {
                forSale.put(UUID.fromString(k), yml.getConfig().getDouble("Sales." + k));
            }
        }
    }

    void writeData() {
        CustomYML yml = new CustomYML(this, "data.yml"); 
        //Give
        List<String> gives = new ArrayList<String>();
        for(UUID u : forClaim) {
            gives.add(u.toString());
        }
        yml.getConfig().set("Give", gives);

        // Sale
        if(PetStore.isEconomy()) {
            for(UUID u : forSale.keySet()) {
                yml.getConfig().set("Sales." + u, forSale.get(u));
            }
        }
        yml.saveConfig();
    }

    public static boolean isEconomy() {
        return economyEnabled;
    }

    /*
     * Contains event listeners required for plugin maintenance.
     */
    private class UpdateListener implements Listener {
        // Update listener
        @EventHandler(ignoreCancelled = true)
        private void onPlayerJoin(PlayerJoinEvent e) {
            if(updater == null) { return; }
            if (e.getPlayer().hasPermission("petstore.notifyupdate")) {
                if(updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE) {
                    e.getPlayer().sendMessage(Message.UPDATE_AVAILABLE.get());
                } else if(updater.getResult() == UpdateResult.SUCCESS) {
                    e.getPlayer().sendMessage(Message.UPDATE_DOWNLOADED.get());
                }
            }
        }
    }

    /*
     * Handles update checking and downloading
     */
    private class UpdateScheduler extends BukkitRunnable {
        @Override
        public void run() {
            // Update script
            final String key = getConfig().getString("Update.ServerModsAPIKey");
            final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PetStore");
            updater = (getConfig().getBoolean("Update.Download"))
                    ? new Updater(plugin, 70808, getFile(), Updater.UpdateType.DEFAULT, key, true)
                    : new Updater(plugin, 70808, getFile(), Updater.UpdateType.NO_DOWNLOAD, key, false);

            if (updater.getResult() == UpdateResult.UPDATE_AVAILABLE) {
                Bukkit.getServer().getConsoleSender()
                        .sendMessage("[PetStore] " + Message.UPDATE_AVAILABLE.get());
            } else if (updater.getResult() == UpdateResult.SUCCESS) {
                Bukkit.getServer().getConsoleSender()
                        .sendMessage("[PetStore] "	+ Message.UPDATE_DOWNLOADED.get());
            }
        }
    }

    public int getSalesCount() {
        if(!isEconomy()) { return 0; }
        return forSale.size();
    }

    public int getGiveCount() {
        return forClaim.size();
    }
}
