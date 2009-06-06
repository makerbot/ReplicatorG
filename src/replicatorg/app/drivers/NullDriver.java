/*
 NullDriver.java

 This driver does absolutely nothing.

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

package replicatorg.app.drivers;

import org.w3c.dom.Node;

import replicatorg.app.exceptions.GCodeException;
import replicatorg.app.tools.XML;

public class NullDriver extends DriverBaseImplementation {
	private double speedup;

	public NullDriver() {
		super();

		speedup = 10;
	}

	public void loadXML(Node xml) {
		super.loadXML(xml);

		if (XML.hasChildNode(xml, "speedup"))
			speedup = Double.parseDouble(XML.getChildNodeValue(xml, "speedup"));
	}

	public void execute() throws InterruptedException {
		// suppress errors.
		try {
			super.execute();
		} catch (GCodeException e) {
		}

		String command = getParser().getCommand();

		if (command.length() > 0 && speedup > 0) {
			// calculate our delay speed.
			int millis = (int) Math.round(getMoveLength()
					/ getCurrentFeedrate() * 60000.0 / speedup);

			if (millis > 0)
				Thread.sleep(millis);
		}
	}
}
