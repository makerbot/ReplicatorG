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
import replicatorg.plugin.toolpath.slic3r.Slic3rGenerator;
import replicatorg.plugin.toolpath.miraclegrue.MiracleGrueGenerator;

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
		
		class Slic3r071 extends Slic3rGenerator {
			{ 	displayName = "Slic3r 0.7.1"; }
			
			/** return directory where slicer exists */
			public File getDefaultSlic3rDir() {
				String Slic3rDir = "skein_engines/slic3r_engines";
				if (Base.isMacOS()) {
					/// For mac, we want to use the slic3r app bundle. Odd, I know
					Slic3rDir = "/Applications/Slic3r.app/Contents/MacOS";
					Base.logger.finer("Slic3r in app data");
					File absSlicerLocation = new File(Slic3rDir);
					if( absSlicerLocation.exists() == false ) {
						Base.logger.severe("Slic3r on mac requires Slicer.app is installed in Applications");
						//throw new RuntimeException("Slic3r on Mac requires Slicer.app is installed in Applications");
						return Base.getApplicationFile("slicer_unavailable");
					}
					return absSlicerLocation;
				}
				if (Base.isLinux()) {
					Slic3rDir +=  "/linux/bin";
				}
				if (Base.isWindows()) {
					Slic3rDir +=  "/windows";
				}
				File x = Base.getApplicationFile(Slic3rDir);
				return x;
			}

			/// Returns the directory containing preference directories. Each sub-directory in the 
			/// returned directory is a specific preference
			public File getUserProfilesDir() {
		    	return Base.getUserDir("skein_engines/slic3r_engines/prefs");
			}
			public List<Slic3rPreference> initPreferences() {				
				List <Slic3rPreference> prefs = new LinkedList<Slic3rPreference>();
				/*
				Slic3rBooleanPreference raftPref = 			
					new Slic3rBooleanPreference("Use Raft/Support",
						"replicatorg.slic3r.useRaft", true,
						"Enables Raft and/or support material.  " + 
						"Enabled: add a 'raft' of plastic before starting the build. If overhangs are detected, add support material.");
				raftPref.addNegateableOption(new Slic3rOption("Raft", "Activate Raft:", "true"));
				raftPref.addNegateableOption(new Slic3rOption("Raftless", "Activate Raftless:", "false"));
				prefs.add(raftPref);
				*/
				return prefs;
			}
		};
		
		class Skeinforge31 extends SkeinforgeGenerator {

			{ displayName = "Skeinforge (31)"; }
			
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

			{ displayName = "Skeinforge (35)"; }
			
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
						"replicatorg.skeinforge.useRaft", true,
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

		class Skeinforge50 extends SkeinforgeGenerator {

			{
				displayName = "Skeinforge (50)";
			}
			
			public File getDefaultSkeinforgeDir() {
		    	return Base.getApplicationFile("skein_engines/skeinforge-50/skeinforge_application");
			}
			
			public File getUserProfilesDir() {
		    	return Base.getUserFile("sf_50_profiles");
			}
			
			public List<SkeinforgePreference> initPreferences() {
				List <SkeinforgePreference> prefs = new LinkedList<SkeinforgePreference>();

				prefs.add(postprocess.getPreference());
				
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

		
		class MiracleGrueBeta extends MiracleGrueGenerator {
			{
				displayName = "Miracle-Grue Beta 0.0.4.0";
			}
			
			public File getDefaultMiracleGrueDir() {
				String target = "skein_engines/mg_engines";
				if (Base.isMacOS()) {
					target += "/mac";
					//Base.logger.severe("base dir:" + target);
				}
				if (Base.isLinux()) {
					if(Base.isx86_64()) target += "/linux/x86_64";
					else  target += "/linux/x86";
				}	
				if (Base.isWindows() ) {
					target += "/windows";
				}
				File x = Base.getApplicationFile(target);
				return x;
			}

			/// Returns the directory of profiles, if needed copies those to a local 
			// users preferences location
			public File getUserProfilesDir() {
		    	return Base.getUserDir("skein_engines/mg_engines/profiles");
			}

			public List<MiracleGruePreference> initPreferences() {				
				List <MiracleGruePreference> prefs = new LinkedList<MiracleGruePreference>();
				return prefs;
			}
		};
		
		
		if((new MiracleGrueBeta()).getDefaultMiracleGrueDir().exists())
			list.add(new ToolpathGeneratorDescriptor(MiracleGrueBeta.displayName, 
				"This is the latest version of MiracleGrue.", MiracleGrueBeta.class));
		if((new Slic3r071()).getDefaultSlic3rDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Slic3r071.displayName, 
				"This is the latest version of Slic3r.", Slic3r071.class));
		if((new Skeinforge50()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge50.displayName, 
				"This is the default version of skeinforge.", Skeinforge50.class));
		if((new Skeinforge47()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge47.displayName, 
				"This is the an older version of skeinforge.", Skeinforge47.class));
		if((new Skeinforge44()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge44.displayName, 
				"This is an experimental version of skeinforge.", Skeinforge44.class));
		if((new Skeinforge40()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge40.displayName, 
				"This is a recent version of skeinforge.", Skeinforge40.class));
		if((new Skeinforge35()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge35.displayName, 
				"This is a decent version of skeinforge.", Skeinforge35.class));
		if((new Skeinforge31()).getDefaultSkeinforgeDir().exists())
			list.add(new ToolpathGeneratorDescriptor(Skeinforge31.displayName, 
				"This is an old version of skeinforge.", Skeinforge31.class));
		
		return list;
	}

	static public String getSelectedName() {
		String name = Base.preferences.get("replicatorg.generator.name", "Skeinforge (50)");
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
