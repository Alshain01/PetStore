/*
 * Copyright (c) 2/16/14 1:43 PM Kevin Seiden. All rights reserved.
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

import io.github.alshain01.flags.Flag;
import io.github.alshain01.flags.Flags;
import io.github.alshain01.flags.ModuleYML;
import io.github.alshain01.petstore.metrics.MetricsManager;
import io.github.alshain01.petstore.update.UpdateListener;
import io.github.alshain01.petstore.update.UpdateScheduler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PetStore extends JavaPlugin {
    static CustomYML message;  // Static for enumeration access
    private static boolean economyEnabled = false;

    long timeout = 250;
    Economy economy = null;

    final Map<String, Object> flags = new HashMap<String, Object>();
    final Map<UUID, PluginCommandType> commandQueue = new HashMap<UUID, PluginCommandType>();
    final Map<UUID, String> transferQueue = new HashMap<UUID, String>();
    final Map<UUID, Double> sellQueue = new HashMap<UUID, Double>();
    final Map<UUID, UUID> buyQueue = new HashMap<UUID, UUID>();
    final Map<UUID, UUID> claimQueue = new HashMap<UUID, UUID>();
    final List<UUID> forClaim = new ArrayList<UUID>();
    Map<UUID, Double> forSale = null;

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

        ConfigurationSection updateConfig = getConfig().getConfigurationSection("Update");
        if (updateConfig.getBoolean("Check")) {
            UpdateScheduler updater = new UpdateScheduler(this, getFile(), updateConfig);
            Long timer = updateConfig.getLong("Interval");
            if(timer < 1) {
                updater.runTaskAsynchronously(this);
            } else {
                updater.runTaskTimerAsynchronously(this, 0, timer * 1200);
            }
            pm.registerEvents(new UpdateListener(updater), this);
        }

        if(this.getConfig().getBoolean("Metrics.Enabled")) {
            MetricsManager.StartMetrics(this);
        }

        timeout = getConfig().getLong("CommandTimeout");
        readData();
        pm.registerEvents(new AnimalListener(this), this);
        getCommand("petstore").setExecutor(new PluginCommand(this));
    }

    @Override
    public void onDisable() {
        writeData();
    }

    void reload() {
        writeData();
        commandQueue.clear();
        transferQueue.clear();
        sellQueue.clear();
        buyQueue.clear();
        claimQueue.clear();
        forClaim.clear();
        if(PetStore.isEconomy()) {
            forSale.clear();
        }
        this.reloadConfig();
        message.reload();
        readData();
    }

    private void readData() {
        CustomYML yml = new CustomYML(this, "data.yml");
        List<?> gives = yml.getConfig().getList("Give");
        if(gives != null) {
            for(Object o : yml.getConfig().getList("Give")) {
                forClaim.add(UUID.fromString((String) o));
            }
        }

        if(PetStore.isEconomy() && yml.getConfig().isConfigurationSection("Sales")) {
            Set<String> keys = yml.getConfig().getConfigurationSection("Sales").getKeys(false);
            if(keys != null) {
                for(String k : keys) {
                    forSale.put(UUID.fromString(k), yml.getConfig().getDouble("Sales." + k));
                }
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

    public int getSalesCount() {
        if(!isEconomy()) { return 0; }
        return forSale.size();
    }

    public int getGiveCount() {
        return forClaim.size();
    }
}
