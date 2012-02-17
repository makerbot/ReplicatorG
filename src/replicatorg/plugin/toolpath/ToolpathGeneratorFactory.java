package replicatorg.plugin.toolpath;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;

import replicatorg.app.Base;
import replicatorg.plugin.toolpath.skeinforge.PrintOMatic;
import replicatorg.plugin.toolpath.skeinforge.PrintOMatic5D;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator;

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
			
			{
				displayName = "Skeinforge (6)";
			}
			
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-0006");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_profiles");
			}
			public List<SkeinforgePreference> initPreferences() {				
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use Raft/Support",
						"replicatorg.skeinforge.useRaft", true,
						"Enables Raft and/or support material.  " + 
						"Enabled: add a 'raft' of plastic before starting the build. If overhangs are detected, add support material.");
				raftPref.addNegateableOption(new SkeinforgeOption("Raft", "Activate Raft:", "true"));
				raftPref.addNegateableOption(new SkeinforgeOption("Raftless", "Activate Raftless:", "false"));
				prefs.add(raftPref);

				return prefs;
			}
		};

		class Skeinforge31 extends SkeinforgeGenerator {

			{
				displayName = "Skeinforge (31)";
			}
			
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-31/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_31_profiles");
			}
			public List<SkeinforgePreference> initPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use Raft/Support",
						"replicatorg.skeinforge.useRaft", true,
						"Enables Raft and/or support material.  " + 
						"Enabled: add a 'raft' of plastic before starting the build. If overhangs are detected, add support material.");
				raftPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Activate Raft", "true"));
				prefs.add(raftPref);

				return prefs;
			}
		};
		
		class Skeinforge35 extends SkeinforgeGenerator {

			{
				displayName = "Skeinforge (35)";
			}
			
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-35/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_35_profiles");
			}
			public List<SkeinforgePreference> initPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				
				prefs.add(postprocess.getPreference());
				
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use Raft/Support",
						"replicatorg.skeinforge.useRaft", false,
						"Enables Raft and/or support material.  " + 
						"Enabled: add a 'raft' of plastic before starting the build. If overhangs are detected, add support material.");
				raftPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit and Set Altitude:", "true"));
				prefs.add(raftPref);
				
				SkeinforgeChoicePreference supportPref =
					new SkeinforgeChoicePreference("Use support material",
							"replicatorg.skeinforge.choiceSupport", "None",
							"If this option is selected, skeinforge will attempt to support large overhangs by laying down a support "+
							"structure that you can later remove. Requires that Raft/Support be checked.");
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","None", "true"));
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","Everywhere", "false"));
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","Exterior Only", "false"));

				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","None", "false"));
				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Everywhere", "false"));
				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Exterior Only", "true"));

				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","None", "false"));
				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Everywhere", "true"));
				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Exterior Only", "false"));
				
				prefs.add(supportPref);

				// This will be done by the SkeinforgePostProcessor
				SkeinforgeBooleanPreference bookendPref = 	
					new SkeinforgeBooleanPreference("Use default start/end gcode",	"replicatorg.skeinforge.useMachineBookend", true,
						"<html>Use the start and end.gcode defined in machines/*.xml for the currently selected machine.<br/>" +
						"Uncheck this to use custom start and end.gcode from the skeinforge profile.</html>");
				bookendPref.addTrueOption(new SkeinforgeOption("preface.csv", "Name of Start File:", ""));
				bookendPref.addTrueOption(new SkeinforgeOption("preface.csv", "Name of End File:", ""));
				final JCheckBox bookendBox = (JCheckBox)bookendPref.getUI();
				final ActionListener a = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(bookendBox.isSelected()) {
							postprocess.setPrependStart(true);
							postprocess.setAppendEnd(true);
						} else {
							postprocess.setPrependStart(false);
							postprocess.setAppendEnd(false);
						}
					}
				};
				bookendBox.addActionListener(a);
				// set initial state
				a.actionPerformed(null);
				prefs.add(bookendPref);
				
				PrintOMatic printOMatic = new PrintOMatic();
				prefs.add(printOMatic);

				return prefs;
			}
		};

		class Skeinforge40 extends SkeinforgeGenerator {

			{
				displayName = "Skeinforge (40) - experimental";
			}
			
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-40/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_40_profiles");
			}
			public List<SkeinforgePreference> initPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use Raft/Support",
						"replicatorg.skeinforge.useRaft", true,
						"Enables Raft and/or support material.  " + 
						"Enabled: add a 'raft' of plastic before starting the build. If overhangs are detected, add support material.");
				raftPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit:", "true"));
				prefs.add(raftPref);
				SkeinforgeChoicePreference supportPref =
					new SkeinforgeChoicePreference("Use support material",
							"replicatorg.skeinforge.choiceSupport", "None",
							"If this option is selected, skeinforge will attempt to support large overhangs by laying down a support "+
							"structure that you can later remove. Requires that Raft/Support be checked.");
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","None", "true"));
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","Everywhere", "false"));
				supportPref.addOption("None", new SkeinforgeOption("raft.csv","Exterior Only", "false"));

				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","None", "false"));
				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Everywhere", "false"));
				supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Exterior Only", "true"));

				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","None", "false"));
				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Everywhere", "true"));
				supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Exterior Only", "false"));
				
				prefs.add(supportPref);

				PrintOMatic printOMatic = new PrintOMatic();
				prefs.add(printOMatic);

				return prefs;
			}
		};
		
		class Skeinforge44 extends SkeinforgeGenerator {

			{
				displayName = "Skeinforge (44) - experimental";
			}
			
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-44/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_44_profiles");
			}
			public List<SkeinforgePreference> initPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				
				PrintOMatic5D printOMatic5D = new PrintOMatic5D();
				prefs.add(printOMatic5D);

				return prefs;
			}
		};
		
		class Skeinforge47 extends SkeinforgeGenerator {

			{
				displayName = "Skeinforge (47)";
			}
			
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-47/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_47_profiles");
			}
			public List<SkeinforgePreference> initPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();

				prefs.add(postprocess.getPreference());
				
				SkeinforgeBooleanPreference raftPref = 			
						new SkeinforgeBooleanPreference("Use Raft/Support",
							"replicatorg.skeinforge.useRaft", false,
							"Enables Raft and/or support material.  " + 
							"Enabled: add a 'raft' of plastic before starting the build. If overhangs are detected, add support material.");
					raftPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit:", "true"));
					prefs.add(raftPref);
					
					SkeinforgeChoicePreference supportPref =
						new SkeinforgeChoicePreference("Use support material",
								"replicatorg.skeinforge.choiceSupport", "None",
								"If this option is selected, skeinforge will attempt to support large overhangs by laying down a support "+
								"structure that you can later remove. Requires that Raft/Support be checked.");
					supportPref.addOption("None", new SkeinforgeOption("raft.csv","None", "true"));
					supportPref.addOption("None", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
					supportPref.addOption("None", new SkeinforgeOption("raft.csv","Everywhere", "false"));
					supportPref.addOption("None", new SkeinforgeOption("raft.csv","Exterior Only", "false"));

					supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","None", "false"));
					supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
					supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Everywhere", "false"));
					supportPref.addOption("Exterior support", new SkeinforgeOption("raft.csv","Exterior Only", "true"));

					supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","None", "false"));
					supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Empty Layers Only", "false"));
					supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Everywhere", "true"));
					supportPref.addOption("Full support", new SkeinforgeOption("raft.csv","Exterior Only", "false"));
					
					prefs.add(supportPref);
					
				// This will be done by the SkeinforgePostProcessor
				SkeinforgeBooleanPreference bookendPref = 	
					new SkeinforgeBooleanPreference("Use default start/end gcode",	"replicatorg.skeinforge.useMachineBookend", true,
						"<html>Use the start and end.gcode defined in machines/*.xml for the currently selected machine.<br/>" +
						"Uncheck this to use custom start and end.gcode from the skeinforge profile.</html>");
				bookendPref.addTrueOption(new SkeinforgeOption("alteration.csv", "Name of Start File:", ""));
				bookendPref.addTrueOption(new SkeinforgeOption("alteration.csv", "Name of End File:", ""));
				final JCheckBox bookendBox = (JCheckBox)bookendPref.getUI();
				final ActionListener a = new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(bookendBox.isSelected()) {
							postprocess.setPrependStart(true);
							postprocess.setAppendEnd(true);
						} else {
							postprocess.setPrependStart(false);
							postprocess.setAppendEnd(false);
						}
					}
				};
				bookendBox.addActionListener(a);
				// set initial state
				a.actionPerformed(null);
				prefs.add(bookendPref);
				
				PrintOMatic5D printOMatic5D = new PrintOMatic5D();
				prefs.add(printOMatic5D);
				
				return prefs;
			}
		};
		
		if((new Skeinforge47()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge47.displayName, 
				"This is a new version of skeinforge.", Skeinforge47.class));
		if((new Skeinforge44()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge44.displayName, 
				"This is an experimental version of skeinforge.", Skeinforge44.class));
		if((new Skeinforge40()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge40.displayName, 
				"This is a recent version of skeinforge.", Skeinforge40.class));
		if((new Skeinforge35()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge35.displayName, 
				"This is the default version of skeinforge.", Skeinforge35.class));
		if((new Skeinforge31()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge31.displayName, 
				"This is an old version of skeinforge.", Skeinforge31.class));
		if((new Skeinforge6()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge6.displayName, 
				"This is an ancient version of skeinforge.", Skeinforge6.class));
		
		return list;
	}

	static public String getSelectedName() {
		String name = Base.preferences.get("replicatorg.generator.name", "Skeinforge (35)");
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
