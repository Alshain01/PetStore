package io.github.alshain01.petstore;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

class AnimalListener implements Listener {
    private PetStore plugin;

    AnimalListener(PetStore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlayerCancelAnimalActions(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        UUID pID = player.getUniqueId();
        Entity entity = e.getRightClicked();


        if((entity instanceof Tameable)
                && ( plugin.commandQueue.containsKey(pID)
                  || plugin.transferQueue.containsKey(pID)
                  || plugin.sellQueue.containsKey(pID) )) {

            Tameable tameable = (Tameable)entity;

            if(tameable.isTamed()) {
                if(PluginCommandType.TAME.equals(plugin.commandQueue.get(pID))) {
                    player.sendMessage(Message.TAME_ERROR.get());
                    plugin.commandQueue.remove(player.getUniqueId());
                    e.setCancelled(true);
                    return;
                }

                if(PluginCommandType.RELEASE.equals(plugin.commandQueue.get(pID))) {
                    Animal.release(player, plugin.releaseFlag, tameable);
                    plugin.commandQueue.remove(pID);
                    e.setCancelled(true);
                    return;
                }

                if(plugin.transferQueue.containsKey(pID)) {
                    Animal.transfer(player, tameable, plugin.transferQueue.get(pID));
                    plugin.transferQueue.remove(pID);
                    e.setCancelled(true);
                    return;
                }

                if(PluginCommandType.CANCEL.equals(plugin.commandQueue.get(pID))) {
                    if(Animal.isOwner(player, tameable)) {
                        plugin.give.cancel(player, entity);
                        if(plugin.sales != null) {
                            plugin.sales.cancel(player, entity);
                        }
                    }
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
