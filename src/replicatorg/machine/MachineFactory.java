/*
 MachineFactory.java

 Load and instantiate Machine objects.

 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

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

package replicatorg.machine;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import replicatorg.app.Base;

public class MachineFactory {
	// private constructor: static access only!!!
	private MachineFactory() {
		// this prevents even the native class from
		// calling this ctor as well :
		throw new AssertionError();
	}

	/**
	 * If possible, create a machine controller for the specified device.
	 * @param name The name of the machine descriptor in one of the machine XML files.
	 * @return the machine controller, or null if no descriptor with the given name could be found.
	 */
	public static Machine load(String name, MachineCallbackHandler callbackHandler) {
		Node machineNode = getMachineNode(name);
		if (machineNode == null) { 
			Base.logger.log(Level.SEVERE, "Could not load machine '" + name + "' no machineNode found");
			return null; 
		}
		return new Machine(machineNode, callbackHandler);
	}

	public static Machine loadSimulator() {
		return load("3-Axis Simulator", new MachineCallbackHandler());
	}
	
	public static Vector<String> getMachineNames() {
		Vector<String> v = new Vector<String>();
		boolean showExperimental = 
			Base.preferences.getBoolean("machine.showExperimental", false);
		MachineMap mm = getMachineMap();
		for (Entry<String, Element> entry : mm.entrySet()) {
			// filter out experimental machines of needed
			if (!showExperimental) {
				String exp = entry.getValue().getAttribute("experimental");
				if (exp.length() != 0 && !exp.equals("0")) {
					continue;
				}
			}
			v.add(entry.getKey());
		}
		Collections.sort(v);
		return v;
	}

	static class MachineMap extends HashMap<String,Element> {
	};
	
	private static MachineMap machineMap = null;
	
	private static MachineMap getMachineMap() {
		if (machineMap == null) {
			machineMap = loadMachinesConfig();
		}
		return machineMap;
	}
	
	// look for machine configuration node.
	public static Node getMachineNode(String name) {
		MachineMap mm = getMachineMap();
		if (mm.containsKey(name)) {
			return mm.get(name);
		}
		return null;
	}
	
	/** Load all machine descriptors from a single DOM object.
	 * @see loadMachinesConfig()
	 * @param dom The parsed XML to scan
	 * @param machineMap The map to add entries to
	 */
	private static void addMachinesForDocument(Document dom, MachineMap map) {
		// get each machine
		NodeList nl = dom.getElementsByTagName("machine");
		for (int i = 0; i < nl.getLength(); i++) {
			Element e = (Element)nl.item(i);
			NodeList names = e.getElementsByTagName("name");
			if (names != null && names.getLength() > 0) {
				String mname = names.item(0).getTextContent().trim();
				Base.logger.log(Level.FINE,"Adding machine "+mname+" for node "+e.toString());
				map.put(mname,e);
			}
		}
	}
	
	/** Load all machine descriptors from a single directory.
	 * @see loadMachinesConfig()
	 * @param dir The directory to scan
	 * @param machineMap The map to add entries to
	 * @param db A documentbuilder object for parsing
	 */
	private static void addMachinesForDirectory(File dir, MachineMap machineMap,DocumentBuilder db) {
		try {
			db.reset(); // Allow reuse of a single DocumentBuilder.
		} catch (UnsupportedOperationException uoe) {
			// In case they've got a rogue xerces. :(
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				Base.logger.log(Level.SEVERE, "Could not create document builder", e);
			}
		}
		List<String> filenames = Arrays.asList(dir.list());
		Collections.sort(filenames); // Files addressed in alphabetical order.
		for (String filename : filenames) {
			if (!filename.endsWith(".xml") && !filename.endsWith(".XML")) {
				continue; // Skip anything with an improper extension
			}
			File f = new File(dir,filename);
			if (f.exists() && f.isFile()) {
				Base.logger.log(Level.FINE,"Scanning file "+filename);
				try {
					Document d = db.parse(f);
					addMachinesForDocument(d,machineMap);
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/** Load all the machine descriptors from XML.  Machine descriptors are looked for in:
	 * <ol>
	 *  <li>The "machines" directory under the ReplicatorG install directory</li>
	 *  <li>The "~/.replicatorg/machines" directory</li>
	 * </ol>
	 * Any files with an .xml extension in these directories will be scanned for machine
	 * descriptors.  Files are scanned in alphabetical order within each directory. If two
	 * machine descriptors have the same name, the latest-scanned one appears in the machine
	 * map.
	 * @return a map of strings to XML Node objects.
	 */
	private static MachineMap loadMachinesConfig() {
		// Create the machine configuration map
		MachineMap machineMap = new MachineMap();
		try { // Catch unlikely parser configuration exception.
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			File f = Base.getApplicationFile("machines");
			if (f.exists() && f.isDirectory()) {
				addMachinesForDirectory(f, machineMap, db);
			}
			f = Base.getUserFile("machines", false);
			if (f.exists() && f.isDirectory()) {
				addMachinesForDirectory(f, machineMap, db);
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return machineMap;
	}

}
