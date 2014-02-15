package io.github.alshain01.petstore;

import io.github.alshain01.flags.Flag;
import io.github.alshain01.flags.area.Area;
import io.github.alshain01.flags.System;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

final class Animal {
    private Animal() { }

    static void tame(Player player, Tameable animal) {
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

    static void release(Player player, Object flag, Tameable animal) {
        if(!isOwner(player, animal) || isFlagSet(player, flag, ((Entity)animal).getLocation())) { return; }

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
        animal.setOwner(null);
    }

    static void transfer(Player owner, Tameable animal, Object flag, String receiver) {
        if(!Animal.isOwner(owner, animal)|| isFlagSet(owner, flag, ((Entity)animal).getLocation())) { return; }

        Player r = Bukkit.getPlayer(receiver);
        if (r == null) {
            owner.sendMessage(Message.PLAYER_ERROR.get());
            return;
        }

        animal.setOwner(r);

        Location loc = ((Entity)animal).getLocation();
        owner.sendMessage(Message.TRANSFER_NOTIFY_OWNER.get().replaceAll("\\{Player\\}", r.getName()));
        r.sendMessage(Message.TRANSFER_NOTIFY_RECEIVER.get().replaceAll("\\{Player\\}", owner.getName())
                        .replaceAll("\\{Location\\}", "X: " + loc.getBlockX() + ", Z: " +loc.getBlockZ()));
    }

    static void sell(PetStore plugin, Player player, Tameable animal, Object flag, Double price) {
        if(!isOwner(player, animal) || isFlagSet(player, flag, ((Entity)animal).getLocation())) { return; }
        UUID aID = ((Entity)animal).getUniqueId();

        if(price > 0D) {
            plugin.forSale.put(aID, price);
            plugin.forClaim.remove(((Entity)animal).getUniqueId());  // Only one at a time
            player.sendMessage(Message.SELL_SET.get().replaceAll("\\{Price\\}", plugin.economy.format(price)));
        } else {
            if(plugin.forSale.containsKey(aID)) {
                plugin.forSale.remove(aID);
                player.sendMessage(Message.SELL_CANCEL.get());
            }
        }
    }

    static boolean advertise(PetStore plugin, Player player, Double price) {
        if(price > plugin.economy.getBalance(player.getName())) {
            player.sendMessage(Message.BUY_LOW_FUNDS.get()
                    .replaceAll("\\{Price\\}", plugin.economy.format(price)));
            return false;
        }
        player.sendMessage(Message.BUY_INSTRUCTION.get()
                    .replaceAll("\\{Price\\}", plugin.economy.format(price)));
        return true;
    }

    static boolean buy(PetStore plugin, Player player, Tameable animal, Double price) {
        EconomyResponse r = plugin.economy.withdrawPlayer(player.getName(), price);
        if(r.transactionSuccess()) {
            r = plugin.economy.depositPlayer(animal.getOwner().getName(), price);
            if(!r.transactionSuccess()) {
                plugin.economy.depositPlayer(player.getName(), price); // Put it back.
                player.sendMessage(Message.TRANSACTION_ERROR.get());
                return false;
            }
        }

        player.sendMessage(Message.BUY_NOTIFY_RECEIVER.get());
        if(((Player)animal.getOwner()).isOnline()) {
            player.sendMessage(Message.BUY_NOTIFY_OWNER.get()
                    .replaceAll("\\{Player\\}", player.getName())
                    .replaceAll("\\{Price\\}", plugin.economy.format(price)));
        }
        animal.setOwner(player);
        return true;
    }

    static void give(PetStore plugin, Player player, Object flag, Tameable animal) {
        if(!isOwner(player, animal) || isFlagSet(player, flag, ((Entity)animal).getLocation())) { return; }
        plugin.forClaim.add(((Entity)animal).getUniqueId());
        if(plugin.forSale != null) {
            plugin.forSale.remove(((Entity)animal).getUniqueId());  // Only one at a time
        }
        player.sendMessage(Message.GIVE_SET.get());
    }

    static void claim(Player player, Tameable animal) {
        animal.setOwner(player);
        player.sendMessage(Message.CLAIM_NOTIFY.get());
    }

    static boolean isOwner(Player player, Tameable animal) {
        if(player.hasPermission("petstore.admin") || animal.getOwner().equals(player)) {
            return true;
        }
        player.sendMessage(Message.OWNER_ERROR.get());
        return false;
    }

    private static boolean isFlagSet(Player player, Object flag, Location location) {
        if(flag != null) {
            Flag f = (Flag)flag;
            Area area = System.getActive().getAreaAt(location);
            if(!area.getValue(f, false)
                    && (!player.hasPermission(f.getBypassPermission())
                    || !area.hasTrust(f, player))) {
                player.sendMessage(area.getMessage(f, player.getName()));
                return true;
            }
        }
        return false;
    }
}
