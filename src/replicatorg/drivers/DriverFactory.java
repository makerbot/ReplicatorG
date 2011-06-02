/*
 DriverFactory.java

 Load and instantiate Driver objects.

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

package replicatorg.drivers;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;

public class DriverFactory {
	// private constructor: static access only!!!
	private DriverFactory() {
		// this prevents even the native class from
		// calling this ctor as well :
		throw new AssertionError();
	}

	/**
	 * Create and instantiate the driver class for our particular machine
	 * 
	 * @param String
	 *            name the name of the driver to instantiate
	 * @return Driver a driver object ready for parsing / running gcode
	 */
	public static Driver factory(Node xml) {
		if (xml == null) {
			// create a null driver
			return factory("NullDriver",null);
		}
		// find the "name" attribute first
		if (xml.hasAttributes()) {
			NamedNodeMap map = xml.getAttributes();
			Node attribute = map.getNamedItem("name");
			if (attribute != null) {
				String driverName = attribute.getNodeValue().trim();

				// use our common factory
				return factory(driverName, xml);
			}
		}

		// fail over to "name" element
		if (xml.hasChildNodes()) {
			NodeList kids = xml.getChildNodes();
			for (int j = 0; j < kids.getLength(); j++) {
				Node kid = kids.item(j);

				if (kid.getNodeName().equals("name")) {
					String driverName = kid.getFirstChild().getNodeValue()
							.trim();

					// use our common factory
					return factory(driverName, xml);
				}
			}
		}

		Base.logger.severe("Failing over to null driver.");

		// bail with a fake driver.
		return loadClass("NullDriver");
	}

	// common driver factory.
	public static Driver factory(String driverName, Node xml) {
		if (driverName.equals("serialpassthrough"))
			return loadClass("replicatorg.drivers.SerialPassthroughDriver", xml);
		else if (driverName.equals("sanguino3g"))
			return loadClass("replicatorg.drivers.gen3.Sanguino3GDriver", xml);
		else if (driverName.equals("makerbot4g"))
			return loadClass("replicatorg.drivers.gen3.Makerbot4GDriver", xml);
		else if (driverName.equals("makerbot4ga"))
			return loadClass("replicatorg.drivers.gen3.Makerbot4GAlternateDriver", xml);
		else if (driverName.equals("reprap5d"))
			return loadClass("replicatorg.drivers.reprap.RepRap5DDriver", xml);
		else if (driverName.equals("simpleReprap5d"))
			return loadClass("replicatorg.drivers.reprap.SimpleRepRap5DDriver", xml);
		else if (driverName.equals("null"))
			return loadClass("replicatorg.drivers.NullDriver", xml);
		else if (driverName.equals("virtualprinter"))
			return loadClass("replicatorg.drivers.VirtualPrinter", xml);
		else {
			// Load driver class 
			Driver driver = loadClass(driverName, xml);
			if (driver == null) {
				Base.logger.severe("Driver not found, failing over to 'null'.");
				return loadClass("replicatorg.drivers.NullDriver", xml);
			} else {
				return driver;
			}
		}
	}

	/**
	 * shortcut class to make it easy to load drivers with their XML configs
	 */
	private static Driver loadClass(String className, Node xml) {
		Driver d = loadClass(className);
		if (xml != null) { d.loadXML(xml); }

		return d;
	}

	/**
	 * this class handles creation of the actual class objects.
	 */
	private static Driver loadClass(String driverName) {
		Base.logger.info("Loading driver: " + driverName);

		String className = driverName;

		// thanks to Peter Edworthy for his help with reflection.
		// lets try to load the class in a nice, dynamic fashion!
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> driverClass = loader.loadClass(className);
			if (Driver.class.isAssignableFrom(driverClass)) {
				return (Driver) driverClass.newInstance();
			}
		} catch (ClassNotFoundException e) {
			// the class being loaded cannot be found
			Base.logger.severe("The class " + className + " cannot be found.");
		} catch (IllegalAccessException e) {
			// The class or its nullary constructor is not accessible.
			Base.logger.severe("The null constructor for " + className
					+ " is not accessible.");
		} catch (InstantiationException e) {
			// The class being created represents an abstract class,
			// an interface, an array class, a primitive type, or void;
			// or if the class has no nullary constructor;
			// or if the instantiation fails for some other reason.
			Base.logger.severe("Initialization of " + className + " failed.");
		} catch (ExceptionInInitializerError e) {
			// The static initialization of the class failed.
			Base.logger.severe("Initialization of " + className + " failed.");
		} catch (SecurityException e) {
			// if there is no permission to create a new instance.
			Base.logger.severe("Permission to create " + className + " denied.");
		}

		return null;
	}
}
