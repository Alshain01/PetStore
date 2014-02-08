package io.github.alshain01.petstore;

import io.github.alshain01.flags.Flag;
import io.github.alshain01.flags.Flags;
import io.github.alshain01.flags.area.Area;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import io.github.alshain01.flags.System;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SellAnimal implements Listener, ConfigurationSerializable {
    //<Owner, Cost>
    private final Map<UUID, Double> sellQueue = new HashMap<UUID, Double>();

    //<Buyer, Animal>
    private final Map<UUID, UUID> buyQueue = new HashMap<UUID, UUID>();

    //<Animal, Cost>
    private final Map<UUID, Double> sales = new HashMap<UUID, Double>();

    private final JavaPlugin plugin;
    private final Economy economy;
    private Object flag = null;

    public SellAnimal(final JavaPlugin plugin, final Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        if(Bukkit.getServer().getPluginManager().isPluginEnabled("Flags")) {
            this.flag = Flags.getRegistrar().getFlag("SellPet");
        }
    }

    public SellAnimal(final JavaPlugin plugin, final Economy economy, final Map<String, Object> map) {
        this.plugin = plugin;
        this.economy = economy;

        if(Bukkit.getServer().getPluginManager().isPluginEnabled("Flags")) {
            this.flag = Flags.getRegistrar().getFlag("SellPet");
        }

        for(String s : map.keySet()) {
            sales.put(UUID.fromString(s), (Double)map.get(s));
        }
    }

    @Override
    public Map<String, Object> serialize() {
        final Map<String, Object> serialized = new HashMap<String, Object>();

        for(UUID u : sales.keySet()) {
            serialized.put(u.toString(), sales.get(u));
        }
        return serialized;
    }

    public int getCount() {
        return sales.size();
    }

    public void add(final Player player, final double price) {
        final UUID pID = player.getUniqueId();

        sellQueue.put(pID, price);
        player.sendMessage(Message.SELL_INSTRUCTION.get());
        new BukkitRunnable() {
            public void run() {
                if(sellQueue.containsKey(pID)) {
                    sellQueue.remove(pID);
                    player.sendMessage(Message.SELL_TIMEOUT.get());
                }
            }
        }.runTaskLater(plugin, PetStore.getTimeout());
    }

    public void cancel(final Player player, final Entity entity) {
        if(sales.containsKey(entity.getUniqueId())) {
            sales.remove(entity.getUniqueId());
            player.sendMessage(Message.SELL_CANCEL.get());
        }
    }

    private void sellAnimal(final Player player, final Tameable animal) {
        final UUID pID = player.getUniqueId();
        final UUID aID = ((Entity)animal).getUniqueId();
        final double cost = sellQueue.get(pID);

        if(cost > 0D) {
            if(sales.containsKey(aID)) { sales.remove(aID); }
            sales.put(aID, sellQueue.get(pID));
            player.sendMessage(Message.SELL_SET.get().replaceAll("\\{Price\\}", economy.format(cost)));
        } else {
            if(sales.containsKey(aID)) {
                sales.remove(aID);
                player.sendMessage(Message.SELL_CANCEL.get());
            }
        }
        sellQueue.remove(pID);
    }

    private void buyAnimal(final Player player, final Tameable animal) {
        final UUID pID = player.getUniqueId();
        final UUID aID = ((Entity)animal).getUniqueId();
        double price = sales.get(aID);

        if(buyQueue.containsKey(pID) && buyQueue.get(pID).equals(aID)) {
            EconomyResponse r = economy.withdrawPlayer(player.getName(), price);
            if(r.transactionSuccess()) {
                r = economy.depositPlayer(animal.getOwner().getName(), price);
                if(!r.transactionSuccess()) {
                    economy.depositPlayer(player.getName(), price); // Put it back.
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
            sales.remove(aID);
            buyQueue.remove(pID);
        } else {
            if(price > economy.getBalance(player.getName())) {
                player.sendMessage(Message.BUY_LOW_FUNDS.get()
                        .replaceAll("\\{Price\\}", economy.format(price)));
            } else {
                player.sendMessage(Message.BUY_INSTRUCTION.get()
                        .replaceAll("\\{Price\\}", economy.format(price)));

                if(buyQueue.containsKey(player.getUniqueId())) {
                    buyQueue.remove(player.getUniqueId());
                }

                buyQueue.put(player.getUniqueId(), aID);
                new BukkitRunnable() {
                    public void run() {
                        if(buyQueue.containsKey(pID)) {
                            sellQueue.remove(pID);
                            player.sendMessage(Message.BUY_TIMEOUT.get());
                        }
                    }
                }.runTaskLater(plugin, PetStore.getTimeout());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerSellAnimal(final PlayerInteractEntityEvent e) {
        if(Validate.isUntamedAnimal(e.getRightClicked())) { return; }

        final UUID aID = (e.getRightClicked().getUniqueId());
        final UUID pID = (e.getPlayer().getUniqueId());
        final Tameable pet = (Tameable)e.getRightClicked();

        if(sales.containsKey(aID)) {
            if(pet.getOwner().equals(e.getPlayer())) {
                e.getPlayer().sendMessage(Message.SELL_SET.get()
                        .replaceAll("\\{Price\\}", economy.format(sales.get(aID))));
            } else {
                    buyAnimal(e.getPlayer(), pet);
            }
            e.setCancelled(true);
            return;
        }

        if(sellQueue.containsKey(pID)) {
            Player player = e.getPlayer();
            if(Validate.isOwner(player, pet)) {
                // Check the flag
                if(flag != null) {
                    Flag salesFlag = (Flag)flag;
                    Area area = System.getActive().getAreaAt(((Entity)pet).getLocation());
                    if(!area.getValue(salesFlag, false)
                            && (!player.hasPermission(salesFlag.getBypassPermission())
                            || !area.hasTrust(salesFlag, player))) {
                        player.sendMessage(area.getMessage(salesFlag, player.getName()));
                        sellQueue.remove(pID);
                        e.setCancelled(true);
                        return;
                    }
                }
                // Sell the animal
                sellAnimal(e.getPlayer(), pet);
            }
            sellQueue.remove(pID);
            e.setCancelled(true);
        }
    }
}
