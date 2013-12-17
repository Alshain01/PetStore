package io.github.alshain01.PetStore;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetStore extends JavaPlugin {
    public static final long timeout = 100;
    public static final ChatColor errorColor = ChatColor.RED;
    public static final ChatColor warnColor = ChatColor.YELLOW;
    public static final ChatColor notifyColor = ChatColor.BLUE;
    public static final ChatColor successColor = ChatColor.GREEN;

    private Economy economy = null;
    private TransferAnimal transfer = new TransferAnimal();
    private GiveAnimal give;
    private SellAnimal sales;

    //<Owner, Cost>
    //private ConcurrentHashMap<UUID, Double> sellQueue = new ConcurrentHashMap<UUID, Double>();

    //<Buyer, Animal>
    //private ConcurrentHashMap<UUID, UUID> buyQueue = new ConcurrentHashMap<UUID, UUID>();

    //<Animal, Cost>
    //private ConcurrentHashMap<UUID, Double> sales = new ConcurrentHashMap<UUID, Double>();

    @Override
    public void onEnable() {
        CustomYML yml = new CustomYML(this, "data.yml");

        // Read give aways from file
        give = new GiveAnimal(yml.getConfig().getList("Give", new ArrayList<String>()));

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(transfer, this);
        pm.registerEvents(give, this);

        // Initialize economy
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
            sales = new SellAnimal(yml.getConfig().getMapList("Sales"));
            pm.registerEvents(sales, this);
        }
    }

    @Override
    public void onDisable() {
        CustomYML yml = new CustomYML(this, "data.yml");

        // Write give aways to file
        yml.getConfig().set("Give", give.get());

        // Write sales to file
        if(economy != null) {
            yml.getConfig().set("Sales", sales.get());
        }
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if(!cmd.getName().equalsIgnoreCase("petstore") || args.length < 1) {
            return false;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage(errorColor + "PetStore commands may not be used from the console.");
        }

        // Give command action
        if(args[0].equalsIgnoreCase("give")) {
            give.add(this, (Player)sender);
            return true;
        }

        if(args.length < 2) {
            return false;
        }

        // Transfer command action
        if(args[0].equalsIgnoreCase("transfer")) {
            Player player = Bukkit.getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(errorColor + "Player could not be found on the server.");
                return false;
            }

            transfer.add(this, (Player) sender, player);
            return true;
        }

        // Sell command action
        if(args[0].equalsIgnoreCase("sell")) {
            if(economy == null) {
                sender.sendMessage("Vault is not configured on this server.");
                return true;
            }

            Double price;
            try {
                price = Double.valueOf(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(errorColor + "Please enter a valid price.");
                return true;
            }

            sellQueue.put(((Player)sender).getUniqueId(), price);
            sender.sendMessage(warnColor + "Right click the tamed animal you wish to sell.");
            new BukkitRunnable() {
                public void run() {
                    if(sellQueue.contains(((Player) sender).getUniqueId())) {
                        sellQueue.remove(((Player) sender).getUniqueId());
                        sender.sendMessage(notifyColor + "Animal sell timed out.");
                    }
                }
            }.runTaskLater(this, timeout);
        }

        return false;
    }



    private void sellAnimal(Player player, Tameable animal) {
        double cost = sellQueue.get(player.getUniqueId());
        Entity pet = (Entity)animal;

        if(cost >= 0) {
            if(sales.contains(pet.getUniqueId())) {
                sales.remove(pet.getUniqueId());
            }
            sales.put(pet.getUniqueId(), sellQueue.get(player.getUniqueId()));
            player.sendMessage(notifyColor + "The animal has been listed for sale at " + economy.format(cost));
        } else {
            if(sales.contains(pet.getUniqueId())) {
                sales.remove(pet.getUniqueId());
                player.sendMessage(successColor + "The animal is no longer listed for sale.");
            }
        }
        sellQueue.remove(player.getUniqueId());
    }

    private void buyAnimal(final Player player, Tameable animal) {
        Entity pet = (Entity)animal;
        double price = sales.get(pet.getUniqueId());

        if(buyQueue.contains(player.getUniqueId())
                && sales.get(player.getUniqueId()).equals(pet.getUniqueId())) {
            EconomyResponse r = economy.withdrawPlayer(player.getName(), price);
            if(r.transactionSuccess()) {
                r = economy.depositPlayer(animal.getOwner().getName(), price);
                if(!r.transactionSuccess()) {
                    r = economy.depositPlayer(player.getName(), price);
                    player.sendMessage(warnColor + "There was an error processing the transaction.");
                    return;
                }
            }
            animal.setOwner(player);
            player.sendMessage(successColor + "Transaction successful.");
            player.sendMessage(successColor + player.getName()
                    + " has purchased an animal for " + economy.format(price));
            sales.remove(pet.getUniqueId());
            buyQueue.remove(player.getUniqueId());
        } else {
            if(price > economy.getBalance(player.getName())) {
                player.sendMessage(errorColor + "This animal costs " + economy.format(price)
                        + ". You do not have enough funds for this purchase.");
            } else {
                player.sendMessage(notifyColor + "This animal costs " + economy.format(price)
                        + ". Right click the animal again to purchase.");

                if(buyQueue.contains(player.getUniqueId())) {
                    buyQueue.remove(player.getUniqueId());
                }

                buyQueue.put(player.getUniqueId(), pet.getUniqueId());
                new BukkitRunnable() {
                    public void run() {
                        if(buyQueue.contains(player.getUniqueId())) {
                            sellQueue.remove(player.getUniqueId());
                            player.sendMessage(notifyColor + "Transaction timed out.");
                        }
                    }
                }.runTaskLater(this, timeout);
            }
        }
    }

    private class Handlers implements Listener {
        @EventHandler
        private void onPlayerTransferTameable(PlayerInteractEntityEvent e) {
            if(!(e.getRightClicked() instanceof Tameable) || !((Tameable)e.getRightClicked()).isTamed()) {
                return;
            }

            Tameable pet = (Tameable)e.getRightClicked();

            if(transferQueue.containsKey(e.getPlayer().getUniqueId())) {
                if(transferQueue.get(e.getPlayer().getUniqueId()) != null) {
                    if(checkOwner(e.getPlayer(), pet)) {
                        transferAnimal(e.getPlayer(), pet);
                    } else {
                        transferQueue.remove(e.getPlayer().getUniqueId());
                    }
                } else {
                    give.add(((Entity)pet).getUniqueId());
                    e.getPlayer().sendMessage(successColor + "The animal is now available to be claimed.");
                }
                e.setCancelled(true);
                return;
            }

            if(sellQueue.containsKey(e.getPlayer().getUniqueId())) {
                if(checkOwner(e.getPlayer(), pet)) {
                    sellAnimal(e.getPlayer(), pet);
                } else {
                    sellQueue.remove(e.getPlayer().getUniqueId());
                }
                e.setCancelled(true);
                return;
            }

            if(sales.contains(((Entity) pet).getUniqueId())) {
                if(pet.getOwner().equals(e.getPlayer())) {
                    e.getPlayer().sendMessage(notifyColor + "This animal has been listed for sale at "
                            + economy.format(sales.get(((Entity)pet).getUniqueId())));
                } else {
                    buyAnimal(e.getPlayer(), pet);
                }
                e.setCancelled(true);
                return;
            }
        }
    }
}
