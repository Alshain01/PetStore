package io.github.alshain01.petstore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public class TransferAnimal implements Listener {
    private Map<UUID, String> queue = new HashMap<UUID, String>();

    public void add(final Player owner, final Player receiver) {
        if(queue.containsKey(owner.getUniqueId())) {
            queue.remove(owner.getUniqueId());
        }
        queue.put(owner.getUniqueId(), receiver.getName());
        owner.sendMessage(Message.TRANSFER_INSTRUCTION.get());

        new BukkitRunnable() {
            public void run() {
                if(queue.containsKey(owner.getUniqueId())) {
                    queue.remove(owner.getUniqueId());
                    owner.sendMessage(Message.TRANSFER_TIMEOUT.get());
                }
            }
        }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), PetStore.getTimeout());
    }

    private void transferAnimal(Player owner, Tameable animal) {
        Player receiver = Bukkit.getServer().getPlayer(queue.get(owner.getUniqueId()));

        if (receiver == null) {
            owner.sendMessage(Message.PLAYER_ERROR.get());
            queue.remove(owner.getUniqueId());
            return;
        }

        animal.setOwner(receiver);
        queue.remove(owner.getUniqueId());

        Location petLoc = ((Entity)animal).getLocation();
        owner.sendMessage(Message.TRANSFER_NOTIFY_OWNER.get().replaceAll("\\{Player\\}", receiver.getName()));
        receiver.sendMessage(Message.TRANSFER_NOTIFY_RECEIVER.get()
                .replaceAll("\\{Player\\}", owner.getName()
                .replaceAll("\\{Location\\}",  petLoc.getBlockX() + ", " + petLoc.getBlockZ())));
    }

    @EventHandler
    private void onPlayerTransferAnimal(PlayerInteractEntityEvent e) {
        if(queue.containsKey(e.getPlayer().getUniqueId())) {
            if(!(e.getRightClicked() instanceof Tameable) || !((Tameable)e.getRightClicked()).isTamed()) { return; }

            if(!Validate.owner(e.getPlayer(), (Tameable)e.getRightClicked())) {
                queue.remove(e.getPlayer().getUniqueId());
                return;
            }

            transferAnimal(e.getPlayer(), (Tameable)e.getRightClicked());
            e.setCancelled(true);
        }
    }
}
