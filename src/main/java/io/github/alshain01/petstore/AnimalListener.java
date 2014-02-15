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

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlayerCancelAnimalActions(PlayerInteractEntityEvent e) {
        final Player player = e.getPlayer();
        final UUID pID = player.getUniqueId();
        final Entity entity = e.getRightClicked();
        final UUID aID = entity.getUniqueId();

        if((entity instanceof Tameable)
                && ( plugin.commandQueue.containsKey(pID)
                  || plugin.transferQueue.containsKey(pID)
                  || plugin.sellQueue.containsKey(pID)
                  || plugin.buyQueue.containsKey(pID)
                  || plugin.claimQueue.containsKey(pID)
                  || plugin.forSale.containsKey(aID)
                  || plugin.forClaim.contains(aID))) {

            Tameable tameable = (Tameable)entity;
            if(!tameable.isTamed()) {
                System.out.print("Animal is not tamed");
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
                                Animal.sell(plugin, player, tameable, null, 0D);
                            }
                            plugin.commandQueue.remove(pID);
                        }
                        break;
                    case TAME:
                        player.sendMessage(Message.TAME_ERROR.get());
                        break;
                    case RELEASE:
                        if(Animal.release(player, plugin.flags.get("ReleasePet"), tameable)) {
                            plugin.forClaim.remove(((Entity)tameable).getUniqueId());
                            if(PetStore.isEconomy()) {
                                plugin.forSale.remove(((Entity)tameable).getUniqueId());
                            }
                        }
                        break;
                    case GIVE:
                        if(Animal.give(plugin, player, plugin.flags.get("GivePet"), tameable)) {
                            plugin.forClaim.add(((Entity)tameable).getUniqueId());
                            if(PetStore.isEconomy()) {
                                plugin.forSale.remove(((Entity)tameable).getUniqueId());  // Only one at a time
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

            if(plugin.transferQueue.containsKey(pID)) {
                if(Animal.transfer(player, tameable,plugin.flags.get("TransferPet"), plugin.transferQueue.get(pID))) {
                    plugin.forClaim.remove(((Entity)tameable).getUniqueId());
                    if(PetStore.isEconomy()) {
                        plugin.forSale.remove(((Entity)tameable).getUniqueId());
                    }
                }
                plugin.transferQueue.remove(pID);
                e.setCancelled(true);
                return;
            }

            if(plugin.sellQueue.containsKey(pID)) {
                Animal.sell(plugin, player, tameable, plugin.flags.get("SellPet"), plugin.sellQueue.get(pID));
                plugin.sellQueue.remove(pID);
                e.setCancelled(true);
                return;
            }

            // Claim and Buy should ALWAYS be last
            if(plugin.forClaim.contains(aID)) {
                if(plugin.claimQueue.containsKey(pID) && plugin.claimQueue.get(pID).equals(aID)) {
                    Animal.claim(player, tameable);
                    plugin.claimQueue.remove(pID);
                    plugin.forClaim.remove(aID);
                } else {
                    if(!Animal.requestClaim(player, tameable)) { return; }
                    plugin.claimQueue.put(pID, aID);
                    new TimeoutTask(plugin, PluginCommandType.CLAIM, player).runTaskLater(plugin, plugin.timeout);
                }
                e.setCancelled(true);
                return;
            }

            if(PetStore.isEconomy() && plugin.forSale.containsKey(aID)) {
                if(plugin.buyQueue.containsKey(pID)) {
                     if(Animal.buy(plugin, player, tameable, plugin.forSale.get(aID))) {
                         plugin.forSale.remove(aID);
                         plugin.buyQueue.remove(pID);
                     }
                } else {
                    if(!Animal.advertise(plugin, player, tameable, plugin.forSale.get(aID))) { return; }
                    plugin.buyQueue.put(pID, aID);
                    new TimeoutTask(plugin, PluginCommandType.BUY, player).runTaskLater(plugin, plugin.timeout);
                }
                e.setCancelled(true);
            }
        }
    }
}
