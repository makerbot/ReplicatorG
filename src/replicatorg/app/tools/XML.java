/*
  XML.java

  XML convenience methods

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

package replicatorg.app.tools;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XML
{
	//private constructor:  static access only!!!
	private XML()
	{
		//this prevents even the native class from 
		//calling this ctor as well :
		throw new AssertionError();
	}
	
	static public boolean hasChildNode(Node node, String name)
	{
		//look through the kids
		NodeList kids = node.getChildNodes();
		for (int j=0; j<kids.getLength(); j++)
		{
			Node kid = kids.item(j);

			//did we find it?
			if (kid.getNodeName().equals(name))
				return true;
		}

		return false;
	}

	static public String getChildNodeValue(Node node, String name)
	{
		//return null if we have nothing.
		if (hasChildNode(node, name))
		{
			//look through the kids.
			NodeList kids = node.getChildNodes();
			for (int j=0; j<kids.getLength(); j++)
			{
				Node kid = kids.item(j);
			
				//did we find it?
				if (kid.getNodeName().equals(name))
					return kid.getFirstChild().getNodeValue().trim();
			}

			return new String();
		}
		else
			return null;
	}
	
	static public Node getChildNodeByName(Node node, String name)
	{
		//return null if we have nothing.
		if (hasChildNode(node, name))
		{
			//look through the kids.
			NodeList kids = node.getChildNodes();
			for (int j=0; j<kids.getLength(); j++)
			{
				Node kid = kids.item(j);
			
				//did we find it?
				if (kid.getNodeName().equals(name))
					return kid;
			}
		}

		//fail.
		return null;
	}
	
	static public String getAttributeValue(Node node, String name)
	{
		if (node.hasAttributes())
		{
			NamedNodeMap map = node.getAttributes();
			Node attribute = map.getNamedItem(name);

			if (attribute != null)
				return attribute.getNodeValue().trim();
		}

		return null;
	}
}
