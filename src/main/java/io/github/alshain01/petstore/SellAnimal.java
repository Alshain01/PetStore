package io.github.alshain01.PetStore;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SellAnimal implements Listener, ConfigurationSerializable {
    //<Owner, Cost>
    private Map<UUID, Double> sellQueue = new HashMap<UUID, Double>();

    //<Buyer, Animal>
    private Map<UUID, UUID> buyQueue = new HashMap<UUID, UUID>();

    //<Animal, Cost>
    private Map<UUID, Double> sales = new HashMap<UUID, Double>();

    private Economy economy;

    public SellAnimal(Economy economy) {
        this.economy = economy;
    }

    public SellAnimal(Economy economy, Map<String, Object> map) {
        this.economy = economy;
        for(String s : map.keySet()) {
            sales.put(UUID.fromString(s), (Double)map.get(s));
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<String, Object>();
        for(UUID u : sales.keySet()) {
            serialized.put(u.toString(), sales.get(u));
        }
        return serialized;
    }

    public void add(final Player player, final double price) {
        sellQueue.put(player.getUniqueId(), price);
        player.sendMessage(PetStore.warnColor + "Right click the tamed animal you wish to sell.");
        new BukkitRunnable() {
            public void run() {
                if(sellQueue.containsKey(player.getUniqueId())) {
                    sellQueue.remove(player.getUniqueId());
                    player.sendMessage(PetStore.notifyColor + "Sell animal timed out.");
                }
            }
        }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), PetStore.timeout);
    }

    public void cancel(Player player, Entity entity) {
        if(sales.containsKey(entity.getUniqueId())) {
            sales.remove(entity.getUniqueId());
            player.sendMessage(PetStore.successColor + "The animal is no longer for sale.");
        }
    }

    private void sellAnimal(Player player, Tameable animal) {
        double cost = sellQueue.get(player.getUniqueId());
        Entity pet = (Entity)animal;

        if(cost > 0D) {
            if(sales.containsKey(pet.getUniqueId())) {
                sales.remove(pet.getUniqueId());
            }
            sales.put(pet.getUniqueId(), sellQueue.get(player.getUniqueId()));
            player.sendMessage(PetStore.notifyColor + "The animal has been listed for sale at " + economy.format(cost));
        } else {
            if(sales.containsKey(pet.getUniqueId())) {
                sales.remove(pet.getUniqueId());
                player.sendMessage(PetStore.successColor + "The animal is no longer listed for sale.");
            }
        }
        sellQueue.remove(player.getUniqueId());
    }

    private void buyAnimal(final Player player, Tameable animal) {
        Entity pet = (Entity)animal;
        double price = sales.get(pet.getUniqueId());

        if(buyQueue.containsKey(player.getUniqueId())
                && buyQueue.get(player.getUniqueId()).equals(pet.getUniqueId())) {
            EconomyResponse r = economy.withdrawPlayer(player.getName(), price);
            if(r.transactionSuccess()) {
                r = economy.depositPlayer(animal.getOwner().getName(), price);
                if(!r.transactionSuccess()) {
                    economy.depositPlayer(player.getName(), price);
                    player.sendMessage(PetStore.warnColor + "There was an error processing the transaction.");
                    return;
                }
            }
            animal.setOwner(player);
            player.sendMessage(PetStore.successColor + "Transaction successful.");
            player.sendMessage(PetStore.successColor + player.getName()
                    + " has purchased an animal for " + economy.format(price));
            sales.remove(pet.getUniqueId());
            buyQueue.remove(player.getUniqueId());
        } else {
            if(price > economy.getBalance(player.getName())) {
                player.sendMessage(PetStore.errorColor + "This animal costs " + economy.format(price)
                        + ". You do not have enough funds for this purchase.");
            } else {
                player.sendMessage(PetStore.notifyColor + "This animal costs " + economy.format(price)
                        + ". Right click the animal again to purchase.");

                if(buyQueue.containsKey(player.getUniqueId())) {
                    buyQueue.remove(player.getUniqueId());
                }

                buyQueue.put(player.getUniqueId(), pet.getUniqueId());
                new BukkitRunnable() {
                    public void run() {
                        if(buyQueue.containsKey(player.getUniqueId())) {
                            sellQueue.remove(player.getUniqueId());
                            player.sendMessage(PetStore.notifyColor + "Transaction timed out.");
                        }
                    }
                }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), PetStore.timeout);
            }
        }
    }

    @EventHandler
    private void onPlayerSellAnimal(PlayerInteractEntityEvent e) {
        if(!(e.getRightClicked() instanceof Tameable) || !((Tameable)e.getRightClicked()).isTamed()) {
            return;
        }

        Tameable pet = (Tameable)e.getRightClicked();

        if(sales.containsKey(((Entity) pet).getUniqueId())) {
            if(pet.getOwner().equals(e.getPlayer())) {
                e.getPlayer().sendMessage(PetStore.notifyColor + "This animal has been listed for sale at "
                    + economy.format(sales.get(((Entity) pet).getUniqueId())));
            } else {
                    buyAnimal(e.getPlayer(), pet);
            }
            e.setCancelled(true);
            return;
        }

        if(sellQueue.containsKey(e.getPlayer().getUniqueId())) {
            if(Validate.owner(e.getPlayer(), pet)) {
                sellAnimal(e.getPlayer(), pet);
            } else {
                sellQueue.remove(e.getPlayer().getUniqueId());
            }
            e.setCancelled(true);
        }
    }
}
