package io.github.alshain01.petstore;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

class Validate {
    public static boolean isOwner(Player player, Tameable animal) {
        if((player.hasPermission("petstore.admin") || animal.getOwner().equals(player))) {
            return true;
        }
        player.sendMessage(Message.OWNER_ERROR.get());
        return false;
    }

    public static boolean isUntamedAnimal(Entity e) {
        return (!(e instanceof Tameable) || !((Tameable)e).isTamed());
    }
}