package io.github.alshain01.petstore;

import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

public class Validate {
    public static boolean owner(Player player, Tameable animal) {
        if(animal.isTamed() && (player.hasPermission("petstore.admin") || animal.getOwner().equals(player))) {
            return true;
        }
        player.sendMessage(Message.OWNER_ERROR.get());
        return false;
    }
}