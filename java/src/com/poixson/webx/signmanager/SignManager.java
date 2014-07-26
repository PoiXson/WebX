package com.poixson.webx.signmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import me.lorenzop.webauctionplus.WebAuctionPlus;

import org.bukkit.Material;
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
import com.poixson.commonjava.Utils.utilsString;
import com.poixson.commonjava.Utils.xTime;
import com.poixson.commonjava.pxdb.dbPool;


public class SignManager implements Listener {

	protected volatile dbPool db = null;

	// first line
	protected final String masterLine;
	protected final Set<String> masterAliases = new CopyOnWriteArraySet<String>();

	// sign types
	protected final Set<SignType> signTypes = new CopyOnWriteArraySet<SignType>();

	// anti-click-spam
	private final Map<String, CoolDown> debounce = new HashMap<String, CoolDown>();
	private final xTime debounceTime = xTime.get(".5s");



	// new sign manager
	public SignManager(final String masterLine) {
		this.masterLine = masterLine;
	}
	public SignManager(final String masterLine, final String[] masterAliases) {
		this(masterLine);
		addMasterAliases(masterAliases);
	}



	// database
	public void db(final dbPool db) {
		this.db = db;
	}
	public dbPool db() {
		return this.db;
	}



	// first line
	public void addMasterAlias(final String line) {
		if(line == null || line.isEmpty()) return;
		this.masterAliases.add(line);
	}
	public void addMasterAliases(final String[] lines) {
		if(lines == null) return;
		for(final String line : lines)
			this.addMasterAlias(line);
	}



	/**
	 * Register a sign type.
	 * @param sign
	 */
	public void register(final SignType sign) {
		if(sign == null) throw new NullPointerException();
		this.signTypes.add(sign);
	}



	/**
	 * Search cached sign locations.
	 * @param block
	 * @return
	 */
	private SignType QuerySign(final Block block) {
		if(block == null) return null;
		final String world = block.getWorld().getName();
		if(world == null || world.isEmpty()) return null;
		final long x = block.getX();
		final long y = block.getY();
		final long z = block.getZ();
		for(final SignType pluginsign : this.signTypes) {
			if(pluginsign.getSignLocation(world, x, y, z) != null)
				return pluginsign;
		}
		return null;
	}



	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		final Block block = event.getBlock();
		final String[] lines = event.getLines();
		final Player player = event.getPlayer();
		if(block == null || lines == null || player == null) return;
		// block is a sign
		if(!isSign(block)) return;
		final Sign sign = (Sign) block.getState();
		{
			// remove if already registered
			final SignType pluginsign = this.QuerySign(block);
			if(pluginsign != null)
				pluginsign.queryRemoveSign(sign);
		}
		// match first line
		final String line0 = utilsString.trims(lines[0], "[", "]");
		boolean found = false;
		if(!this.masterLine.equalsIgnoreCase(line0))
			found = true;
		else
		if(!found) {
			for(final String alias : this.masterAliases) {
				if(alias.equalsIgnoreCase(line0)) {
					found = true;
					break;
				}
			}
		}
		// not a plugin sign
		if(!found) return;
		boolean changed = false;
		// set first line
		if(!lines[0].equals("["+this.masterLine+"]")) {
			lines[0] = "["+this.masterLine+"]";
			changed = true;
		}
		// find sign type
		SignType pluginsign = null;
		for(final SignType type : this.signTypes) {
			if(type.ValidateSign(lines)) {
				pluginsign = type;
				break;
			}
		}
		// invalid sign
		if(pluginsign == null) {
			event.setCancelled(true);
			return;
		}
		// create sign permission
		if(!pluginsign.canCreateSign(player)) {
			player.sendMessage(WebAuctionPlus.chatPrefix + WebAuctionPlus.Lang.getString("no_permission"));
			event.setCancelled(true);
			return;
		}
		// validate and format sign
		if(pluginsign.FullValidateSign(lines))
			changed = true;
		// update sign lines
		if(changed)
			sign.update();
		// register new sign
		pluginsign.queryAddSign(sign);
//TODO: created new sign msg
//		p.sendMessage(WebAuctionPlus.chatPrefix + WebAuctionPlus.Lang.getString("created_shout_sign"));
	}



	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Block  block  = event.getClickedBlock();
		final Player player = event.getPlayer();
		final Action action = event.getAction();
		if(block == null || player == null || action == null) return;
		// block is a sign
		if(!isSign(block)) return;
		final Sign sign = (Sign) block.getState();
		// left/right clicked
		final boolean leftClicked =
				(action == Action.LEFT_CLICK_BLOCK) ||
				(action == Action.LEFT_CLICK_AIR);
		final boolean rightClicked =
			(action == Action.RIGHT_CLICK_BLOCK) ||
			(action == Action.RIGHT_CLICK_AIR);
		if(!leftClicked && !rightClicked)
			return;
		// is registered sign
		SignType pluginsign = this.QuerySign(block);
		// not a plugin sign
		if(pluginsign == null)
			return;
		// has remove permission
		if(leftClicked)
			if(player.hasPermission(this.getSignRemovePermNode()))
				return;
		// prevent click spamming
		if(!this.debounce(player)) {
			event.setCancelled(true);
			player.sendMessage(WebAuctionPlus.chatPrefix + this.getClickSpamMsg());
			return;
		}
		event.setCancelled(true);
		// check first line
		boolean hasChanged = false;
		final String[] lines = sign.getLines();
		if(!lines[0].equals(this.masterLine)) {
			lines[0] = this.masterLine;
			hasChanged = true;
		}
		// validate sign
		if(pluginsign.FullValidateSign(lines))
			hasChanged = true;
		// sign has changed
		if(hasChanged)
			sign.update();
		// has permission to use
		if(!pluginsign.canUseSign(player)) {
			player.sendMessage(WebAuctionPlus.chatPrefix + WebAuctionPlus.Lang.getString("no_permission"));
			return;
		}
		// player clicked event
		pluginsign.SignClicked(sign, player);
	}



	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		final Block block = event.getBlock();
		final Player player = event.getPlayer();
		// block is a sign
		if(!isSign(block)) return;
		final Sign sign = (Sign) block.getState();
		// is registered sign
		SignType pluginsign = this.QuerySign(block);
		// not a plugin sign
		if(pluginsign == null)
			return;
		// permission to remove sign
		if(player.hasPermission(this.getSignRemovePermNode())) {
			pluginsign.queryRemoveSign(sign);
			player.sendMessage(WebAuctionPlus.chatPrefix + this.getSignRemovedMsg());
		} else {
			event.setCancelled(true);
			player.sendMessage(WebAuctionPlus.chatPrefix + this.getPermissionDeniedMsg());
		}
	}



	/**
	 * Player quit; reset debounce time
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {
		final String uuid = event.getPlayer().getUniqueId().toString();
		this.debounce.remove(uuid);
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



	public static boolean isSign(final Block block) {
		if(block == null) return false;
		final Material mat = block.getType();
		return (
			mat == Material.SIGN      ||
			mat == Material.SIGN_POST ||
			mat == Material.WALL_SIGN
		);
	}



	protected String getClickSpamMsg() {
		return "Please wait a bit before using that again.";
	}
	protected String getPermissionDeniedMsg() {
		return "You don't have permission to do that.";
	}
	protected String getSignRemovedMsg() {
		return "WebAuctionPlus sign removed.";
	}



	protected String getSignRemovePermNode() {
		return "wa.sign.remove";
	}



}
