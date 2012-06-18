package replicatorg.plugin.toolpath.slic3r;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.swing.ListModel;

import replicatorg.app.Base;
import replicatorg.plugin.toolpath.slic3r.Slic3rGenerator.Profile;

/**
 * Helper utilities for profiles.
 * 
 * @author MarkusK
 *
 */
public class ProfileUtils {
	
	public ProfileUtils() {
	}
	
	/** delete the profile, aka the directory which is represented by this profile.
	 * 
	 * @param p the profile
	 * @return true on success
	 */
	public boolean delete(Slic3rGenerator.Profile p) {
		return delete(new File(p.getFullPath()));
		
	}

	private boolean delete(File file) {
		boolean result = true;
		if (file.exists()) {
			if (file.isDirectory()) {
				try {
					if (!file.getAbsolutePath().equals(file.getCanonicalPath())) {
						// This is probably a symbolic link.  Do not follow.
						return false;
					}
				} catch (IOException ioe) {
					return false;
				}

				for (File f : file.listFiles()) {
					result = result && delete(f);
				}
			}
			return result && file.delete();
		}
		else {
			return false;
		}
	}
	public boolean openFolder(Slic3rGenerator.Profile p) {
		return openFolder(new File(p.getFullPath()));
		
	}

	private boolean openFolder(File file) {
		if(Base.openFolderAvailable() != true) 
		{
				Base.logger.info("The profile can be found in this directory: \""+file+"\".");
				return false;
		}
		if (file.exists()) {
			if (file.isDirectory()) {
				Base.logger.fine("The profile can be found in this directory: "+file);
				try {
					Base.openFolder(file);
				} finally {
					
				}
				return true;
			}
		}
		else {
			return false;
		}
		return false;
	}

	//used to check if we want to display a specific profile, based on selected machine, etc.
	public static boolean shouldDisplay(Slic3rGenerator.Profile p) {

		String selectedMachine = Base.preferences.get("machine.name", "no machine selected");
		
		if("no machine selected".equals(selectedMachine) ||
			p.getTargetMachines().isEmpty()  || // if the profile specifies no targets
			p.getTargetMachines().contains(selectedMachine)) // if the profile targets the selected machine
			return true;
		return false;
	}
	public static Profile getListedProfile(ListModel model, Collection<Profile> profiles, int idx) {
		String selected = (String)model.getElementAt(idx);
		for(Profile p : profiles)
			if(selected.equals(p.toString()))
				return p;
		Base.logger.severe("Could not find profile! The programmer has done something foolish.");
		return null;
	}
}
