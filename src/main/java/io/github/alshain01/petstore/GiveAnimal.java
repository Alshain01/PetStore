package io.github.alshain01.petstore;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GiveAnimal implements Listener {
    private final JavaPlugin plugin;

    // Holds a list of players who have used the give command
    private final Set<UUID> queue = new HashSet<UUID>();

    // Holds a list of
    private final Set<UUID> give;
    private final Map<UUID, UUID> claim = new HashMap<UUID, UUID>();

    /*
     * Constructors
     */
    public GiveAnimal(final JavaPlugin plugin, final List<?> animals) {
        this.plugin = plugin;

        Set<UUID> uSet = new HashSet<UUID>();
        for(Object o : animals) {
            uSet.add(UUID.fromString((String)o));
        }
        this.give = uSet;
    }

    public List<String> get() {
        List<String> sList = new ArrayList<String>();
        for(UUID u : give) {
            sList.add(u.toString());
        }
        return sList;
    }

    /*
     * Public methods
     */
    public void add(final Player owner) {
        final UUID pID = owner.getUniqueId();
        if(!queue.contains(pID)) {
            queue.add(pID);
        }
        owner.sendMessage(Message.GIVE_INSTRUCTION.get());

        new BukkitRunnable() {
            public void run() {
                if(queue.contains(pID)) {
                    queue.remove(pID);
                    owner.sendMessage(Message.GIVE_TIMEOUT.get());
                }
            }
        }.runTaskLater(plugin, PetStore.getTimeout());
    }

    public void cancel(Player player, Entity entity) {
        if(give.contains(entity.getUniqueId())) {
            give.remove(entity.getUniqueId());
            player.sendMessage(Message.GIVE_CANCEL.get());
        }
    }

    @EventHandler
    private void onPlayerGiveAnimal(final PlayerInteractEntityEvent e) {
        if(Validate.isUntamedAnimal(e.getRightClicked())) { return; }

        final Tameable animal = (Tameable)e.getRightClicked();
        final Player player = e.getPlayer();
        // Player ID, Animal ID
        final UUID pID = player.getUniqueId(), aID = e.getRightClicked().getUniqueId();

        if(queue.contains(pID)) {
            if(Validate.isOwner(player, animal)) {
                give.add(aID);
                player.sendMessage(Message.GIVE_SET.get());
                e.setCancelled(true);
            }
            queue.remove(pID);
            return;
        }

        if(give.contains(aID)) {
            if(claim.containsKey(pID) && claim.get(pID).equals(aID)) {
                animal.setOwner(player);
                claim.remove(pID);
                give.remove(aID);
                player.sendMessage(Message.CLAIM_NOTIFY.get());
                e.setCancelled(true);
                return;
            }

            player.sendMessage(Message.CLAIM_INSTRUCTION.get());
            claim.put(pID, aID);
            new BukkitRunnable() {
                public void run() {
                    if(claim.containsKey(pID)) {
                        claim.remove(pID);
                        player.sendMessage(Message.CLAIM_TIMEOUT.get());
                    }
                }
            }.runTaskLater(plugin, PetStore.getTimeout());
        }
    }
}
