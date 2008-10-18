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

package processing.app;

import javax.swing.*;
import java.util.regex.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.*;


public class MachineFactory
{
	//private constructor:  static access only!!!
	private MachineFactory()
	{
		//this prevents even the native class from 
		//calling this ctor as well :
		throw new AssertionError();
	}
	
	public static MachineController load(String name)
	{
		return new MachineController(getMachineNode(name));
	}
	
	public static MachineController loadSimulator()
	{
		return load("3-Axis Simulator");
	}
	
	//look for machine configuration node.
	private static Node getMachineNode(String name)
	{
		//load config...
		Document dom = loadMachinesConfig();
	  
		if (dom == null)
		{
			Base.showError(null, "Error parsing machines.xml", null);
		}
		
		//get each machines
		NodeList nl = dom.getElementsByTagName("machine");

		for (int i=0; i<nl.getLength(); i++)
		{
			//look up each machines set of kids
			Node n = nl.item(i);
			NodeList kids = n.getChildNodes();

			for (int j=0; j<kids.getLength(); j++)
			{
				Node kid = kids.item(j);

				if (kid.getNodeName().equals("name"))
				{
					String machineName = kid.getFirstChild().getNodeValue().trim();

					if (machineName.equals(name))
						return n;
				}
			}
		}
		
		//fail with our dom.
		return dom.getFirstChild();
	}
	
	//why not load it everytime!  no stale configs...
	public static org.w3c.dom.Document loadMachinesConfig()
	{
		//attempt to load our xml document.
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder db = dbf.newDocumentBuilder();
			try
			{
				File f = new File("machines.xml");
				try
				{
					return db.parse(f);
				}
				catch (SAXException e)
				{ 
					e.printStackTrace();
				}
			}
			catch (IOException e)
			{ 
				e.printStackTrace();
			}
		}
		catch (ParserConfigurationException e)
		{
			e.printStackTrace();
		}
		
		return null;
	}

}
