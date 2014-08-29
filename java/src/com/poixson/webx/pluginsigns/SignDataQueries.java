package com.poixson.webx.pluginsigns;

import java.sql.SQLException;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.poixson.commonjava.pxdb.dbQuery;
import com.poixson.commonjava.xLogger.xLog;


public class signQueries {



	private signQueries() {}
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}



	// query db for signs
	public static void querySigns(final dbQuery query, final String name,
			final Set<Location> locations) {
		if(query == null) throw new NullPointerException();
		if(name == null || name.isEmpty()) throw new NullPointerException();
		final String nameStr = name.toUpperCase();
		try {
			log().fine("Query: "+nameStr+" Signs");
			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT ")
				.append("`world`, `x`, `y`, `z`, `valueA`, `valueB` ")
				.append("FROM `_table_Signs` ")
				.append("WHERE `type` = ?");
			query.prepare(sql.toString());
			query.setString(1, nameStr);
			query.exec();
			while(query.next()) {
				final World world = Bukkit.getWorld(query.getString("world"));
				if(world == null) {
					log().warning("Sign world not found: "+query.getString("world"));
					continue;
				}
				locations.add(
					new Location(
						world,
						query.getLng("x"),
						query.getLng("y"),
						query.getLng("z")
					)
				);
			}
		} catch (SQLException e) {
			log().severe("Failed to query "+nameStr+" signs");
			log().trace(e);
		} finally {
			query.clean();
		}
	}



	// add sign to db
	public static void queryAddSign(final String dbKey,
			final Location location, final SignType signType) {
	}



	// remove sign from db
	public static void queryRemoveSign(final String dbKey,
			final Location location) {
	}



	// logger
	public static xLog log() {
		return SignManager.log();
	}



}
