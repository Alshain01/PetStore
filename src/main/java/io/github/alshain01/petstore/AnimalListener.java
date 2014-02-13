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
    private PetStore plugin;

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
                  || plugin.sellQueue.containsKey(pID) )) {

            Tameable tameable = (Tameable)entity;

            if(tameable.isTamed()) {
                if(plugin.forSale != null && plugin.forSale.containsKey(aID)) {
                    if(plugin.buyQueue.containsKey(pID)) {
                         if(Animal.buy(plugin, player, tameable, plugin.forSale.get(aID))) {
                             plugin.forSale.remove(aID);
                             plugin.buyQueue.remove(pID);
                         }
                    } else {
                        if(Animal.advertise(plugin, player, plugin.forSale.get(aID))) {
                            plugin.buyQueue.put(pID, aID);
                            new TimeoutTask(plugin, PluginCommandType.BUY, player).runTaskLater(plugin, PetStore.getTimeout());
                        }
                    }
                    e.setCancelled(true);
                    return;
                }

                if(plugin.commandQueue.get(pID) != null) {
                    switch(plugin.commandQueue.get(pID)) {
                        case TAME:
                            player.sendMessage(Message.TAME_ERROR.get());
                            break;
                        case RELEASE:
                            Animal.release(player, plugin.flags.get("ReleasePet"), tameable);
                            break;
                        case CANCEL:
                            //TODO Fix this for new system
                            if(Animal.isOwner(player, tameable)) {
                                plugin.give.cancel(player, entity);
                                Animal.sell(plugin, player, tameable, null, 0D);
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
                    Animal.transfer(player, tameable, plugin.transferQueue.get(pID));
                    plugin.transferQueue.remove(pID);
                    e.setCancelled(true);
                    return;
                }

                if(plugin.sellQueue.containsKey(pID)) {
                    Animal.sell(plugin, player, tameable, plugin.flags.get("SellPet"), plugin.sellQueue.get(pID));
                    plugin.sellQueue.remove(pID);
                    e.setCancelled(true);
                }
            } else {
                if(PluginCommandType.TAME.equals(plugin.commandQueue.get(pID))) {
                    Animal.tame(player, tameable);
                    plugin.commandQueue.remove(player.getUniqueId());
                    e.setCancelled(true);
                }
            }
        }
    }
}
