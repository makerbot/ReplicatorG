package replicatorg.app;

import java.io.File;
import java.lang.reflect.Method;

import com.apple.mrj.MRJFileUtils;
import com.apple.mrj.MRJOSType;
import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryKey;

/**
 * We're switching to the standard Java Preferences interface.  This code is for loading in legacy
 * preferences.
 * @author phooky
 *
 */
public class LegacyPrefs {


	static final short kUserDomain = -32763;

	static public File getSettingsFolder() {
		File dataFolder = null;

		String pref = OldPreferences.get("settings.path");
		System.err.println("****** S.P: " + pref);
		if (pref != null) {
			dataFolder = new File(pref);

		} else if (Base.isMacOS()) {
			// carbon folder constants
			// http://developer.apple.com/documentation/Carbon/Reference
			// /Folder_Manager/folder_manager_ref/constant_6.html#/
			// /apple_ref/doc/uid/TP30000238/C006889

			// additional information found int the local file:
			// /System/Library/Frameworks/CoreServices.framework
			// /Versions/Current/Frameworks/CarbonCore.framework/Headers/

			// this is the 1.4 version.. but using 1.3 since i have the stubs
			// import com.apple.eio.*
			// println(FileManager.findFolder(kUserDomain,
			// kDomainLibraryFolderType));

			// not clear if i can write to this folder tho..
			try {
				// this method has to be dynamically loaded, because
				MRJOSType domainLibrary = new MRJOSType("dlib");
				Method findFolderMethod = MRJFileUtils.class.getMethod(
						"findFolder",
						new Class[] { Short.TYPE, MRJOSType.class });
				File libraryFolder = (File) findFolderMethod.invoke(null,
						new Object[] { new Short(kUserDomain), domainLibrary });

				dataFolder = new File(libraryFolder, "ReplicatorG");

			} catch (Exception e) {
				// this could be FileNotFound or NoSuchMethod
				// } catch (FileNotFoundException e) {
				// e.printStackTrace();
				// System.exit(1);
				Base.showError("Problem getting data folder",
						"Error getting the ReplicatorG data folder.", e);
			}

		} else if (Base.isWindows()) {
			// looking for Documents and Settings/blah/Application
			// Data/ReplicatorG

			// this is just based on the other documentation, and eyeballing
			// that part of the registry.. not confirmed by any msft/msdn docs.
			// HKEY_CURRENT_USER\Software\Microsoft
			// \Windows\CurrentVersion\Explorer\Shell Folders
			// Value Name: AppData
			// Value Type: REG_SZ
			// Value Data: path

			try {
				// RegistryKey topKey = Registry.getTopLevelKey("HKCU");
				RegistryKey topKey = Registry.HKEY_CURRENT_USER;

				String localKeyPath = "Software\\Microsoft\\Windows\\CurrentVersion"
						+ "\\Explorer\\Shell Folders";
				RegistryKey localKey = topKey.openSubKey(localKeyPath);
				String appDataPath = Base.cleanKey(localKey
						.getStringValue("AppData"));
				// System.out.println("app data path is " + appDataPath);
				// System.exit(0);
				// topKey.closeKey(); // necessary?
				// localKey.closeKey();

				dataFolder = new File(appDataPath, "ReplicatorG");

			} catch (Exception e) {
				Base.showError("Problem getting data folder",
						"Error getting the ReplicatorG data folder.", e);
			}
			// return null;

		} else {
			// otherwise make a .replicatorg directory int the user's home dir
			File home = new File(System.getProperty("user.home"));
			dataFolder = new File(home, ".replicatorg");
		}

		// create the folder if it doesn't exist already
		boolean result = true;
		if (dataFolder != null && !dataFolder.exists()) {
			result = dataFolder.mkdirs();
		}

		if (!result) {
			// try the fallback location
			System.out.println("Using fallback path for settings.");
			String fallback = OldPreferences.get("settings.path.fallback");
			dataFolder = new File(fallback);
			if (!dataFolder.exists()) {
				result = dataFolder.mkdirs();
			}
		}

		if (!result) {
			Base.showError("Settings issues",
					"ReplicatorG cannot run because it could not\n"
							+ "create a folder to store your settings.", null);
		}

		return dataFolder;
	}

	static public File getSettingsFile(String filename) {
		return new File(getSettingsFolder(), filename);
	}


}
