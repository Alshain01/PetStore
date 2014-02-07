package io.github.alshain01.petstore;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.alshain01.petstore.Updater.UpdateResult;

import java.io.IOException;
import java.util.*;

public class PetStore extends JavaPlugin {
    private TransferAnimal transfer = new TransferAnimal();
    private GiveAnimal give = null;
    private SellAnimal sales = null;
    private Updater updater = null;
    protected CustomYML message;

    private Set<UUID> cancelQueue = new HashSet<UUID>();
    private Set<UUID> releaseQueue = new HashSet<UUID>();
    private Set<UUID> tameQueue = new HashSet<UUID>();

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
            }
        }

        // Read give aways from file
        give = new GiveAnimal(yml.getConfig().getList("Give", new ArrayList<String>()));

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new CancelListner(), this);
        pm.registerEvents(transfer, this);
        pm.registerEvents(give, this);

        // Read sales from file
        if(economy != null) {
            if(yml.getConfig().isConfigurationSection("Sales")) {
                sales = new SellAnimal(economy, yml.getConfig().getConfigurationSection("Sales").getValues(false));
            } else {
                sales = new SellAnimal(economy);
            }

            pm.registerEvents(sales, this);
        }

        if (getConfig().getBoolean("Update.Check")) {
            new UpdateScheduler().runTaskTimer(this, 0, 1728000);
            getServer().getPluginManager().registerEvents(new UpdateListener(), this);
        }

        if(this.getConfig().getBoolean("Metrics.Enabled")) {
            try {
                new MetricsLite(this).start();
            } catch (IOException ex) {
                this.getLogger().warning("Metrics failed to start.");
            }
        }
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

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        // Check that it is our command and has the action
        if(!cmd.getName().equalsIgnoreCase("petstore") || args.length < 1) {
            return false;
        }

        // All of the command require an animal entity to be identified
        if(!(sender instanceof Player)) {
            sender.sendMessage(Message.CONSOLE_ERROR.get());
        }

        final Player player = (Player) sender;
        CommandAction action;

        // Get the action.
        try {
            action = CommandAction.valueOf(args[0].toUpperCase());
        } catch(IllegalArgumentException e) {
            sender.sendMessage(getHelp(player));
            return true;
        }

        // Check that there are enough arguments
        if(args.length < action.getTotalArgs()) {
            player.sendMessage(action.getHelp());
            return true;
        }

        // Check the permissions
        if(!action.hasPermission(player)) {
            player.sendMessage(Message.COMMAND_ERROR.get());
            return true;
        }

        // Perform the command
        switch (action) {
            case GIVE:
                give.add((Player)sender);
                return true;
            case TRANSFER:
                Player receiver = Bukkit.getServer().getPlayer(args[1]);
                if (receiver == null) {
                    player.sendMessage(Message.PLAYER_ERROR.get());
                } else {
                    transfer.add(player, receiver);
                }
                return true;
            case SELL:
                if(sales == null) {
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

                sales.add(player, price);
                return true;
            case TAME:
                tameQueue.add(player.getUniqueId());
                new BukkitRunnable() {
                    public void run() {
                        if(tameQueue.contains(player.getUniqueId())) {
                            tameQueue.remove(player.getUniqueId());
                            player.sendMessage(Message.TAME_TIMEOUT.get());
                        }
                    }
                }.runTaskLater(this, getTimeout());
                return true;
            case RELEASE:
                releaseQueue.add(player.getUniqueId());
                new BukkitRunnable() {
                    public void run() {
                        if(releaseQueue.contains(player.getUniqueId())) {
                            releaseQueue.remove(player.getUniqueId());
                            player.sendMessage(Message.RELEASE_TIMEOUT.get());
                        }
                    }
                }.runTaskLater(this, getTimeout());
                return true;
            case CANCEL:
                if(!cancelQueue.contains(player.getUniqueId())) {
                    cancelQueue.add(player.getUniqueId());
                    new BukkitRunnable() {
                        public void run() {
                            if(cancelQueue.contains(player.getUniqueId())) {
                                cancelQueue.remove(player.getUniqueId());
                                player.sendMessage(Message.CANCEL_TIMEOUT.get());
                            }
                        }
                    }.runTaskLater(this, getTimeout());
                }
                sender.sendMessage(Message.CANCEL_INSTRUCTION.get());
                return true;
            case RELOAD:
                this.reload();
                return true;
            case SAVE:
                CustomYML yml = new CustomYML(this, "data.yml");

                // Write give aways to file
                yml.getConfig().set("Give", give.get());

                // Write sales to file
                if(sales != null) {
                    yml.getConfig().set("Sales", sales.serialize());
                }
                yml.saveConfig();
                return true;
            default:
                player.sendMessage(action.getHelp());
                return true;
        }
    }

    private void reload() {
        cancelQueue = new HashSet<UUID>();
        releaseQueue = new HashSet<UUID>();
        tameQueue = new HashSet<UUID>();
        this.reloadConfig();
        message.reload();
    }

    private String getHelp(Permissible player) {
        StringBuilder helpText = new StringBuilder("/petstore <");
        boolean first = true;
        for(CommandAction a : CommandAction.values()) {
            if(a.hasPermission(player)) {
                if(a != CommandAction.SELL || sales != null) {
                    if(!first) { helpText.append(" | "); }
                    helpText.append(a.toString().toLowerCase());
                    first = false;
                }
            }
        }
        return helpText.append(">").toString();
    }

    protected static long getTimeout() {
       return Bukkit.getPluginManager().getPlugin("PetStore").getConfig().getLong("CommandTimeout");
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
        @EventHandler
        private void onPlayerCancelAnimalActions(PlayerInteractEntityEvent e) {
            Player player = e.getPlayer();
            Entity entity = e.getRightClicked();

            if(!(entity instanceof Tameable)) { return; }

            if(((Tameable)entity).isTamed()) {
                if(cancelQueue.contains(player.getUniqueId())) {
                    if(Validate.owner(player, (Tameable)entity)) {
                        give.cancel(player, entity);
                        if(sales != null) {
                            sales.cancel(player, entity);
                        }
                    }

                    cancelQueue.remove(player.getUniqueId());
                    return;
                }

                if(releaseQueue.contains(player.getUniqueId())) {
                    if(Validate.owner(player, (Tameable)entity)) {
                        releaseAnimal((Tameable) entity);
                    }
                    releaseQueue.remove(player.getUniqueId());
                }
            } else {
                if(tameQueue.contains(player.getUniqueId())) {
                    tameAnimal(player, (Tameable)entity);
                    tameQueue.remove(player.getUniqueId());
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
}
