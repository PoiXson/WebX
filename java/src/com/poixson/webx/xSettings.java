package com.poixson.webx;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsMath;
import com.poixson.commonjava.pxdb.dbQuery;


public class xSettings {

	private static Map<String, xSettings> instances = new ConcurrentHashMap<String, xSettings>();

	protected final String dbKey;
	protected final Map<String, String> values = new ConcurrentHashMap<String, String>();



	public static xSettings get(final String dbKey) {
		if(utils.isEmpty(dbKey)) throw new NullPointerException();
		// existing instance
		{
			final xSettings settings = instances.get(dbKey);
			if(settings != null)
				return settings;
		}
		// new instance
		{
			final xSettings settings = new xSettings(dbKey);
			instances.put(dbKey, settings);
			return settings;
		}
	}



	protected xSettings(final String dbKey) {
		this.dbKey = dbKey;
		this.Refresh();
	}



	public void Refresh() {
		final dbQuery db = dbQuery.get(this.dbKey);
		try {
			db.desc("getting settings from db");
			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT ")
				.append("`name`, `value` ")
				.append("FROM `_table_Settings`");
			db.Prepare(sql.toString());
			db.Execute();
			synchronized(this.values) {
				int count = 0;
				while(db.hasNext()) {
					final String name  = db.getString("name");
					final String value = db.getString("value");
					if(utils.isEmpty(name))
						continue;
					count++;
					this.values.put(
						name,
						value
					);
				}
System.out.println("Loaded [ "+Integer.toString(count)+" ] settings from db.");
//				log().info("Loaded [ "+Integer.toString(count)+" ] settings.");
			}
		} catch (SQLException e) {
e.printStackTrace();
//			log().trace(e);
		} finally {
			db.free();
		}
	}



	public void addDefault(final String name, final String value) {
		if(this.values.containsKey(name))
			return;
		synchronized(this.values) {
			if(this.values.containsKey(name))
				return;
			this.values.put(name, value);
			final dbQuery db = dbQuery.get(this.dbKey);
			try {
				db.desc("Adding default setting: "+name+" = "+value);
				final StringBuilder sql = new StringBuilder();
				sql.append("INSERT INTO `_table_Settings` (")
					.append("`name`, `value`")
					.append(") VALUES (")
					.append("?, ?")
					.append(")");
				db.Prepare(sql.toString());
				db.setString(1, name);
				db.setString(2, value);
				db.Execute();
			} catch (SQLException e) {
				e.printStackTrace();
//log().trace(e);
			} finally {
				db.free();
			}
		}
	}



	public String getString(final String name) {
		return this.values.get(name);
	}
	public Integer getInteger(final String name) {
		return utilsMath.toInteger(
			this.getString(name)
		);
	}
	public Double getDouble(final String name) {
		return utilsMath.toDouble(
			this.getString(name)
		);
	}
	public Boolean getBoolean(final String name) {
		return utilsMath.toBoolean(
			this.getString(name)
		);
	}



}
