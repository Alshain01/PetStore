package io.github.alshain01.petstore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class TransferAnimal implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, String> queue = new HashMap<UUID, String>();

    TransferAnimal(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void add(final Player owner, final Player receiver) {
        final UUID pID = owner.getUniqueId();

        if(queue.containsKey(pID)) {
            queue.remove(pID);
        }
        queue.put(pID, receiver.getName());
        owner.sendMessage(Message.TRANSFER_INSTRUCTION.get());

        new BukkitRunnable() {
            public void run() {
                if(queue.containsKey(pID)) {
                    queue.remove(pID);
                    owner.sendMessage(Message.TRANSFER_TIMEOUT.get());
                }
            }
        }.runTaskLater(plugin, PetStore.getTimeout());
    }

    private void transferAnimal(final Player owner, final Tameable animal) {
        final UUID pID = owner.getUniqueId();
        final Player receiver = Bukkit.getServer().getPlayer(queue.get(pID));

        if (receiver == null) {
            owner.sendMessage(Message.PLAYER_ERROR.get());
            queue.remove(pID);
            return;
        }

        animal.setOwner(receiver);
        queue.remove(pID);

        Location petLoc = ((Entity)animal).getLocation();
        owner.sendMessage(Message.TRANSFER_NOTIFY_OWNER.get().replaceAll("\\{Player\\}", receiver.getName()));
        receiver.sendMessage(Message.TRANSFER_NOTIFY_RECEIVER.get()
                .replaceAll("\\{Player\\}", owner.getName()
                .replaceAll("\\{Location\\}",  petLoc.getBlockX() + ", " + petLoc.getBlockZ())));
    }

    @EventHandler
    private void onPlayerTransferAnimal(final PlayerInteractEntityEvent e) {
        final UUID pID = e.getPlayer().getUniqueId();
        final Entity en = e.getRightClicked();

        if(queue.containsKey(pID)) {
            if(Validate.isUntamedAnimal(en)) { return; }

            if(!Validate.isOwner(e.getPlayer(), (Tameable) en)) {
                queue.remove(pID);
                return;
            }

            transferAnimal(e.getPlayer(), (Tameable)en);
            e.setCancelled(true);
        }
    }
}
