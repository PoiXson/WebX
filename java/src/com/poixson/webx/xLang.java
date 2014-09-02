package com.poixson.webx;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.poixson.commonjava.Utils.utils;
import com.poixson.commonjava.Utils.utilsDirFile;
import com.poixson.commonjava.Utils.utilsSan;
import com.poixson.commonjava.Utils.utilsString;
import com.poixson.commonjava.xLogger.xLog;


public class xLang {

	protected final Map<String, String> phrases  = new ConcurrentHashMap<String, String>();
	protected final Map<String, String[]> arrays = new ConcurrentHashMap<String, String[]>();
	// null=not loaded  ""=defaults
	protected volatile String current = null;



	public xLang() {
//		this.LoadDefaults();
	}
	public void LoadDefaults() {
		synchronized(this.phrases) {
			this.phrases.clear();
			this.arrays.clear();
			this.doLoadDefaults();
			this.current = "";
		}
	}



	// default phrases
	protected void doLoadDefaults() {
		addDefault(webxDefines.LANG_CHAT_PREFIX,        "[WebX] "           );
		addDefault(webxDefines.LANG_RELOADING,          "Reloading.."       );
		addDefault(webxDefines.LANG_FINISHED_RELOADING, "Finished reloading");
		addDefault(webxDefines.LANG_NO_PERMISSION,      "No permission."    );
		addDefault(webxDefines.LANG_NO_CHEATING,        "No cheating!"      );
		addDefault(webxDefines.LANG_CLICK_SPAM,         "Please wait."      );
		// signs
		addDefault(webxDefines.LANG_SIGN_INVALID,       "Invalid sign."     );
		addDefault(webxDefines.LANG_SIGN_REMOVED,       "Sign removed."     );
		addDefault(webxDefines.LANG_SIGN_MASTER,        new String[] { "Plugin" });
		// mailboxes
		addDefault(webxDefines.LANG_MAILBOX_TITLE,      "MailBox");
		addDefault(webxDefines.LANG_MAILBOX_OPENED,     "MailBox opened.");
		addDefault(webxDefines.LANG_MAILBOX_CLOSED,     "MailBox saved.");
	}
	protected void addDefault(final String name, final String msg) {
		if(utils.isEmpty(name)) throw new NullPointerException();
		if(utils.isEmpty(msg) ) throw new NullPointerException();
		this.phrases.put(name, msg);
	}
	protected void addDefault(final String name, final String[] msgs) {
		if(utils.isEmpty(name)) throw new NullPointerException();
		if(msgs == null)        throw new NullPointerException();
		this.arrays.put(name, msgs);
	}



	// load language file
	public boolean Load(final Plugin plugin, final String lang) {
		if(plugin == null)      throw new NullPointerException();
		if(utils.isEmpty(lang)) throw new NullPointerException("lang not set, cannot load");
		// load defaults first
		this.LoadDefaults();
		// load lang file
		if(lang != null && !lang.isEmpty()) {
			try {
				final String langSan = utilsSan.FileName(lang);
				//if(langSan.length() != 2) throw new IllegalArgumentException("lang must be specified as 2 letters");
				final YamlConfiguration yml = LoadYaml(
					plugin,
					"languages/"+utilsString.ensureEnds(".yml", langSan)
				);
				if(yml != null) {
					// populate phrases
					synchronized(this.phrases) {
						for(final String key : yml.getKeys(false)) {
							if(utils.isEmpty(key))   continue;
							if(key.startsWith("#"))  continue;
							if(key.startsWith("//")) continue;
							// msg string
							if(this.phrases.containsKey(key))
								this.phrases.put(key, yml.getString(key));
							else
							// msg array
							if(this.arrays.containsKey(key))
								this.arrays.put(key, yml.getStringList(key).toArray(new String[0]));
							// unknown type
							else
								log().warning("Unknown lang key in "+utilsString.ensureEnds(".yml", langSan)+" '"+key+"'");
						}
					}
					this.current = langSan;
					return true;
				}
			} catch (Exception e) {
				log().trace(e);
			}
		}
		return false;
	}



	// find and load .yml file
	private YamlConfiguration LoadYaml(final Plugin plugin, final String filename) {
		YamlConfiguration yml = null;
		final String fname = utilsString.ensureEnds(".yml", filename);
		// load from plugins/
		yml = loadFromFileSystem(plugin, fname);
		if(yml != null) {
			log().stats("Loaded file: "+fname);
			return yml;
		}
		// load from jar
		yml = loadFromResource(plugin, fname);
		if(yml != null) {
			log().stats("Loaded resource: "+fname);
			return yml;
		}
		// file not found
		return null;
	}
	// load from plugins/name/languages/lang.yml
	private YamlConfiguration loadFromFileSystem(final Plugin plugin, final String path) {
		final String dir = plugin.getDataFolder().toString();
		final File file = new File(
			utilsDirFile.buildFilePath(
				dir, path, ".yml"
			)
		);
		try {
			// file exists
			if(file.exists() && file.canRead())
				return YamlConfiguration.loadConfiguration(file);
		} catch (Exception e) {
			log().trace(e);
		}
		return null;
	}
	// load from jar resource
	private YamlConfiguration loadFromResource(final Plugin plugin, final String path) {
		try {
			final InputStream stream = plugin.getClass().getClassLoader().getResourceAsStream(path);
			if(stream != null)
				return YamlConfiguration.loadConfiguration(
						new InputStreamReader(stream)
				);
		} catch (Exception e) {
			log().trace(e);
		}
		return null;
	}



	// get message/phrase
	public String getMsg(final String key) {
		if(key == null || key.isEmpty()) throw new NullPointerException();
		if(this.current == null)
			this.LoadDefaults();
		final String msg = this.phrases.get(key);
		if(!utils.isEmpty(msg))
			return msg;
		log().severe("Language message/phrase not found: "+key);
		return "<lang:"+key+">";
	}
	public String[] getMsgArray(final String key) {
		if(key == null || key.isEmpty()) throw new NullPointerException();
		if(this.current == null)
			this.LoadDefaults();
		final String[] arr = this.arrays.get(key);
		if(arr != null)
			return arr;
		log().severe("Language message/phrase not found: "+key);
		return null;
	}



	// logger
	public static xLog log() {
		return xLog.getRoot();
	}



}
