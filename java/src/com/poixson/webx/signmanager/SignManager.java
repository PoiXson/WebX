package com.poixson.webx.signmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.poixson.commonjava.Utils.CoolDown;
import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsString;
import com.poixson.commonjava.Utils.xTime;
import com.poixson.commonjava.Utils.byRef.boolRef;
import com.poixson.commonjava.pxdb.dbPool;
import com.poixson.commonjava.xLogger.xLog;
import com.poixson.webx.bukkit.bukkitUtils;


public class SignManager implements Listener {

	protected volatile dbPool db = null;

	// first line
	protected final String masterLine;
	protected final String[] masterAliases;

	// sign types
	protected final Set<SignType> signTypes = new CopyOnWriteArraySet<SignType>();

	// anti-click-spam
	private final Map<String, CoolDown> debounce = new HashMap<String, CoolDown>();
	private final xTime debounceTime = xTime.get(".5s");



	// new sign manager
	public SignManager(final String masterLine, final String[] masterAliases) {
		if(utils.isEmpty(masterLine)) throw new NullPointerException();
		this.masterLine = masterLine;
		this.masterAliases = (utils.isEmpty(masterAliases)) ? null : masterAliases.clone();
	}
	public SignManager(final String masterLine) {
		this(masterLine, null);
	}
	/**
	 * Register a sign type.
	 * @param sign
	 */
	public void register(final SignType sign) {
		if(sign == null) throw new NullPointerException();
		this.signTypes.add(sign);
	}



	public SignType hasSignLocation(final Sign sign) {
		if(sign == null) throw new NullPointerException();
		final Location loc = sign.getLocation();
		final String world = loc.getWorld().getName();
		final long x = loc.getBlockX();
		final long y = loc.getBlockY();
		final long z = loc.getBlockZ();
		for(final SignType type : this.signTypes) {
			if(type.hasLocation(world, x, y, z))
				return type;
		}
		return null;
	}



	// create sign event
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onSignChange(final SignChangeEvent event) {
		final String[] lines = event.getLines();
		final boolRef changed = new boolRef(false);
		// match first line
		if(!matchFirstLine(lines[0]))
			return;
		final Player player = event.getPlayer();
		if(player == null) return;
		// new sign
		final SignType type = this.CreateSign(player, changed, lines);
		if(type == null) {
			event.setCancelled(true);
			return;
		}
		final Sign sign = (Sign) event.getBlock().getState();
		// update sign
		if(changed.value) {
			sign.update();
			log().fine("Updated new sign: "+lines[1]);
		}
		// success
		type.addLocation(sign.getLocation());
	}
	// sign click event
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEvent event) {
		final Sign sign;
		{
			final Block block = event.getClickedBlock();
			if(!bukkitUtils.isSign(block)) return;
			sign = (Sign) block.getState();
			if(sign == null) return;
		}
		String[] lines = sign.getLines();
		// match first line
		if(!matchFirstLine(lines[0])) return;
		// player
		final Player player = event.getPlayer();
		if(player == null) return;
		// prevent click spamming
		if(this.debounce(player)) {
			event.setCancelled(true);
			return;
		}
		// left/right clicked
		final Action action = event.getAction();
		final boolean leftClicked =
				(action == Action.LEFT_CLICK_BLOCK) ||
				(action == Action.LEFT_CLICK_AIR);
		final boolean rightClicked =
				(action == Action.RIGHT_CLICK_BLOCK) ||
				(action == Action.RIGHT_CLICK_AIR);
		if(!leftClicked && !rightClicked) {
			event.setCancelled(true);
			return;
		}
		// is plugin sign
		SignType type = this.hasSignLocation(sign);
		// create from existing sign
		if(type == null) {
			final boolRef changed = new boolRef(false);
			// new sign
			type = this.CreateSign(player, changed, lines);
			if(type == null) {
				event.setCancelled(true);
				return;
			}
			// update sign
			if(changed.value) {
				sign.update();
				log().fine("Updated new sign: "+lines[1]);
			}
			// success
			type.addLocation(sign.getLocation());
		}
		// allow break sign
		if(leftClicked && type.canRemoveSign(player)) {
			//event.setCancelled(false);
			return;
		}
		event.setCancelled(true);
		// check permissions
		if(!type.canUseSign(player)) {
			player.sendMessage(type.msgNoPermission());
			return;
		}
		// click event
		type.SignClicked(event, lines);
	}



	// sign break event
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockBreak(final BlockBreakEvent event) {
		final Sign sign;
		{
			final Block block = event.getBlock();
			if(!bukkitUtils.isSign(block)) return;
			sign = (Sign) block.getState();
			if(sign == null) return;
		}
		// match first line
		final String[] lines = sign.getLines();
		if(!matchFirstLine(lines[0]))
			return;
		final Player player = event.getPlayer();
		if(player == null) return;
		final Location loc = sign.getLocation();
		// find sign type to remove
		for(final SignType type : this.signTypes) {
			if(!type.hasLocation(loc)) continue;
			// check permissions
			if(!type.canRemoveSign(player)) {
				event.setCancelled(true);
				player.sendMessage(type.msgNoPermission());
				return;
			}
			if(!type.removeLocation(loc)) {
				event.setCancelled(true);
				player.sendMessage("Failed to remove sign from db!");
				log().severe("Failed to remove sign from db!");
				return;
			}
			player.sendMessage(type.msgSignRemoved());
			log().info(player.getName()+" removed a "+lines[1]+" sign");
		}
	}



	/**
	 * Player quit; reset debounce time
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		final String uuid = event.getPlayer().getUniqueId().toString();
		this.debounce.remove(uuid);
	}



	/**
	 * Called by an event which may possibly create a new sign.
	 * @param player Player affected by the event.
	 * @param sign Newly created or clicked sign.
	 * @return SignType if successful; null if failed
	 */
	public SignType CreateSign(final Player player, final boolRef changed, String[] lines) {
		if(player == null) throw new NullPointerException();
		// match first line
		if(!matchFirstLine(lines[0]))
			return null;
		// set first line
		if(!lines[0].equals("["+this.masterLine+"]")) {
			lines[0] = "["+this.masterLine+"]";
			changed.value = true;
		}
		// find sign type
		for(final SignType type : this.signTypes) {
			final Boolean result = type.ValidateSign(player, changed, lines);
			// invalid sign
			if(result == null)
				return null;
			// success
			if(result.booleanValue()) {
				player.sendMessage(type.msgSignCreated());
				log().info(player.getName()+" created a new "+lines[1]+" sign.");
				return type;
			}
		}
		// unknown sign type
		player.sendMessage(this.msgInvalidSign());
		log().info(player.getName()+" created an invalid sign");
		return null;
	}



	// database
	public void db(final dbPool db) {
		this.db = db;
	}
	public dbPool db() {
		return this.db;
	}



	public boolean matchFirstLine(final String line) {
		final String line0 = utilsString.trims(line, "[", "]");
		if(utils.isEmpty(line0)) return false;
		if(line0.equalsIgnoreCase(this.masterLine))
			return true;
		for(final String alias : this.masterAliases)
			if(alias.equalsIgnoreCase(line0))
				return true;
		return false;
	}



	/**
	 * Prevent click spamming.
	 * @param player
	 * @return true if click spam; false if ok
	 */
	protected boolean debounce(final Player player) {
		final String uuid = player.getUniqueId().toString();
		final CoolDown cool = this.debounce.get(uuid);
		if(cool == null) {
			this.debounce.put(uuid, CoolDown.get(this.debounceTime));
			return false;
		}
		if(cool.runAgain())
			return false;
		player.sendMessage(this.msgClickSpam());
		return true;
	}



	// default language messages
	protected String msgClickSpam() {
		return "Please wait.";
	}
	protected String msgInvalidSign() {
		return "Invalid sign.";
	}



	// logger
	public static xLog log() {
		return xLog.getRoot();
	}



}
