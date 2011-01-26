package replicatorg.plugin.toolpath.skeinforge;

import java.io.File;
import java.io.IOException;

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
	public boolean delete(SkeinforgeGenerator.Profile p) {
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

}
