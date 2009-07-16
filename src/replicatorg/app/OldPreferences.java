/*
 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 Forked from Arduino: http://www.arduino.cc

 Based on Processing http://www.processing.org
 Copyright (c) 2004-05 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package replicatorg.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import replicatorg.app.syntax.SyntaxStyle;

// TODO change this to use the Java Preferences API
// http://www.onjava.com/pub/a/onjava/synd/2001/10/17/j2se.html
// http://www.particle.kth.se/~lindsey/JavaCourse/Book/Part1/Java/Chapter10/Preferences.html

/**
 * 
 */
public class OldPreferences {

	// what to call the feller

	static final String PREFS_FILE = "preferences.txt";

	/*
	 * // remove this for 0121, because quaqua takes care of it static { if
	 * (Base.isMacOS()) BUTTON_HEIGHT = 29; }
	 */

	// value for the size bars, buttons, etc
	static public final int GRID_SIZE = 33;



	// data model
	// we have multiple preference files, one main one and a few subsidiary
	// ones with prefixes. the preferences from the main file go in table
	// and are saved back to the main file. the preferences from the
	// subsidiary files are stored in prefixes (which maps a prefix string to
	// a Hashtable mapping unprefixed keys to values) and are not saved.
	static Hashtable<String, String> table = new Hashtable<String, String>();

	static Hashtable<String, Hashtable<String, String>> prefixes = new Hashtable<String, Hashtable<String, String>>();

	static public File preferencesFile;

	static public void init() {

		// start by loading the defaults, in case something
		// important was deleted from the user prefs

		try {
			load(Base.getStream(PREFS_FILE));

		} catch (Exception e) {
			Base.showError(null, "Could not read default settings.\n"
					+ "You'll need to reinstall ReplicatorG.", e);
		}

		// check for platform-specific properties in the defaults

		String platformExtension = "."
				+ Base.platform.name().toLowerCase();
		
		int extensionLength = platformExtension.length();

		Enumeration e = table.keys(); // properties.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			if (key.endsWith(platformExtension)) {
				// this is a key specific to a particular platform
				String actualKey = key.substring(0, key.length()
						- extensionLength);
				String value = get(key);
				table.put(actualKey, value);
			}
		}

		// other things that have to be set explicitly for the defaults

		setColor("run.window.bgcolor", SystemColor.control);

		// next load user preferences file

		preferencesFile = LegacyPrefs.getSettingsFile(PREFS_FILE);

		if (!preferencesFile.exists()) {
			// create a new preferences file if none exists
			// saves the defaults out to the file
			save();

		} else {
			// load the previous preferences file

			try {
				load(new FileInputStream(preferencesFile));

			} catch (Exception ex) {
				Base.showError("Error reading preferences",
						"Error reading the preferences file. "
								+ "Please delete (or move)\n"
								+ preferencesFile.getAbsolutePath()
								+ " and restart ReplicatorG.", ex);
			}
		}
	}

	// .................................................................
	// .................................................................

	static public void load(InputStream input) throws IOException {
		load(input, null);
	}

	static public void load(InputStream input, String prefix)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		Hashtable<String, String> table = OldPreferences.table;

		if (prefix != null) {
			table = new Hashtable<String, String>();
			prefixes.put(prefix, table);
		}

		// table = new Hashtable();
		String line = null;
		while ((line = reader.readLine()) != null) {
			if ((line.length() == 0) || (line.charAt(0) == '#'))
				continue;

			// this won't properly handle = signs being in the text
			int equals = line.indexOf('=');
			if (equals != -1) {
				String key = line.substring(0, equals).trim();
				String value = line.substring(equals + 1).trim();
				table.put(key, value);
			}
		}
		reader.close();
	}

	// .................................................................

	static public void save() {
		try {
			FileOutputStream output = new FileOutputStream(preferencesFile);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));

			Enumeration e = table.keys(); // properties.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				writer.println(key + "=" + ((String) table.get(key)));
			}

			writer.flush();
			writer.close();

		} catch (IOException ex) {
			Base.showWarning(null, "Error while saving the settings file", ex);
			// e.printStackTrace();
		}
	}

	// .................................................................

	// all the information from preferences.txt

	// static public String get(String attribute) {
	// return get(attribute, null);
	// }

	static public String get(String attribute /* , String defaultValue */) {
		// if the attribute starts with a prefix used by one of our subsidiary
		// preference files, look up the attribute in that file's Hashtable
		// (don't override with or fallback to the main file). otherwise,
		// look up the attribute in the main file's Hashtable.
		Hashtable table = OldPreferences.table;
		if (attribute.indexOf('.') != -1) {
			String prefix = attribute.substring(0, attribute.indexOf('.'));
			if (prefixes.containsKey(prefix)) {
				table = (Hashtable) prefixes.get(prefix);
				attribute = attribute.substring(attribute.indexOf('.') + 1);
			}
		}
		return (String) table.get(attribute);
	}

	/**
	 * Get the top-level key prefixes defined in the subsidiary file loaded with
	 * the given prefix. For example, if the file contains: foo.count=1
	 * bar.count=2 baz.count=3 this will return { "foo", "bar", "baz" }.
	 */
	static public Iterator<String> getSubKeys(String prefix) {
		if (!prefixes.containsKey(prefix))
			return null;
		Set<String> subkeys = new HashSet<String>();
		for (Enumeration e = (prefixes.get(prefix)).keys(); e.hasMoreElements();) {
			String subkey = (String) e.nextElement();
			if (subkey.indexOf('.') != -1)
				subkey = subkey.substring(0, subkey.indexOf('.'));
			subkeys.add(subkey);
		}
		return subkeys.iterator();
	}

	static public void set(String attribute, String value) {
		// preferences.put(attribute, value);
		table.put(attribute, value);
	}

	static public boolean getBoolean(String attribute) {
		String value = get(attribute); // , null);
		return (new Boolean(value)).booleanValue();
	}

	static public void setBoolean(String attribute, boolean value) {
		set(attribute, value ? "true" : "false");
	}

	static public int getInteger(String attribute /* , int defaultValue */) {
		return Integer.parseInt(get(attribute));
	}

	static public void setInteger(String key, int value) {
		set(key, String.valueOf(value));
	}

	static public Color getColor(String name /* , Color otherwise */) {
		Color parsed = null;
		String s = get(name); // , null);
		// System.out.println(name + " = " + s);
		if ((s != null) && (s.indexOf("#") == 0)) {
			try {
				int v = Integer.parseInt(s.substring(1), 16);
				parsed = new Color(v);
			} catch (Exception e) {
			}
		}
		// if (parsed == null) return otherwise;
		return parsed;
	}

	static public void setColor(String attr, Color what) {
		String r = Integer.toHexString(what.getRed());
		String g = Integer.toHexString(what.getGreen());
		String b = Integer.toHexString(what.getBlue());
		set(attr, "#" + r.substring(r.length() - 2)
				+ g.substring(g.length() - 2) + b.substring(b.length() - 2));
	}

	static public Font getFont(String which /* , Font otherwise */) {
		// System.out.println("getting font '" + which + "'");
		String str = get(which);
		// if (str == null) return otherwise; // ENABLE LATER
		StringTokenizer st = new StringTokenizer(str, ",");
		String fontname = st.nextToken();
		String fontstyle = st.nextToken();
		return new Font(fontname,
				((fontstyle.indexOf("bold") != -1) ? Font.BOLD : 0)
						| ((fontstyle.indexOf("italic") != -1) ? Font.ITALIC
								: 0), Integer.parseInt(st.nextToken()));
	}

	static public SyntaxStyle getStyle(String what /* , String dflt */) {
		String str = get("editor." + what + ".style"); // , dflt);

		StringTokenizer st = new StringTokenizer(str, ",");

		String s = st.nextToken();
		if (s.indexOf("#") == 0)
			s = s.substring(1);
		Color color = new Color(Integer.parseInt(s, 16));

		s = st.nextToken();
		boolean bold = (s.indexOf("bold") != -1);
		boolean italic = (s.indexOf("italic") != -1);
		// System.out.println(what + " = " + str + " " + bold + " " + italic);

		return new SyntaxStyle(color, italic, bold);
	}
}
