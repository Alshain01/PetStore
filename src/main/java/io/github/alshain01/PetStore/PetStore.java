package io.github.alshain01.PetStore;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PetStore extends JavaPlugin {
    public static final long timeout = 100;
    public static final ChatColor errorColor = ChatColor.RED;
    public static final ChatColor warnColor = ChatColor.YELLOW;
    public static final ChatColor notifyColor = ChatColor.BLUE;
    public static final ChatColor successColor = ChatColor.GREEN;

    private TransferAnimal transfer = new TransferAnimal();
    private GiveAnimal give = null;
    private SellAnimal sales = null;

    private Set<UUID> cancelQueue = new HashSet<UUID>();

    @Override
    public void onEnable() {
        CustomYML yml = new CustomYML(this, "data.yml");

        // Read give aways from file
        give = new GiveAnimal(yml.getConfig().getList("Give", new ArrayList<String>()));

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new CancelListner(), this);
        pm.registerEvents(transfer, this);
        pm.registerEvents(give, this);

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

        // Read sales from file
        if(economy != null) {
            sales = new SellAnimal(economy, yml.getConfig().getConfigurationSection("Sales").getValues(false));
            pm.registerEvents(sales, this);
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
        if(!cmd.getName().equalsIgnoreCase("petstore") || args.length < 1) {
            return false;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage(errorColor + "PetStore commands may not be used from the console.");
        }
        final Player player = (Player) sender;

        if(args[0].equalsIgnoreCase("cancel")) {
            if(!cancelQueue.contains(player.getUniqueId())) {
                cancelQueue.add(player.getUniqueId());
                new BukkitRunnable() {
                    public void run() {
                        if(cancelQueue.contains(player.getUniqueId())) {
                            cancelQueue.remove(player.getUniqueId());
                            player.sendMessage(notifyColor + "Cancel animal actions timed out.");
                        }
                    }
                }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), timeout);
            }
            sender.sendMessage(warnColor + "Right click the animal you wish to cancel give or sell actions on.");
            return true;
        }

        // Give command action
        if(args[0].equalsIgnoreCase("give")) {
            give.add((Player)sender);
            return true;
        }

        if(args.length < 2) {
            return false;
        }

        // Transfer command action
        if(args[0].equalsIgnoreCase("transfer")) {
            Player receiver = Bukkit.getServer().getPlayer(args[1]);
            if (receiver == null) {
                player.sendMessage(errorColor + "Player could not be found on the server.");
                return false;
            }

            transfer.add(player, receiver);
            return true;
        }

        // Sell command action
        if(args[0].equalsIgnoreCase("sell")) {
            if(sales == null) {
                player.sendMessage("Vault is not configured on this server.");
                return true;
            }

            Double price;
            try {
                price = Double.valueOf(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(errorColor + "Please enter a valid price.");
                return true;
            }

            sales.add(player, price);
        }
        return false;
    }

    private class CancelListner implements Listener {
        @EventHandler
        private void onPlayerCancelAnimalActions(PlayerInteractEntityEvent e) {
            Entity entity = e.getRightClicked();
            if(!(entity instanceof Tameable) || !((Tameable)entity).isTamed()) { return; }

            Player player = e.getPlayer();
            if(cancelQueue.contains(player.getUniqueId())) {
                if(!Validate.owner(player, (Tameable)entity)) {
                    cancelQueue.remove(player.getUniqueId());
                    return;
                }
                give.cancel(player, entity);
                sales.cancel(player, entity);
                cancelQueue.remove(e.getPlayer().getUniqueId());
            }
        }
    }
}
