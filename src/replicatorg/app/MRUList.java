package replicatorg.app;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.prefs.Preferences;

/**
 * The Most-Recently-Used list of file paths.  Every time a file is explicitly opened by the user, it should
 * be placed at the front of the MRU list.  If the file previously appeared in the list, the old entry is
 * removed; otherwise the list grows.  When the list size is exceeded, the last entry is removed.  The list
 * is rewritten to the preferences on every update.
 * 
 * Due to limitations in the length of parameter values, the number of paths stored may be smaller than
 * the maximum number of entries.
 * @author phooky
 *
 */
public class MRUList implements Iterable<String> {

	// Preference key name
	final static String MRU_LIST_KEY = "mru_list";
	
	// List of most recently opened files names.
	private LinkedList<String> filePaths;
	
	private static MRUList singleton = null;
	
	public static MRUList getMRUList() {
		if (singleton == null) {
			singleton = new MRUList();
		}
		return singleton;
	}
	
	private MRUList() {
		String mruString = Base.preferences.get(MRU_LIST_KEY,null);
		filePaths = new LinkedList<String>();
		// Deserialize preference
		if (mruString != null && mruString.length() != 0) {
			for (String entry : mruString.split(",")) {
				if (new File(entry).exists()) {
					filePaths.addLast(entry);
				}
			}
		}
	}
	
	private void writeToPreferences() {
		// Truncate the string at MAX_VALUE_LENGTH; long paths hurt the
		// prefs mechanism.  Someday we'll switch to some hacky KEY_1, KEY_2...
		// mechanism to work around this for most paths.
		StringBuffer sb = new StringBuffer();
		int remaining_chars = Preferences.MAX_VALUE_LENGTH;
		for (String s : filePaths) {
			final int len = sb.length();
			if ( (remaining_chars - (len+1)) < 0) {
				break;
			}
			if (len != 0) {
				sb.append(",");
				remaining_chars--;
			}
			sb.append(s);
			remaining_chars -= len;
		}
		Base.preferences.put(MRU_LIST_KEY, sb.toString());		
	}

	public void update(String filePath) {
		filePaths.remove(filePath);
		filePaths.addFirst(filePath);
		writeToPreferences();
	}

	public Iterator<String> iterator() {
		return filePaths.iterator();
	}
}
