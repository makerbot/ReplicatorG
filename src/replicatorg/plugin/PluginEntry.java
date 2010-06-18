package replicatorg.plugin;

public class PluginEntry {
	private String name;
	private String description;
	private boolean enabled;
	private Object plugin;
	
	public PluginEntry(String name, String description, Object pluginObject) {
		this.name = name;
		this.description = description;
		this.plugin = pluginObject;
		this.enabled = true;
	}
	
	public Object getPlugin() { return plugin; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
