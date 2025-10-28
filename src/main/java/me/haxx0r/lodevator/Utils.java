package me.haxx0r.lodevator;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class Utils {
    public static boolean isSolid(Location loc) {
        Block block = loc.getBlock();
        if (block.getType().isSolid())
            return true;

        Block above = block.getRelative(BlockFace.UP);
        return above.getType().isSolid();
    }

    public static Block getBelow(Player player) {
        return player.getLocation().getBlock().getRelative(BlockFace.DOWN);
    }
}
