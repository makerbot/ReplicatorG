package replicatorg.plugin.toolpath;

import java.io.File;

import replicatorg.plugin.toolpath.SkeinforgeGenerator.Profile;

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
	public boolean delete(Profile p) {
		return delete(new File(p.getFullPath()));
		
	}

	private boolean delete(File file) {
		boolean result = true;
		if (file.exists()) {
			if (file.isDirectory()) {
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

}
