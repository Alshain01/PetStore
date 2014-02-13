package io.github.alshain01.petstore;

import io.github.alshain01.flags.*;
import io.github.alshain01.flags.area.Area;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

class GiveAnimal implements Listener {
    private final JavaPlugin plugin;

    // Holds a list of players who have used the give command
    private final Set<UUID> queue = new HashSet<UUID>();

    // Holds a list of
    private final Set<UUID> give;
    private final Map<UUID, UUID> claim = new HashMap<UUID, UUID>();
    private Object flag = null;


    /*
     * Constructors
     */
    public GiveAnimal(final JavaPlugin plugin, final List<?> animals) {
        this.plugin = plugin;
        if(Bukkit.getServer().getPluginManager().isPluginEnabled("Flags")) {
            this.flag = Flags.getRegistrar().getFlag("GivePet");
        }

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

    public int getCount() {
        return give.size();
    }

    /*
     * Public methods
     */
    public void add(final Player owner) {
        final UUID pID = owner.getUniqueId();
        if(!queue.contains(pID)) {
            queue.add(pID);
        }
        owner.sendMessage(Message.CLICK_INSTRUCTION.get().replaceAll("\\{Action\\}", Message.GIVE.get().toLowerCase()));

        new BukkitRunnable() {
            public void run() {
                if(queue.contains(pID)) {
                    queue.remove(pID);
                    owner.sendMessage(Message.TIMEOUT.get().replaceAll("\\{Action\\}", Message.GIVE.get()));
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerGiveAnimal(final PlayerInteractEntityEvent e) {
        if(Validate.isUntamedAnimal(e.getRightClicked())) { return; }

        final Tameable animal = (Tameable)e.getRightClicked();
        final Player player = e.getPlayer();
        // Player ID, Animal ID
        final UUID pID = player.getUniqueId(), aID = e.getRightClicked().getUniqueId();

        if(queue.contains(pID)) {
            if(Validate.isOwner(player, animal)) {

                // Check the flag
                if(flag != null) {
                    Flag giveFlag = (Flag)flag;
                    Area area = io.github.alshain01.flags.System.getActive().getAreaAt(((Entity)animal).getLocation());
                    if(!area.getValue(giveFlag, false)
                            && (!player.hasPermission(giveFlag.getBypassPermission())
                            || !area.hasTrust(giveFlag, player))) {
                        player.sendMessage(area.getMessage(giveFlag, player.getName()));
                        queue.remove(pID);
                        e.setCancelled(true);
                        return;
                    }
                }

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
                        player.sendMessage(Message.TIMEOUT.get().replaceAll("\\{Action\\}", Message.GIVE.get()));
                    }
                }
            }.runTaskLater(plugin, PetStore.getTimeout());
        }
    }
}
