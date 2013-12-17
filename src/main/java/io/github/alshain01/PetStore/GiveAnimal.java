package io.github.alshain01.PetStore;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
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
    private Set<UUID> queue = new HashSet<UUID>();
    private Set<UUID> give = new HashSet<UUID>();
    private Map<UUID, UUID> claim = new ConcurrentHashMap<UUID, UUID>();

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

    public void add(PetStore ps, final Player owner) {
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
        }.runTaskLater(ps, PetStore.timeout);
    }

    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if(!(e.getRightClicked() instanceof Tameable) || !((Tameable)e.getRightClicked()).isTamed()) { return; }

        if(queue.contains(e.getPlayer().getUniqueId())) {
            if(!Validate.owner(e.getPlayer(), (Tameable)e.getRightClicked())) {
                queue.remove(e.getPlayer().getUniqueId());
                return;
            }

            give.add(((Entity) e.getRightClicked()).getUniqueId());
            e.setCancelled(true);
        }

        if(give.contains(((Entity) e.getRightClicked()).getUniqueId())) {
            if(((Tameable)e.getRightClicked()).getOwner().equals(e.getPlayer())) {
                e.getPlayer().sendMessage(PetStore.notifyColor + "This animal has been set to be given away.");
                return;
            }

            if(claim.containsKey(e.getPlayer().getUniqueId())
                    && claim.get(e.getPlayer().getUniqueId()).equals(((Entity) e.getRightClicked()).getUniqueId())) {
                ((Tameable)e.getRightClicked()).setOwner(e.getPlayer());
                claim.remove(e.getPlayer().getUniqueId());
                e.getPlayer().sendMessage(PetStore.successColor + "You have claimed this animal.");
                e.setCancelled(true);
                return;
            }

            e.getPlayer().sendMessage(PetStore.warnColor + "This animal is available.  Right click to claim it.");
            claim.put(e.getPlayer().getUniqueId(), ((Entity)e.getRightClicked()).getUniqueId());
        }
    }
}
