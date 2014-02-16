package io.github.alshain01.petstore;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

final class AnimalListener implements Listener {
    private final PetStore plugin;

    AnimalListener(PetStore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onPlayerCancelAnimalActions(PlayerInteractEntityEvent e) {
        final Player player = e.getPlayer();
        final Entity entity = e.getRightClicked();
        final UUID pID = player.getUniqueId(),  aID = entity.getUniqueId();

        if(!(entity instanceof Tameable)
                || !(plugin.commandQueue.containsKey(pID)
                || (plugin.forSale != null && plugin.forSale.containsKey(aID))
                || plugin.forClaim.contains(aID))) { return; }

        Tameable tameable = (Tameable)entity;
        if(!tameable.isTamed()) {
            if(PluginCommandType.TAME.equals(plugin.commandQueue.get(pID))) {
                Animal.tame(player, tameable);
                plugin.commandQueue.remove(player.getUniqueId());
                e.setCancelled(true);
                return;
            }
        }

        if(plugin.commandQueue.containsKey(pID)) {
            switch(plugin.commandQueue.get(pID)) {
                case CANCEL:
                    if(Animal.isOwner(player, tameable)) {
                        if(plugin.forClaim.remove(entity.getUniqueId())) {
                            player.sendMessage(Message.GIVE_CANCEL.get());
                        }
                        if(PetStore.isEconomy()) {
                            Animal.sell(plugin.economy, player, tameable, null, 0D);
                        }
                    }
                    break;
                case TAME:
                    player.sendMessage(Message.TAME_ERROR.get());
                    break;
                case RELEASE:
                    if(Animal.release(player, plugin.flags.get("ReleasePet"), tameable)) {
                        clearState(aID);
                    }
                    break;
                case TRANSFER:
                    if(Animal.transfer(player, tameable,plugin.flags.get("TransferPet"), plugin.transferQueue.get(pID))) {
                        clearState(aID);
                    }
                    break;
                case GIVE:
                    if(Animal.give(player, plugin.flags.get("GivePet"), tameable)) {
                        clearState(aID);
                        plugin.forClaim.add(((Entity)tameable).getUniqueId());
                    }
                    break;
                case CLAIM:
                    if(plugin.claimQueue.get(pID).equals(aID)) {
                        Animal.claim(player, tameable);
                        plugin.forClaim.remove(aID);
                    }
                    break;
                case SELL:
                    if(PetStore.isEconomy()) {
                        Boolean success = Animal.sell(plugin.economy, player, tameable, plugin.flags.get("SellPet"), plugin.sellQueue.get(pID));
                        if(success == null) { break; }
                        clearState(aID);
                        if(success) {
                            plugin.forSale.put(aID,  plugin.sellQueue.get(pID));
                        }
                        break;
                    }
                    break;
                case BUY:
                    if(plugin.buyQueue.get(pID).equals(aID)) {
                        if(Animal.buy(plugin.economy, player, tameable, plugin.forSale.get(aID))) {
                            clearState(aID);
                        }
                    }
                    break;
                default:
                    break;
            }
            plugin.commandQueue.remove(player.getUniqueId());
            e.setCancelled(true);
            return;
        }

        // Handles advertising and instructions for Claim and Buy
        if(plugin.forClaim.contains(aID)) {
            if(!Animal.requestClaim(player, tameable)) { return; }
            plugin.commandQueue.put(pID, PluginCommandType.CLAIM);
            plugin.claimQueue.put(pID, aID);
            new TimeoutTask(plugin, PluginCommandType.CLAIM, player).runTaskLater(plugin, plugin.timeout);
            e.setCancelled(true);
            return;
        }

        if(PetStore.isEconomy() && plugin.forSale.containsKey(aID)) {
            if(!Animal.advertise(plugin, player, tameable, plugin.forSale.get(aID))) { return; }
            plugin.commandQueue.put(pID, PluginCommandType.BUY);
            plugin.buyQueue.put(pID, aID);
            new TimeoutTask(plugin, PluginCommandType.BUY, player).runTaskLater(plugin, plugin.timeout);
            e.setCancelled(true);
        }
    }

    void clearState(UUID aID) {
        if(PetStore.isEconomy()) {
            plugin.forSale.remove(aID);
        }
        plugin.forClaim.remove(aID);
    }
}
