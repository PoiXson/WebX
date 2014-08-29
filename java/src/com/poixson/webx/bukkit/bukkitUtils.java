package com.poixson.webx.bukkit;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.poixson.commonjava.Utils.utils;


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
	public static void BroadcastRadius(final Location location, final int radius, final String msg) {
		if(utils.isEmpty(msg)) return;
		final Set<Player> players = getPlayersNearby(location, radius);
		if(utils.isEmpty(players)) return;
		for(final Player player : players)
			player.sendMessage(msg);
	}



	public static Set<Player> getPlayersNearby(final Location location, final int radius) {
		if(location == null) throw new NullPointerException();
		if(radius < 1) throw new IllegalArgumentException();
		final String w = location.getWorld().getName();
		final double x = location.getX();
		final double y = location.getY();
		final double z = location.getZ();
		final double r = Integer.valueOf(radius).doubleValue();
		final Set<Player> players = new HashSet<Player>();
		final Player[] online = Bukkit.getOnlinePlayers();
		if(online.length == 0) return players;
		for(final Player p : online) {
			final Location loc = p.getLocation();
			if(loc == null) continue;
			final String playerW = loc.getWorld().getName();
			if(!w.equalsIgnoreCase(playerW)) continue;
			final double playerX = loc.getX();
			if(playerX < x - r || playerX > x + r) continue;
			final double playerY = loc.getY();
			if(playerY < y - (r * 1.5) || playerY > y + (r * 1.5)) continue;
			final double playerZ = loc.getZ();
			if(playerZ < z - r || playerZ > z + r) continue;
			players.add(p);
		}
		return players;
	}



}
