/*
 * Copyright (c) 2/16/14 1:43 PM Kevin Seiden. All rights reserved.
 *
 *  This works is licensed under the Creative Commons Attribution-NonCommercial 3.0
 *
 *  You are Free to:
 *     to Share: to copy, distribute and transmit the work
 *     to Remix: to adapt the work
 *
 *  Under the following conditions:
 *     Attribution: You must attribute the work in the manner specified by the author (but not in any way that suggests that they endorse you or your use of the work).
 *     Non-commercial: You may not use this work for commercial purposes.
 *
 *  With the understanding that:
 *     Waiver: Any of the above conditions can be waived if you get permission from the copyright holder.
 *     Public Domain: Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
 *     Other Rights: In no way are any of the following rights affected by the license:
 *         Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
 *         The author's moral rights;
 *         Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.
 *
 *  Notice: For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.
 *  http://creativecommons.org/licenses/by-nc/3.0/
 */

package io.github.alshain01.petstore;

import io.github.alshain01.flags.api.Flag;
import io.github.alshain01.flags.api.FlagsAPI;
import io.github.alshain01.flags.api.area.Area;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

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
        player.sendMessage(Message.ACTION_NOTIFY.get().replace("{Action}", "tamed"));
    }

    static boolean release(Player player, Object flag, Tameable animal) {
        if(!isOwner(player, animal) || isFlagSet(player, flag, ((Entity)animal).getLocation())) { return false; }

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
        player.sendMessage(Message.ACTION_NOTIFY.get().replace("{Action}", "released"));
        return true;
    }

    static boolean transfer(Player owner, Tameable animal, Object flag, Player receiver) {
        if(!Animal.isOwner(owner, animal)|| isFlagSet(owner, flag, ((Entity)animal).getLocation())) { return false; }

        if (receiver == null) {
            owner.sendMessage(Message.PLAYER_ERROR.get());
            return false;
        }

        Location loc = ((Entity)animal).getLocation();
        ((Player)animal.getOwner()).sendMessage(Message.TRANSFER_NOTIFY_OWNER.get().replace("{Player}", receiver.getName()));
        animal.setOwner(receiver);
        receiver.sendMessage(Message.TRANSFER_NOTIFY_RECEIVER.get().replace("{Player}", owner.getName())
                        .replace("{Location}", "X: " + loc.getBlockX() + ", Z: " +loc.getBlockZ()));
        return true;
    }

    static Boolean sell(Economy economy, Player player, Tameable animal, Object flag, Double price) {
        if(!isOwner(player, animal) || isFlagSet(player, flag, ((Entity)animal).getLocation())) { return null; }

        if(price > 0D) {
            player.sendMessage(Message.SELL_SET.get().replace("{Price}", economy.format(price)));
            return true;
        }

        player.sendMessage(Message.SELL_CANCEL.get());
        return false;
    }

    static boolean advertise(PetStore plugin, Player player, Tameable animal, Double price) {
        if(animal.getOwner().equals(player)) { return false; }
        if(price > plugin.economy.getBalance(player.getName())) {
            player.sendMessage(Message.BUY_LOW_FUNDS.get()
                    .replace("{Price}", plugin.economy.format(price)));
            return false;
        }
        player.sendMessage(Message.BUY_INSTRUCTION.get()
                    .replace("{Price}", plugin.economy.format(price)));
        return true;
    }

    static boolean buy(Economy economy, Player player, Tameable animal, Double price) {
        EconomyResponse r = economy.withdrawPlayer(player.getName(), price);
        if(r.transactionSuccess()) {
            r = economy.depositPlayer(animal.getOwner().getName(), price);
            if(!r.transactionSuccess()) {
                economy.depositPlayer(player.getName(), price); // Put it back.
                player.sendMessage(Message.TRANSACTION_ERROR.get());
                return false;
            }
        }

        player.sendMessage(Message.BUY_NOTIFY_RECEIVER.get());
        if(((Player)animal.getOwner()).isOnline()) {
            ((Player)animal.getOwner()).sendMessage(Message.BUY_NOTIFY_OWNER.get()
                    .replace("{Player}", player.getName())
                    .replace("{Price}", economy.format(price)));
        }
        animal.setOwner(player);
        return true;
    }

    static boolean give(Player player, Object flag, Tameable animal) {
        if(!isOwner(player, animal) || isFlagSet(player, flag, ((Entity)animal).getLocation())) { return false; }
        player.sendMessage(Message.GIVE_SET.get());
        return true;
    }

    static boolean requestClaim(Player player, Tameable animal) {
        if(animal.getOwner().equals(player)) { return false; }
        player.sendMessage(Message.CLAIM_INSTRUCTION.get());
        return true;
    }

    static void claim(Player player, Tameable animal) {
        animal.setOwner(player);
        player.sendMessage(Message.ACTION_NOTIFY.get().replace("{Action}", "claimed"));
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
            Area area = FlagsAPI.getAreaAt(location);
            if(!area.getState(f)
                    && (!player.hasPermission(f.getBypassPermission())
                    || !area.hasTrust(f, player))) {
                player.sendMessage(area.getMessage(f, player.getName()));
                return true;
            }
        }
        return false;
    }
}
