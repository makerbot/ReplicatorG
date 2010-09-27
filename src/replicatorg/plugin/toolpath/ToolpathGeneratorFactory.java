package replicatorg.plugin.toolpath;

import java.io.File;
import java.util.Vector;

import replicatorg.app.Base;

public class ToolpathGeneratorFactory {
	public static class ToolpathGeneratorDescriptor {
		public String name;
		public String description;
		public Class<?> tpClass;
		
		public ToolpathGeneratorDescriptor(String name, String description, 
				Class<?> tpClass) {
			this.name = name;
			this.description = description;
			this.tpClass = tpClass;
		}
	
		public ToolpathGenerator instantiate() {
			try {
				return (ToolpathGenerator)tpClass.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static private Vector<ToolpathGeneratorDescriptor> generatorList = null;
	
	public static Vector<ToolpathGeneratorDescriptor> getGeneratorList() {
		if (generatorList == null) {
			generatorList = buildGeneratorList();
		}
		return generatorList;
	}
	static private Vector<ToolpathGeneratorDescriptor> buildGeneratorList() {
		Vector<ToolpathGeneratorDescriptor> list = new Vector<ToolpathGeneratorDescriptor>();
		class Skeinforge6 extends SkeinforgeGenerator {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-0006");
			}
			File getUserProfilesDir() {
		    	return Base.getApplicationFile("sf_profiles");
			}
		};
		class Skeinforge31 extends SkeinforgeGenerator {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-31/skeinforge_application");
			}
			File getUserProfilesDir() {
		    	return Base.getApplicationFile("sf_31_profiles");
			}
		};

		list.add(new ToolpathGeneratorDescriptor("Skeinforge (standard)", 
				"This is the standard version of skeinforge that has shipped with "+
				"ReplicatorG since 0016.", Skeinforge6.class));
		list.add(new ToolpathGeneratorDescriptor("Skeinforge (31)", 
				"This is the latest version of skeinforge.", Skeinforge31.class));
		
		return list;
	}

	static public String getSelectedName() {
		String name = Base.preferences.get("replicatorg.generator.name", "Skeinforge (standard)");
		return name;
	}

	static public void setSelectedName(String name) {
		Base.preferences.put("replicatorg.generator.name", name);
	}

	static public ToolpathGenerator createSelectedGenerator() {
		String name = getSelectedName();
		Vector<ToolpathGeneratorDescriptor> list = getGeneratorList();
		ToolpathGenerator tg = null;
		for (ToolpathGeneratorDescriptor tgd : list) {
			if (name.equals(tgd.name)) { tg = tgd.instantiate(); break; }
		}
		return tg;
	}
}
