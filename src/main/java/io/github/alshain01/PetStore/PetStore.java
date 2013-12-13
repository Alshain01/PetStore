package io.github.alshain01.PetStore;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PetStore extends JavaPlugin {
    //<Sender, Receiver>
    private ConcurrentHashMap<UUID, String> giveQueue = new ConcurrentHashMap<UUID, String>();

    //<Owner, Cost>
    private ConcurrentHashMap<UUID, Double> sellQueue = new ConcurrentHashMap<UUID, Double>();

    //<Buyer, Animal>
    private ConcurrentHashMap<UUID, UUID> buyQueue = new ConcurrentHashMap<UUID, UUID>();

    //<Animal, Cost>
    private ConcurrentHashMap<UUID, Double> sales = new ConcurrentHashMap<UUID, Double>();

    private Economy economy = null;

    @Override
    public void onEnable() {
        Map<String, Object> readSales =
                new CustomYML(this, "data.yml").getConfig().getConfigurationSection("Sales").getValues(false);
        for(String s : readSales.keySet()) {
            sales.put(UUID.fromString(s), (Double) readSales.get(s));
        }


        Bukkit.getServer().getPluginManager().registerEvents(new Handlers(), this);

        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Vault")) {
            final RegisteredServiceProvider<Economy> economyProvider = Bukkit
                .getServer().getServicesManager()
                .getRegistration(Economy.class);

            if (economyProvider != null) {
                economy = economyProvider.getProvider();
            }
        }
    }

    @Override
    public void onDisable() {
        Map<String, Object> writeSales = new HashMap<String, Object>();
        for(UUID u : sales.keySet()) {
            writeSales.put(u.toString(), sales.get(u));
        }
        new CustomYML(this, "data.yml").getConfig().set("Sales", writeSales);
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if(!cmd.getName().equalsIgnoreCase("petstore") || args.length < 2) {
            return false;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "PetStore commands may not be used from the console.");
        }

        if(args[0].equalsIgnoreCase("give")) {
            Player player = Bukkit.getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player could not be found on the server.");
                return false;
            }

            if(giveQueue.contains(((Player) sender).getUniqueId())) {
                giveQueue.remove(((Player) sender).getUniqueId());
            }

            giveQueue.put(((Player) sender).getUniqueId(), player.getName());
            sender.sendMessage(ChatColor.YELLOW + "Right click the tamed animal you wish to give.");
            new BukkitRunnable() {
                public void run() {
                    if(giveQueue.contains(((Player) sender).getUniqueId())) {
                        giveQueue.remove(((Player) sender).getUniqueId());
                        sender.sendMessage(ChatColor.BLUE + "Animal transfer timed out.");
                    }
                }
            }.runTaskLater(this, 100);
        }

        if(args[0].equalsIgnoreCase("sell")) {
            if(economy == null) {
                sender.sendMessage("Vault is not configured on this server.");
                return true;
            }

            Double price;
            try {
                price = Double.valueOf(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Please enter a valid price.");
                return true;
            }

            sellQueue.put(((Player)sender).getUniqueId(), price);
            sender.sendMessage(ChatColor.YELLOW + "Right click the tamed animal you wish to sell.");
            new BukkitRunnable() {
                public void run() {
                    if(sellQueue.contains(((Player) sender).getUniqueId())) {
                        sellQueue.remove(((Player) sender).getUniqueId());
                        sender.sendMessage(ChatColor.BLUE + "Animal sell timed out.");
                    }
                }
            }.runTaskLater(this, 100);
        }

        return false;
    }

    private void giveAnimal(Player player, Tameable animal) {
        Player receiver = Bukkit.getServer().getPlayer(giveQueue.get(player.getUniqueId()));
        if (receiver == null) {
            player.sendMessage(ChatColor.RED + "Player could not be found on the server.");
            giveQueue.remove(player.getUniqueId());
            return;
        }

        animal.setOwner(receiver);
        giveQueue.remove(player.getUniqueId());
        player.sendMessage(ChatColor.DARK_GREEN + "The animal ownership has been transferred to " + receiver.getName() + ".");
        Location petLoc = ((Entity)animal).getLocation();
        receiver.sendMessage(ChatColor.DARK_GREEN + player.getName() + " has given you ownership of an animal currently at "
                + petLoc.getBlockX() + "," + petLoc.getBlockZ() + ".");
    }

    private void sellAnimal(Player player, Tameable animal) {
        double cost = sellQueue.get(player.getUniqueId());
        Entity pet = (Entity)animal;

        if(cost >= 0) {
            if(sales.contains(pet.getUniqueId())) {
                sales.remove(pet.getUniqueId());
            }
            sales.put(pet.getUniqueId(), sellQueue.get(player.getUniqueId()));
            player.sendMessage(ChatColor.BLUE + "The animal has been listed for sale at " + economy.format(cost));
        } else {
            if(sales.contains(pet.getUniqueId())) {
                sales.remove(pet.getUniqueId());
                player.sendMessage(ChatColor.BLUE + "The animal is no longer listed for sale.");
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
                    player.sendMessage(ChatColor.RED + "There was an error processing the transaction.");
                    return;
                }
            }
            animal.setOwner(player);
            player.sendMessage(ChatColor.DARK_GREEN + "Transaction successful.");
            player.sendMessage(ChatColor.DARK_GREEN + player.getName()
                    + " has purchased an animal for " + economy.format(price));
            sales.remove(pet.getUniqueId());
            buyQueue.remove(player.getUniqueId());
        } else {
            if(price > economy.getBalance(player.getName())) {
                player.sendMessage(ChatColor.RED + "This animal costs " + economy.format(price)
                        + ". You do not have enough funds for this purchase.");
            } else {
                player.sendMessage(ChatColor.GOLD + "This animal costs " + economy.format(price)
                        + ". Right click the animal again to purchase.");

                if(buyQueue.contains(player.getUniqueId())) {
                    buyQueue.remove(player.getUniqueId());
                }

                buyQueue.put(player.getUniqueId(), pet.getUniqueId());
                new BukkitRunnable() {
                    public void run() {
                        if(buyQueue.contains(player.getUniqueId())) {
                            sellQueue.remove(player.getUniqueId());
                            player.sendMessage(ChatColor.BLUE + "Transaction timed out.");
                        }
                    }
                }.runTaskLater(this, 100);
            }
        }
    }

    private boolean checkOwner(Player player, Tameable animal) {
        if(!animal.getOwner().equals(player)) {
            player.sendMessage(ChatColor.RED + "You do not own that animal.");
            return false;
        }
        return true;
    }

    private class Handlers implements Listener {
        @EventHandler
        private void onPlayerGiveTameable(PlayerInteractEntityEvent e) {
            if(!(e.getRightClicked() instanceof Tameable) || !((Tameable)e.getRightClicked()).isTamed()) {
                return;
            }

            Tameable pet = (Tameable)e.getRightClicked();

            if(giveQueue.containsKey(e.getPlayer().getUniqueId())) {
                if(checkOwner(e.getPlayer(), pet)) {
                    giveAnimal(e.getPlayer(), pet);
                } else {
                    giveQueue.remove(e.getPlayer().getUniqueId());
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
                    e.getPlayer().sendMessage(ChatColor.BLUE + "This animal has been listed for sale at "
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
