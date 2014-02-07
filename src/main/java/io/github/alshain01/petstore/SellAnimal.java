package io.github.alshain01.petstore;

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
        player.sendMessage(Message.SELL_INSTRUCTION.get());
        new BukkitRunnable() {
            public void run() {
                if(sellQueue.containsKey(player.getUniqueId())) {
                    sellQueue.remove(player.getUniqueId());
                    player.sendMessage(Message.SELL_TIMEOUT.get());
                }
            }
        }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), PetStore.getTimeout());
    }

    public void cancel(Player player, Entity entity) {
        if(sales.containsKey(entity.getUniqueId())) {
            sales.remove(entity.getUniqueId());
            player.sendMessage(Message.SELL_CANCEL.get());
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
            player.sendMessage(Message.SELL_SET.get().replaceAll("\\{Price\\}", economy.format(cost)));
        } else {
            if(sales.containsKey(pet.getUniqueId())) {
                sales.remove(pet.getUniqueId());
                player.sendMessage(Message.SELL_CANCEL.get());
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
                    player.sendMessage(Message.TRANSACTION_ERROR.get());
                    return;
                }
            }

            player.sendMessage(Message.BUY_NOTIFY_RECEIVER.get());
            if(((Player)animal.getOwner()).isOnline()) {
                player.sendMessage(Message.BUY_NOTIFY_OWNER.get()
                        .replaceAll("\\{Player\\}", player.getName())
                        .replaceAll("\\{Price\\}", economy.format(price)));
            }
            animal.setOwner(player);
            sales.remove(pet.getUniqueId());
            buyQueue.remove(player.getUniqueId());
        } else {
            if(price > economy.getBalance(player.getName())) {
                player.sendMessage(Message.BUY_LOW_FUNDS.get().replaceAll("\\{Price\\}", economy.format(price)));
            } else {
                player.sendMessage(Message.BUY_INSTRUCTION.get()
                        .replaceAll("\\{Price\\}", economy.format(price)));

                if(buyQueue.containsKey(player.getUniqueId())) {
                    buyQueue.remove(player.getUniqueId());
                }

                buyQueue.put(player.getUniqueId(), pet.getUniqueId());
                new BukkitRunnable() {
                    public void run() {
                        if(buyQueue.containsKey(player.getUniqueId())) {
                            sellQueue.remove(player.getUniqueId());
                            player.sendMessage(Message.BUY_TIMEOUT.get());
                        }
                    }
                }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), PetStore.getTimeout());
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
                e.getPlayer().sendMessage(Message.SELL_SET.get()
                        .replaceAll("\\{Price\\}", economy.format(sales.get(((Entity) pet).getUniqueId()))));
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
