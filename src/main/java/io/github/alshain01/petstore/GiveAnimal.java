package io.github.alshain01.petstore;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GiveAnimal implements Listener {
    // Holds a list of players who have used the give command
    private Set<UUID> queue = new HashSet<UUID>();

    // Holds a list of
    private Set<UUID> give = new HashSet<UUID>();
    private Map<UUID, UUID> claim = new HashMap<UUID, UUID>();

    /*
     * Constructors
     */
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

    /*
     * Public methods
     */
    public void add(final Player owner) {
        if(queue.contains(owner.getUniqueId())) {
            queue.remove(owner.getUniqueId());
        }

        queue.add(owner.getUniqueId());
        owner.sendMessage(Message.GIVE_INSTRUCTION.get());

        new BukkitRunnable() {
            public void run() {
                if(queue.contains(owner.getUniqueId())) {
                    queue.remove(owner.getUniqueId());
                    owner.sendMessage(Message.GIVE_TIMEOUT.get());
                }
            }
        }.runTaskLater(getPlugin(), PetStore.getTimeout());
    }

    public void cancel(Player player, Entity entity) {
        if(give.contains(entity.getUniqueId())) {
            give.remove(entity.getUniqueId());
            player.sendMessage(Message.GIVE_CANCEL.get());
        }
    }

    @EventHandler
    private void onPlayerGiveAnimal(PlayerInteractEntityEvent e) {
        if(!(e.getRightClicked() instanceof Tameable) || !((Tameable)e.getRightClicked()).isTamed()) { return; }
        final Tameable animal = (Tameable)e.getRightClicked();
        final Player player = e.getPlayer();

        if(queue.contains(player.getUniqueId())) {
            if(Validate.owner(player, animal)) {
                give.add(e.getRightClicked().getUniqueId());
                e.getPlayer().sendMessage(Message.GIVE_SET.get());
                e.setCancelled(true);
            }
            queue.remove(e.getPlayer().getUniqueId());
            return;
        }

        if(give.contains(e.getRightClicked().getUniqueId())) {
            if(claim.containsKey(player.getUniqueId())
                    && claim.get(player.getUniqueId()).equals(e.getRightClicked().getUniqueId())) {
                animal.setOwner(player);
                claim.remove(player.getUniqueId());
                give.remove(e.getRightClicked().getUniqueId());
                player.sendMessage(Message.CLAIM_NOTIFY.get());
                e.setCancelled(true);
                return;
            }

            player.sendMessage(Message.CLAIM_INSTRUCTION.get());
            claim.put(e.getPlayer().getUniqueId(), e.getRightClicked().getUniqueId());
            new BukkitRunnable() {
                public void run() {
                    if(claim.containsKey(player.getUniqueId())) {
                        claim.remove(player.getUniqueId());
                        player.sendMessage(Message.CLAIM_TIMEOUT.get());
                    }
                }
            }.runTaskLater(getPlugin(), PetStore.getTimeout());
        }
    }

    private Plugin getPlugin() {
        return Bukkit.getServer().getPluginManager().getPlugin("PetStore");
    }
}
