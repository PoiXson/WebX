package com.poixson.webx.bukkit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.plugin.java.JavaPlugin;


public abstract class mcPlugin extends JavaPlugin {

	private static Map<String, mcPlugin> plugins = new ConcurrentHashMap<String, mcPlugin>();

	// plugin info
	protected final String name;
	protected final String title;
//	private volatile String availableVersion = null;
	// plugin state
	public enum PluginState {CLOSED, LOADING, OK, STOPPING, FAILED};
	private volatile PluginState state = PluginState.CLOSED;

	// fail messages
	private final List<String> failMessages = new CopyOnWriteArrayList<String>();



	// get plugin by name
	public static mcPlugin getPlugin(final String name) {
		if(name == null || name.isEmpty()) return null;
		return plugins.get(name);
	}
	// new plugin instance
	public mcPlugin(final String name, final String title) {
		// only allow one instance of each plugin
		synchronized(plugins) {
			if(plugins.containsKey(name))
				throw new IllegalStateException(
					"Plugin '"+name+"' is already loaded. Cannot initiate a second instance.");
			plugins.put(name, this);
			this.state = PluginState.CLOSED;
		}
		this.name  = name;
		this.title = title;
		this.InitPlugin();
	}



	@Override
	public void onEnable() {
		switch(this.state) {
		// previous fail
		case FAILED:
			log().warning("Plugin "+this.title()+" has previously failed. Trying again..");
			this.failMessages.clear();
			this.state = PluginState.CLOSED;
			break;
		// bad state?
		case LOADING:
			throw new IllegalStateException("Plugin '"+this.getName()+"' is already loading?");
		case OK:
			throw new IllegalStateException("Plugin '"+this.getName()+"' is already loaded?");
		case STOPPING:
			throw new IllegalStateException("Plugin '"+this.getName()+"' is stopping?");
		// ready for startup
		case CLOSED:
		default:
			break;
		}
		this.state = PluginState.LOADING;
		// start plugin
		this.StartPlugin();
		// startup successful
		if(PluginState.LOADING.equals(this.state))
			this.state = PluginState.OK;
	}
	@Override
	public void onDisable() {
		switch(this.state) {
		// already stopped
		case CLOSED:
		case STOPPING:
			return;
		// proceed stopping
		case OK:
		case LOADING:
		case FAILED:
		default:
			break;
		}
		this.state = PluginState.STOPPING;
		// stop plugin
		this.StopPlugin();
		// stop successful
		if(PluginState.STOPPING.equals(this.state))
			this.state = PluginState.CLOSED;
	}



	protected abstract void InitPlugin();
	protected abstract void StartPlugin();
	protected abstract void StopPlugin();
	protected abstract void ReloadPlugin();



	public String getDisplayName() {
		return this.title+" "+this.getVersion();
	}
	public String name() {
		return this.name;
	}
	public String title() {
		return this.title;
	}



	// plugin version
	public String getVersion() {
		return getDescription().getVersion();
	}
	// available version
	public String getAvailableVersion() {
//TODO:
		return null;
	}
	// new version is available
	public String isUpdateAvailable() {
//TODO:
		return null;
	}



	// plugin startup failed
	public boolean hasFailed() {
		return PluginState.FAILED.equals(this.state);
	}
	public void fail(final String msg) {
		boolean hasAlreadyFailed = this.hasFailed();
		this.state = PluginState.FAILED;
		if(msg != null && !msg.isEmpty())
			this.failMessages.add(msg);
		log().severe("Failed to load "+this.title()+" "+msg);
		if(!hasAlreadyFailed)
			this.onDisable();
	}
	public void fail() {
		this.fail(null);
	}



	//TODO: temp logger
	public java.util.logging.Logger log() {
		return this.getLogger();
	}



}
