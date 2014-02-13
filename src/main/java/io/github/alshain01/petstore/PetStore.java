package io.github.alshain01.petstore;

import io.github.alshain01.flags.Flag;
import io.github.alshain01.flags.Flags;
import io.github.alshain01.flags.ModuleYML;
import io.github.alshain01.flags.area.Area;
import io.github.alshain01.petstore.metrics.MetricsManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.alshain01.petstore.Updater.UpdateResult;

import java.util.*;

public class PetStore extends JavaPlugin {
    CustomYML message;
    GiveAnimal give = null;
    SellAnimal sales = null;
    TransferAnimal transfer;

    Set<UUID> releaseQueue = new HashSet<UUID>();
    Set<UUID> tameQueue = new HashSet<UUID>();
    Set<UUID> cancelQueue = new HashSet<UUID>();

    private Updater updater = null;
    private Object releaseFlag  = null;

    private static long timeout = 250;
    private static boolean economyEnabled = false;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        CustomYML yml = new CustomYML(this, "data.yml");
        message = new CustomYML(this, "message.yml");
        message.saveDefaultConfig();

        // Initialize economy
        Economy economy = null;
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Vault")) {
            final RegisteredServiceProvider<Economy> economyProvider = Bukkit
                .getServer().getServicesManager()
                .getRegistration(Economy.class);

            if (economyProvider != null) {
                economy = economyProvider.getProvider();
                economyEnabled = true;
            }
        }

        //Initialize Flags
        if(Bukkit.getServer().getPluginManager().isPluginEnabled("Flags")) {
            getLogger().info("Enabling Flags Integration");

            // Connect to the data file and register the flags
            Flags.getRegistrar().register(new ModuleYML(this, "flags.yml"), this.getName());
            releaseFlag = Flags.getRegistrar().getFlag("ReleasePet");
        }

        // Read give aways from file
        give = new GiveAnimal(this, yml.getConfig().getList("Give", new ArrayList<String>()));
        transfer = new TransferAnimal(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new CancelListner(), this);
        pm.registerEvents(transfer, this);
        pm.registerEvents(give, this);

        // Read sales from file
        if(economy != null) {
            if(yml.getConfig().isConfigurationSection("Sales")) {
                sales = new SellAnimal(this, economy, yml.getConfig().getConfigurationSection("Sales").getValues(false));
            } else {
                sales = new SellAnimal(this, economy);
            }

            pm.registerEvents(sales, this);
        }

        if (getConfig().getBoolean("Update.Check")) {
            new UpdateScheduler().run();
            new UpdateScheduler().runTaskTimer(this, 0, 1728000); // Run every 24 hours after the first time
            getServer().getPluginManager().registerEvents(new UpdateListener(), this);
        }

        if(this.getConfig().getBoolean("Metrics.Enabled")) {
            MetricsManager.StartMetrics(this);
        }

        timeout = getConfig().getLong("CommandTimeout");
        getCommand("petstore").setExecutor(new PluginCommand(this));
    }

    @Override
    public void onDisable() {
        CustomYML yml = new CustomYML(this, "data.yml");

        // Write give aways to file
        yml.getConfig().set("Give", give.get());

        // Write sales to file
        if(sales != null) {
            yml.getConfig().set("Sales", sales.serialize());
        }
        yml.saveConfig();
    }

    public static boolean isEconomy() {
        return economyEnabled;
    }

    void reload() {
        cancelQueue = new HashSet<UUID>();
        releaseQueue = new HashSet<UUID>();
        tameQueue = new HashSet<UUID>();
        this.reloadConfig();
        message.reload();
    }

    static long getTimeout() {
       return timeout;
    }

    private void tameAnimal(Player player, Tameable animal) {
        if(animal instanceof Horse) {
            ((Horse)animal).setDomestication(((Horse)animal).getMaxDomestication());
        }

        animal.setOwner(player);

        if(animal instanceof Ocelot) {
            int rand = (int)(Math.random()*2);
            Ocelot.Type type;
            switch (rand) {
                case 0:
                    type = Ocelot.Type.BLACK_CAT;
                    break;
                case 1:
                    type = Ocelot.Type.SIAMESE_CAT;
                    break;
                default:
                    type = Ocelot.Type.RED_CAT;
            }
            ((Ocelot)animal).setCatType(type);
            ((Ocelot)animal).setSitting(true);
        }

        if(animal instanceof Wolf) {
            ((Wolf)animal).setSitting(true);
        }
    }

    private void releaseAnimal(Tameable animal) {
        if(animal instanceof Ocelot) {
            ((Ocelot)animal).setCatType(Ocelot.Type.WILD_OCELOT);
            ((Ocelot)animal).setSitting(false);
        }

        if(animal instanceof Wolf) {
            ((Wolf)animal).setSitting(false);
        }

        if(animal instanceof Horse) {
            ((Horse)animal).setCarryingChest(false);
            ((Horse)animal).getInventory().setArmor(new ItemStack(Material.AIR));
            ((Horse)animal).getInventory().setSaddle(new ItemStack(Material.AIR));
            ((Horse)animal).setDomestication(0);
        }
        ((LivingEntity)animal).setLeashHolder(null);
        (animal).setOwner(null);
    }

    private class CancelListner implements Listener {
        @EventHandler(priority = EventPriority.HIGH)
        private void onPlayerCancelAnimalActions(PlayerInteractEntityEvent e) {
            Player player = e.getPlayer();
            Entity entity = e.getRightClicked();

            if(!(entity instanceof Tameable)) { return; }

            if(((Tameable)entity).isTamed()) {
                if(tameQueue.contains(player.getUniqueId())) {
                    player.sendMessage(Message.TAME_ERROR.get());
                    tameQueue.remove(player.getUniqueId());
                    e.setCancelled(true);
                }

                if(cancelQueue.contains(player.getUniqueId())) {
                    if(Validate.isOwner(player, (Tameable) entity)) {
                        give.cancel(player, entity);
                        if(sales != null) {
                            sales.cancel(player, entity);
                        }
                    }

                    e.setCancelled(true);
                    cancelQueue.remove(player.getUniqueId());
                    return;
                }

                if(releaseQueue.contains(player.getUniqueId())) {
                    // Check the flag
                    if(releaseFlag != null) {
                        Flag flag = (Flag)releaseFlag;
                        Area area = io.github.alshain01.flags.System.getActive().getAreaAt((e.getRightClicked().getLocation()));
                        if(!area.getValue(flag, false)
                                && (!player.hasPermission(flag.getBypassPermission())
                                || !area.hasTrust(flag, player))) {
                            player.sendMessage(area.getMessage(flag, player.getName()));
                            releaseQueue.remove(player.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                    }

                    if(Validate.isOwner(player, (Tameable) entity)) {
                        releaseAnimal((Tameable) entity);
                    }
                    e.setCancelled(true);
                    releaseQueue.remove(player.getUniqueId());
                }
            } else {
                if(tameQueue.contains(player.getUniqueId())) {
                    tameAnimal(player, (Tameable)entity);
                    tameQueue.remove(player.getUniqueId());
                    e.setCancelled(true);
                }
            }

        }
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
        return sales.getCount();
    }

    public int getGiveCount() {
        return give.getCount();
    }

}
