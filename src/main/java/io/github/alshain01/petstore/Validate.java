package io.github.alshain01.petstore;

import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

public class Validate {
    public static boolean owner(Player player, Tameable animal) {
        if(!animal.getOwner().equals(player)) {
            player.sendMessage(PetStore.warnColor + "You do not own that animal.");
            return false;
        }
        return true;
    }
}
