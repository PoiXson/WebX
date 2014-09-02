package com.poixson.webx.bukkit;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsString;


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



	public static void SendMsg(final CommandSender entity, final String msg) {
		if(utils.isEmpty(msg)) return;
		// send msg to console
		if(entity == null || entity instanceof ConsoleCommandSender)
			entity.sendMessage(ConsoleColors(msg));
		else
		// send msg to a player
		if(entity instanceof Player)
			entity.sendMessage(ChatColors(msg));
		else
			entity.sendMessage(msg);
	}



	// chat colors
	public static String ChatColors(final String text) {
		if(utils.isEmpty(text))
			return text;
		if(!text.contains("{"))
			return text;
		String txt = text;
		txt = txt.replace("{reset}",       ChatColor.RESET.toString()         );
		txt = txt.replace("{bold}",        ChatColor.BOLD.toString()          );
		txt = txt.replace("{italic}",      ChatColor.ITALIC.toString()        );
		txt = txt.replace("{underline}",   ChatColor.UNDERLINE.toString()     );
		txt = txt.replace("{strike}",      ChatColor.STRIKETHROUGH.toString() );
		txt = txt.replace("{magic}",       ChatColor.MAGIC.toString()         );
		txt = txt.replace("{black}",       ChatColor.BLACK.toString()         );
		txt = txt.replace("{darkblue}",    ChatColor.DARK_BLUE.toString()     );
		txt = txt.replace("{darkgreen}",   ChatColor.DARK_GREEN.toString()    );
		txt = txt.replace("{darkaqua}",    ChatColor.DARK_AQUA.toString()     );
		txt = txt.replace("{darkred}",     ChatColor.DARK_RED.toString()      );
		txt = txt.replace("{darkpurple}",  ChatColor.DARK_PURPLE.toString()   );
		txt = txt.replace("{gold}",        ChatColor.GOLD.toString()          );
		txt = txt.replace("{gray}",        ChatColor.GRAY.toString()          );
		txt = txt.replace("{darkgray}",    ChatColor.DARK_GRAY.toString()     );
		txt = txt.replace("{blue}",        ChatColor.BLUE.toString()          );
		txt = txt.replace("{green}",       ChatColor.GREEN.toString()         );
		txt = txt.replace("{aqua}",        ChatColor.AQUA.toString()          );
		txt = txt.replace("{red}",         ChatColor.RED.toString()           );
		txt = txt.replace("{purple}",      ChatColor.LIGHT_PURPLE.toString()  );
		txt = txt.replace("{yellow}",      ChatColor.YELLOW.toString()        );
		txt = txt.replace("{white}",       ChatColor.WHITE.toString()         );
		return txt;
	}



	// console colors
	public static String ConsoleColors(final String text) {
		if(utils.isEmpty(text))
			return text;
		if(!text.contains("{") || !text.contains("}"))
			return text;
		String txt = utilsString.ensureEnds("{reset}", text);
		// https://searchcode.com/codesearch/view/37841218/
		txt = txt.replace("{reset}",       "\u001B[0m"              );
		txt = txt.replace("{bold}",        "\u001B[5m"              );
		txt = txt.replace("{italic}",      "\u001B[3m"              );
		txt = txt.replace("{underline}",   "\u001B[4m"              );
		txt = txt.replace("{strike}",      "\u001B[8m"              );
		txt = txt.replace("{magic}",       "\u001B[6m"              );
		txt = txt.replace("{black}",       "\u001B[2m"+"\u001B[30m" );
		txt = txt.replace("{darkblue}",    "\u001B[2m"+"\u001B[34m" );
		txt = txt.replace("{darkgreen}",   "\u001B[2m"+"\u001B[32m" );
		txt = txt.replace("{darkaqua}",    "\u001B[2m"+"\u001B[36m" );
		txt = txt.replace("{darkred}",     "\u001B[2m"+"\u001B[31m" );
		txt = txt.replace("{darkpurple}",  "\u001B[2m"+"\u001B[35m" );
		txt = txt.replace("{gold}",        "\u001B[2m"+"\u001B[33m" );
		txt = txt.replace("{gray}",        "\u001B[1m"+"\u001B[30m" );
		txt = txt.replace("{darkgray}",    "\u001B[2m"+"\u001B[37m" );
		txt = txt.replace("{blue}",        "\u001B[1m"+"\u001B[34m" );
		txt = txt.replace("{green}",       "\u001B[1m"+"\u001B[32m" );
		txt = txt.replace("{aqua}",        "\u001B[1m"+"\u001B[36m" );
		txt = txt.replace("{red}",         "\u001B[1m"+"\u001B[31m" );
		txt = txt.replace("{purple}",      "\u001B[1m"+"\u001B[35m" );
		txt = txt.replace("{yellow}",      "\u001B[1m"+"\u001B[33m" );
		txt = txt.replace("{white}",       "\u001B[1m"+"\u001B[37m" );
		txt = txt.replace("{bgblack}",     "\u001B[40m" );
		txt = txt.replace("{bgblue}",      "\u001B[44m" );
		txt = txt.replace("{bggreen}",     "\u001B[42m" );
		txt = txt.replace("{bgaqua}",      "\u001B[46m" );
		txt = txt.replace("{bgred}",       "\u001B[41m" );
		txt = txt.replace("{bgpurple}",    "\u001B[45m" );
		txt = txt.replace("{bgyellow}",    "\u001B[43m" );
		txt = txt.replace("{bgwhite}",     "\u001B[47m" );
		return txt;
	}



}
