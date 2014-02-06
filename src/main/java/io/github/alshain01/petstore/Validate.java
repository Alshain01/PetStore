package io.github.alshain01.petstore;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.permissions.Permissible;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Validate {
    public static boolean owner(Player player, Tameable animal) {
        if(animal.isTamed() && (player.hasPermission("petstore.admin") || animal.getOwner().equals(player))) {
            return true;
        }
        player.sendMessage(PetStore.warnColor + "You do not own that animal.");
        return false;
    }
}
