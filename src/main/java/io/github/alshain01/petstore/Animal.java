package io.github.alshain01.petstore;

import io.github.alshain01.flags.Flag;
import io.github.alshain01.flags.area.Area;
import io.github.alshain01.flags.System;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Animal {
    private Animal() { }

    public static void tame(Player player, Tameable animal) {
        if(animal instanceof Horse) {
            ((Horse)animal).setDomestication(((Horse)animal).getMaxDomestication());
        }

        animal.setOwner(player);

        if(animal instanceof Ocelot) {
            int rand = (int)(Math.random()*2);
            Ocelot.Type type;
            switch (rand) {
                case 0:
                    type = Ocelot.Type.BLACK_CAT;
                    break;
                case 1:
                    type = Ocelot.Type.SIAMESE_CAT;
                    break;
                default:
                    type = Ocelot.Type.RED_CAT;
            }
            ((Ocelot)animal).setCatType(type);
            ((Ocelot)animal).setSitting(true);
        }

        if(animal instanceof Wolf) {
            ((Wolf)animal).setSitting(true);
        }
    }

    public static boolean release(Player player, Object flag, Tameable animal) {
        // Check the flag
        if(flag != null) {
            Flag f = (Flag)flag;
            Area area = System.getActive().getAreaAt(((Entity)animal).getLocation());
            if(!area.getValue(f, false)
                    && (!player.hasPermission(f.getBypassPermission())
                    || !area.hasTrust(f, player))) {
                player.sendMessage(area.getMessage(f, player.getName()));
                return false;
            }
        }

        if(isOwner(player, animal)) {
            if(animal instanceof Ocelot) {
                ((Ocelot)animal).setCatType(Ocelot.Type.WILD_OCELOT);
                ((Ocelot)animal).setSitting(false);
            }

            if(animal instanceof Wolf) {
                ((Wolf)animal).setSitting(false);
            }

            if(animal instanceof Horse) {
                ((Horse)animal).setCarryingChest(false);
                ((Horse)animal).getInventory().setArmor(new ItemStack(Material.AIR));
                ((Horse)animal).getInventory().setSaddle(new ItemStack(Material.AIR));
                ((Horse)animal).setDomestication(0);
            }
            ((LivingEntity)animal).setLeashHolder(null);
            (animal).setOwner(null);
            return true;
        }
        return false;
    }

    public static boolean transfer(Player owner, Tameable animal, String receiver) {
        Player r = Bukkit.getPlayer(receiver);

        if(!Animal.isOwner(owner, animal)) { return false; }

        if (r == null) {
            owner.sendMessage(Message.PLAYER_ERROR.get());
            return false;
        }

        animal.setOwner(r);

        Location loc = ((Entity)animal).getLocation();
        owner.sendMessage(Message.TRANSFER_NOTIFY_OWNER.get().replaceAll("\\{Player\\}", r.getName()));
        r.sendMessage(Message.TRANSFER_NOTIFY_RECEIVER.get()
                .replaceAll("\\{Player\\}", owner.getName()
                        .replaceAll("\\{Location\\}", loc.getBlockX() + ", " + loc.getBlockZ())));
        return true;
    }

    public static boolean isOwner(Player player, Tameable animal) {
        if((player.hasPermission("petstore.admin") || animal.getOwner().equals(player))) {
            return true;
        }
        player.sendMessage(Message.OWNER_ERROR.get());
        return false;
    }

    /**
     * Gets if the animal can be tamed
     *
     * @return False if the animal is not tameable or is already tamed.
     */
    public static boolean isUntamed(Entity e) {
        return (!(e instanceof Tameable) || !((Tameable)e).isTamed());
    }
}
