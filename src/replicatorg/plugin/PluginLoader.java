package replicatorg.plugin;

/***
 * The ReplicatorG plugin loader looks for valid plugin jars in the ~/.replicatorg/plugins directory.
 * A valid plugin jar contains a few classes and a plugin.properties file at the root which looks like
 * the following:
 * 
 * plugin.class = fully.qualified.name.of.your.Plugin
 * plugin.name = Short name of plugin
 * plugin.description = A longer (~50 words) description of what this plugin does
 * 
 * The plugin type is implicit in the interfaces returned by the plugin.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class PluginLoader {
	static PluginLoader instance = new PluginLoader();
	
	private PluginLoader() {}
	
	public PluginLoader getInstance() { return instance; }
	
	private Vector<File> getCandidateJars() {
		Vector<File> candidates = new Vector<File>();
		String path = System.getProperty("user.home") +
			File.separator +
			".replicatorg" +
			File.separator +
			"plugins";
		File dir = new File(path);
		if (dir.isDirectory()) {
			String[] names=dir.list();
			for (String name: names) {
				if (name.endsWith(".jar")) {
					candidates.add(new File(dir,name));
				}
			}
		}
		return candidates;
	}
	
	public Vector<PluginEntry> loadPlugins()
	{
		Vector<PluginEntry> plugins = new Vector<PluginEntry>();
		for (File f: getCandidateJars()) {
			if (f.isFile()) {
				try {
					JarFile jar = new JarFile(f);
					ZipEntry entry = jar.getEntry("plugin.properties");
					if (entry == null || entry.isDirectory()) continue;
					InputStream is = jar.getInputStream(entry);
					Properties props = new Properties();
					props.load(is);
					String className = props.getProperty("plugin.class");
					String name = props.getProperty("plugin.name");
					String description = props.getProperty("plugin.description");
					is.close();
					jar.close();
					if (className == null || className.length() == 0 ||
							name == null || name.length() == 0)
					{
						System.err.println("Malformed plugin.properties found in "+f.getName());
						continue;
					}
					// Load class
					URL urls[] = { f.toURI().toURL() };
					URLClassLoader cl = new URLClassLoader(urls);
					Class c;
					try {
						c = cl.loadClass(className.trim());
						Object o = c.newInstance();
						plugins.add(new PluginEntry(name,description,o));
					} catch (ClassNotFoundException e) {
						System.err.println("Could not find class "+className+" in "+f.getName());
					} catch (InstantiationException e) {
						System.err.println("Could not instantiate "+className);
					} catch (IllegalAccessException e) {
						System.err.println("Could not instantiate "+className);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return plugins;
	}

}
