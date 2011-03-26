package replicatorg.plugin.toolpath;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import replicatorg.app.Base;
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
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-0006");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_profiles");
			}
			public List<SkeinforgePreference> getPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use raft",
						"replicatorg.skeinforge.useRaft", true,
						"If this option is checked, skeinforge will lay down a rectangular 'raft' of plastic before starting the build.  "
						+ "Rafts increase the build size slightly, so you should avoid using a raft if your build goes to the edge of the platform.");
				raftPref.addNegateableOption(new SkeinforgeOption("Raft", "Activate Raft:", "true"));
				raftPref.addNegateableOption(new SkeinforgeOption("Raftless", "Activate Raftless:", "false"));
				prefs.add(raftPref);
				return prefs;
			}
		};

		class Skeinforge31 extends SkeinforgeGenerator {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-31/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_31_profiles");
			}
			public List<SkeinforgePreference> getPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use raft",
						"replicatorg.skeinforge.useRaft", true,
						"If this option is checked, skeinforge will lay down a rectangular 'raft' of plastic before starting the build.  "
						+ "Rafts increase the build size slightly, so you should avoid using a raft if your build goes to the edge of the platform.");
				raftPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Activate Raft", "true"));
				prefs.add(raftPref);
				return prefs;
			}
		};
		
		class Skeinforge35 extends SkeinforgeGenerator {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-35/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_35_profiles");
			}
			public List<SkeinforgePreference> getPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();
				SkeinforgeBooleanPreference raftPref = 			
					new SkeinforgeBooleanPreference("Use raft",
						"replicatorg.skeinforge.useRaft", true,
						"If this option is checked, skeinforge will lay down a rectangular 'raft' of plastic before starting the build.  "
						+ "Rafts increase the build size slightly, so you should avoid using a raft if your build goes to the edge of the platform.");
				raftPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit and Set Altitude:", "true"));
				prefs.add(raftPref);
				SkeinforgeChoicePreference supportPref =
					new SkeinforgeChoicePreference("Use support material",
							"replicatorg.skeinforge.choiceSupport", "None",
							"If this option is selected, skeinforge will attempt to support large overhangs by laying down a support "+
							"structure that you can later remove.");
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
				
				SkeinforgeBooleanPreference printOMaticPref =
					new SkeinforgeBooleanPreference("Use Print-O-Matic",
							"replicatorg.skeinforge.printOMaticPref", true,
							"If this option is checked, skeinforge will use the values below to control the print");
				printOMaticPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit and Set Altitude:", "true"));
				prefs.add(printOMaticPref);
				
				PrintOMatic printOMatic = new PrintOMatic();
				prefs.add(printOMatic);
					
//				SkeinforgeValuePreference desiredFeedrate = 
//					new SkeinforgeValuePreference("Desired Feedrate (mm/s)",
//							"replicatorg.skeinforge.printOMatic.desiredFeedrate", "30",
//							"slow: 0-20, default: 30, Fast: 40+");
//				prefs.add(desiredFeedrate);
//				
//				SkeinforgeValuePreference desiredLayerHeight = 
//					new SkeinforgeValuePreference("Desired Layer Height (mm)",
//							"replicatorg.skeinforge.printOMatic.desiredLayerHeight", "0.35",
//							"Set the desired feedrate");
//				prefs.add(desiredFeedrate);
//				
//				SkeinforgeValuePreference filamentDiameter = 
//					new SkeinforgeValuePreference("Filament Diameter (mm)",
//							"replicatorg.skeinforge.printOMatic.filamentDiameter", "2.98",
//							"measure feedstock");
//				prefs.add(filamentDiameter);
//				
//				SkeinforgeValuePreference nozzleDiameter = 
//					new SkeinforgeValuePreference("Nozzle Diameter (mm)",
//							"replicatorg.skeinforge.printOMatic.nozzleDiameter", "0.5",
//							"exit hole diameter");
//				prefs.add(nozzleDiameter);
//				
//				SkeinforgeValuePreference driveGearDiameter = 
//					new SkeinforgeValuePreference("Drive Gear Diameter (mm)",
//							"replicatorg.skeinforge.printOMatic.driveGearDiameter", "10.58",
//							"measure at teeth");
//				prefs.add(driveGearDiameter);
//
//				SkeinforgeValuePreference driveGearFudgeFactor = 
//					new SkeinforgeValuePreference("GEAR DIAMETER FUDGE FACTOR",
//							"replicatorg.skeinforge.printOMatic.driveGearFudgeFactor", "0.85",
//							"ABS = 0.85, PLA = 1");
//				prefs.add(driveGearFudgeFactor);
//				
//				SkeinforgeValuePreference modelHasThinFeatures = 
//					new SkeinforgeValuePreference("No thin features",
//							"replicatorg.skeinforge.printOMatic.modelHasThinFeatures", "1",
//							"Model does not contain any thin features (<2.5mm) (1=true, 0=false)");
//				prefs.add(modelHasThinFeatures);
//				
//				SkeinforgeValuePreference reversalDistance = 
//					new SkeinforgeValuePreference("Extruder Reversal Distance (mm)",
//							"replicatorg.skeinforge.printOMatic.reversalDistance", "1.235",
//							"input distance");
//				prefs.add(reversalDistance);
//				
//				SkeinforgeValuePreference reversalPushBack = 
//					new SkeinforgeValuePreference("Extruder Push Back Distance (mm)",
//							"replicatorg.skeinforge.printOMatic.reversalPushBack", "1.285",
//							"input distance (Push back should be slightly longer to overcome nozzle pressure)");
//				prefs.add(reversalPushBack);
//				
//				SkeinforgeValuePreference reversalSpeed = 
//					new SkeinforgeValuePreference("Reversal Speed (RPM)",
//							"replicatorg.skeinforge.printOMatic.reversalSpeed", "35",
//							"35 is default for 3mm, 60 is default for 1.75");
//				prefs.add(reversalSpeed);
				
				return prefs;
			}
		};

		class Skeinforge39 extends Skeinforge35 {
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-39/skeinforge_application");
			}
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_39_profiles");
				
			}
		};
		list.add(new ToolpathGeneratorDescriptor("Skeinforge (standard)", 
				"This is the standard version of skeinforge that has shipped with "+
				"ReplicatorG since 0016.", Skeinforge6.class));
		list.add(new ToolpathGeneratorDescriptor("Skeinforge (39)", 
				"This is the latest version of skeinforge.", Skeinforge39.class));
		list.add(new ToolpathGeneratorDescriptor("Skeinforge (35)", 
				"This a recent version of skeinforge.", Skeinforge35.class));
		list.add(new ToolpathGeneratorDescriptor("Skeinforge (31)", 
				"This is an old version of skeinforge.", Skeinforge31.class));
		
		
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