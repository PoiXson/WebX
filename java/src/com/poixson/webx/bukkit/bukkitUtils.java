package com.poixson.webx.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;


public final class bukkitUtils {
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	private bukkitUtils() {}



	public static boolean isSign(final Block block) {
		if(block == null) return false;
		final Material mat = block.getType();
		if( mat == Material.SIGN      ||
			mat == Material.SIGN_POST ||
			mat == Material.WALL_SIGN)
				return true;
		return false;
	}



	// announce radius
	public static void BroadcastRadius(final Location loc, final int radius, final String msg) {
		if(loc == null || radius < 1) return;
		if(msg == null || msg.isEmpty()) return;
		final Player[] list = Bukkit.getOnlinePlayers();
		if(list.length == 0) return;
		final double rad = Integer.valueOf(radius).doubleValue();
		final double x = loc.getX();
		final double z = loc.getZ();
		for(final Player player : list) {
			final double playerX = player.getLocation().getX();
			final double playerZ = player.getLocation().getZ();
			if( (playerX < x + rad ) &&
				(playerX > x - rad ) &&
				(playerZ < z + rad ) &&
				(playerZ > z - rad ) )
					player.sendMessage(msg);
		}
	}



}
