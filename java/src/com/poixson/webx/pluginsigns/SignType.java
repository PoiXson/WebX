package com.poixson.webx.pluginsigns;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsString;
import com.poixson.commonjava.Utils.byRef.boolRef;
import com.poixson.commonjava.pxdb.dbQuery;
import com.poixson.commonjava.xLogger.xLog;


public abstract class SignType {

	protected final Set<Location> locations = new CopyOnWriteArraySet<Location>();

	protected final String dbKey;



	public SignType(final String dbKey, final String typeName) {
		if(dbKey    == null || dbKey.isEmpty()   ) throw new NullPointerException("dbKey not set");
		if(typeName == null || typeName.isEmpty()) throw new NullPointerException();
		this.dbKey = dbKey;
		// query db for signs
		{
			final dbQuery db = dbQuery.get(dbKey);
			signQueries.querySigns(db, typeName, this.locations);
			db.release();
		}
	}



	/**
	 * Validated sign click event
	 * @param event
	 * @param lines String[]
	 */
	public abstract void SignClicked(final PlayerInteractEvent event, final String[] lines);



	/**
	 * Validate a new sign to this type.
	 * @param player - Player affected by the event.
	 * @param lines - Sign contents to be parsed.
	 * @return true if line matches; false if not this sign type; null if invalid
	 */
	protected abstract Boolean ValidateSign(final Player player, final boolRef changed, String[] lines);



	// sign exists in this location
	public boolean hasLocation(final String world,
			final long x, final long y, final long z) {
		if(utils.isEmpty(world)) return false;
		for(final Location loc : this.locations) {
			if(!loc.getWorld().getName().equalsIgnoreCase(world)) continue;
			if( loc.getBlockX() != x ||
				loc.getBlockY() != y ||
				loc.getBlockZ() != z) continue;
			return true;
		}
		return false;
	}
	public boolean hasLocation(final Location location) {
		if(location == null) return false;
		return this.hasLocation(
			location.getWorld().getName(),
			location.getBlockX(),
			location.getBlockY(),
			location.getBlockZ()
		);
	}
	// new sign
	protected void addLocation(final Location location) {
		if(location == null) throw new NullPointerException();
		this.locations.add(location);
		signQueries.queryAddSign(this.dbKey, location, this);
	}
	/**
	 * Remove plugin sign.
	 * @param location Location of the sign.
	 * @return true if success; false if not relevant
	 */
	protected boolean removeLocation(final Location location) {
		if(location == null) throw new NullPointerException();
		if(!this.hasLocation(location)) return false;
		// remove from cache
		this.locations.remove(location);
		// remove from db
		signQueries.queryRemoveSign(this.dbKey, location);
		return true;
	}



	protected Integer parseLineValue(final String line, final String[] units) {
		String str = utilsString.trims(line.trim(), "[", "]");
		if(utils.isEmpty(str)) return null;
		String st = str.toLowerCase();
		for(final String unit : units) {
			try {
				if(st.startsWith(unit.toLowerCase())) {
					str = str.substring(unit.length()).trim();
					st  = str.toLowerCase();
				}
			} catch (Exception ignore) {}
		}
		try {
			return Integer.valueOf(Integer.parseInt(str));
		} catch (NumberFormatException ignore) {}
		return null;
	}



	// permissions
	protected abstract boolean canCreateSign(final Player player);
	protected abstract boolean canRemoveSign(final Player player);
	protected abstract boolean canUseSign   (final Player player);



	// language messages
	protected abstract String msgSignCreated();
	protected abstract String msgSignRemoved();
	protected abstract String msgNoPermission();
	protected abstract String msgNoCheating();



	// logger
	public static xLog log() {
		return SignManager.log();
	}



}
