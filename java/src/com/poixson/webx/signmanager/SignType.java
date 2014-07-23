package com.poixson.webx.signmanager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;


public abstract class SignType {

	protected final Set<SignLocation> locations = new CopyOnWriteArraySet<SignLocation>();



	// validate sign
	/**
	 * Match the second line of a sign with possible aliases.
	 * @param lines - Sign contents to be parsed.
	 * @return true if line matches; false if not this sign type.
	 */
	public abstract boolean ValidateSign(String[] lines);
	/**
	 * Used after initial validation, to sanitize last 3 lines.
	 * @param lines - Sign contents, to be parsed and formatted.
	 * @return true if sign changed; false if sign is perfect.
	 */
	public abstract boolean FullValidateSign(String[] lines);

	// permissions
	public abstract boolean canCreateSign(final Player player);
	public abstract boolean canRemoveSign(final Player player);
	public abstract boolean canUseSign   (final Player player);

	// queries
	public abstract void queryAddSign   (final Sign sign);
	public abstract void queryRemoveSign(final Sign sign);

	// events
	public abstract void SignClicked(final Sign sign, final Player player);



	// sign location dao
	public SignLocation getSignLocation(final String world,
			final long x, final long y, final long z) {
		for(final SignLocation loc : this.locations) {
			if(loc.world.equalsIgnoreCase(world) &&
					loc.x == x && loc.y == y && loc.z == z)
				return loc;
		}
		return null;
	}

	public static class SignLocation {

		public final String world;
		public final long x;
		public final long y;
		public final long z;

		public SignLocation(final String world,
				final long x, final long y, final long z) {
			this.world = world;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		public SignLocation(final ResultSet rs) throws SQLException {
			this.world = rs.getString("world");
			this.x = rs.getLong("x");
			this.y = rs.getLong("y");
			this.z = rs.getLong("z");
		}

	}



}
