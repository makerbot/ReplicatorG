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

package replicatorg.app;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MachineFactory {
	// private constructor: static access only!!!
	private MachineFactory() {
		// this prevents even the native class from
		// calling this ctor as well :
		throw new AssertionError();
	}

	public static MachineController load(String name) {
		return new MachineController(getMachineNode(name));
	}

	public static MachineController loadSimulator() {
		return load("3-Axis Simulator");
	}
	
	public static Vector<String> getMachineNames() {
		Vector<String> v = new Vector<String>();
		
		NodeList nl = MachineFactory.loadMachinesConfig()
				.getElementsByTagName("machine");
		for (int i = 0; i < nl.getLength(); i++) {
			// look up each machines set of kids
			Node n = nl.item(i);
			NodeList kids = n.getChildNodes();
			for (int j = 0; j < kids.getLength(); j++) {
				Node kid = kids.item(j);
				if (kid.getNodeName().equals("name")) {
					String machineName = kid.getFirstChild().getNodeValue()
							.trim();
					v.add(machineName);
				}
			}
		}
		return v;
	}

	// look for machine configuration node.
	private static Node getMachineNode(String name) {
		// load config...
		Document dom = loadMachinesConfig();

		if (dom == null) {
			Base.showError(null, "Error parsing machines.xml", null);
			return null;
		}

		// get each machine
		NodeList nl = dom.getElementsByTagName("machine");

		for (int i = 0; i < nl.getLength(); i++) {
			// look up each machine's set of kids
			Node n = nl.item(i);
			NodeList kids = n.getChildNodes();

			for (int j = 0; j < kids.getLength(); j++) {
				Node kid = kids.item(j);

				if (kid.getNodeName().equals("name")) {
					String machineName = kid.getFirstChild().getNodeValue()
							.trim();

					if (machineName.equals(name))
						return n;
				}
			}
		}

		// fail with our dom.
		return dom.getFirstChild();
	}

	/** We only need to warn the user if we're loading the machines.xml.dist file once. */
	static boolean usingDistXmlWarned = false; 

	/** Attempt to load the machines.xml file.  If it's not found, attempt to load the machines.xml.dist
	 * file.
	 * @return a Document object representing the config, or null if unable to load the document.
	 */
	private static Document loadMachinesConfig() {
		// attempt to load our xml document.
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			try {
				File f = new File("machines.xml");
				if (!f.exists()) {
					f = new File("machines.xml.dist");
					if (f.exists()) {
						if (!usingDistXmlWarned) {
							Base.showMessage(
											"Machines.xml Not Found",
											"The machine description file 'machines.xml' was not found.\n"
													+ "Falling back to using 'machines.xml.dist' instead.");
							usingDistXmlWarned = true;
						}
					} else {
						Base.showError(
										"Machines.xml Not Found",
										"The machine description file 'machines.xml' was not found.\n"
												+ "Make sure you're running ReplicatorG from the correct directory.",
										null);
						return null;
					}
				}
				try {
					return db.parse(f);
				} catch (SAXException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
				Base.showError(null, "Could not read machines.xml.\n"
						+ "You'll need to reinstall ReplicatorG.", e);
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		return null;
	}

}
