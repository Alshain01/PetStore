package io.github.alshain01.PetStore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransferAnimal implements Listener {
    private Map<UUID, String> queue = new ConcurrentHashMap<UUID, String>();

    public void add(final Player owner, final Player receiver) {
        if(queue.containsKey(owner.getUniqueId())) {
            queue.remove(owner.getUniqueId());
        }
        queue.put(owner.getUniqueId(), receiver.getName());
        owner.sendMessage(PetStore.warnColor + "Right click the tamed animal you wish to transfer.");

        new BukkitRunnable() {
            public void run() {
                if(queue.containsKey(owner.getUniqueId())) {
                    queue.remove(owner.getUniqueId());
                    owner.sendMessage(PetStore.notifyColor + "Transfer animal timed out.");
                }
            }
        }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), PetStore.timeout);
    }

    private void transferAnimal(Player owner, Tameable animal) {
        Player receiver = Bukkit.getServer().getPlayer(queue.get(owner.getUniqueId()));

        if (receiver == null) {
            owner.sendMessage(PetStore.errorColor + "Player could not be found on the server.");
            queue.remove(owner.getUniqueId());
            return;
        }

        animal.setOwner(receiver);
        queue.remove(owner.getUniqueId());

        Location petLoc = ((Entity)animal).getLocation();
        owner.sendMessage(PetStore.successColor
                + "The animal ownership has been transferred to "
                + receiver.getName() + ".");
        receiver.sendMessage(PetStore.successColor + owner.getName()
                + " has transferred ownership of an animal currently at "
                + petLoc.getBlockX() + "," + petLoc.getBlockZ() + ".");
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
