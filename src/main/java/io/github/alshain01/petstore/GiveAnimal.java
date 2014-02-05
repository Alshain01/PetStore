package io.github.alshain01.petstore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GiveAnimal implements Listener {
    // Holds a list of players who have used the give command
    private Set<UUID> queue = new HashSet<UUID>();

    // Holds a list of
    private Set<UUID> give = new HashSet<UUID>();
    private Map<UUID, UUID> claim = new HashMap<UUID, UUID>();

    public GiveAnimal(List<?> animals) {
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

    public void add(final Player owner) {
        if(queue.contains(owner.getUniqueId())) {
            queue.remove(owner.getUniqueId());
        }

        queue.add(owner.getUniqueId());
        owner.sendMessage(PetStore.warnColor + "Right click the tamed animal you wish to give away.");

        new BukkitRunnable() {
            public void run() {
                if(queue.contains(owner.getUniqueId())) {
                    queue.remove(owner.getUniqueId());
                    owner.sendMessage(PetStore.notifyColor + "Give animal timed out.");
                }
            }
        }.runTaskLater(Bukkit.getServer().getPluginManager().getPlugin("PetStore"), PetStore.timeout);
    }

    public void cancel(Player player, Entity entity) {
        if(give.contains(entity.getUniqueId())) {
            give.remove(entity.getUniqueId());
            player.sendMessage(PetStore.successColor + "The animal is no longer being given away.");
        }
    }

    @EventHandler
    private void onPlayerGiveAnimal(PlayerInteractEntityEvent e) {
        if(!(e.getRightClicked() instanceof Tameable) || !((Tameable)e.getRightClicked()).isTamed()) { return; }
        Tameable animal = (Tameable)e.getRightClicked();
        Player player = e.getPlayer();

        if(queue.contains(player.getUniqueId())) {
            if(Validate.owner(player, animal)) {
                give.add(e.getRightClicked().getUniqueId());
                e.getPlayer().sendMessage(PetStore.notifyColor + "This animal has been set to be given away.");
                e.setCancelled(true);
            }
            queue.remove(e.getPlayer().getUniqueId());
            return;
        }

        if(give.contains(e.getRightClicked().getUniqueId())) {
            if(claim.containsKey(e.getPlayer().getUniqueId())
                    && claim.get(e.getPlayer().getUniqueId()).equals(e.getRightClicked().getUniqueId())) {
                ((Tameable)e.getRightClicked()).setOwner(e.getPlayer());
                claim.remove(e.getPlayer().getUniqueId());
                give.remove(e.getRightClicked().getUniqueId());
                e.getPlayer().sendMessage(PetStore.successColor + "You have claimed this animal.");
                e.setCancelled(true);
                return;
            }

            e.getPlayer().sendMessage(PetStore.warnColor + "This animal is available.  Right click to claim it.");
            claim.put(e.getPlayer().getUniqueId(), e.getRightClicked().getUniqueId());
        }
    }
}
