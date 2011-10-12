package replicatorg.machine.model;
import java.text.NumberFormat;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;
import replicatorg.app.tools.XML;

public class WipeModel {

	private int index;
	private float x1, y1, z1, x2, y2, z2;
	private float wait;
	private int purgeDuration, reverseDuration;
	private float purgeRPM;
	private float reverseRPM;

	Node xml;
	public WipeModel(Node n)
	{
		xml = n;
		loadXML();

	}
	public void loadXML()
	{
		try
		{
			//load our index.
			String n = XML.getAttributeValue(xml, "index");
			if (n != null)
			{
				index = Integer.parseInt(n);
			}
			//load our x1.
			n = XML.getAttributeValue(xml, "X1");
			if (n != null)
			{
				
				x1 = Float.parseFloat(n);
			}
			//load our y1.
			n = XML.getAttributeValue(xml, "Y1");
			if (n != null)
			{
				y1 = Float.parseFloat(n);
			}
			//load our z1
			n = XML.getAttributeValue(xml, "Z1");
			if (n != null)
			{
				z1 = Float.parseFloat(n);
			}
			//load our x2.
			n = XML.getAttributeValue(xml, "X2");
			if (n != null)
			{
				x2 = Float.parseFloat(n);
			}
			//load our y2.
			n = XML.getAttributeValue(xml, "Y2");
			if (n != null)
			{
				y2 = Float.parseFloat(n);
			}
			//load our z2
			n = XML.getAttributeValue(xml, "Z2");
			if (n != null)
			{
				z2 = Float.parseFloat(n);
			}
			n = XML.getAttributeValue(xml, "wait");
			if (n != null)
			{
				wait = Float.parseFloat(n);
			}
			n = XML.getAttributeValue(xml, "purge_duration");
			if (n != null)
			{
				purgeDuration = (int)Float.parseFloat(n);
			}
			n = XML.getAttributeValue(xml, "reverse_duration");
			if (n != null)
			{
				reverseDuration = (int)Float.parseFloat(n);
			}
			n = XML.getAttributeValue(xml, "purge_rpm");
			if (n != null)
			{
				purgeRPM = Float.parseFloat(n);
			}
			n = XML.getAttributeValue(xml, "reverse_rpm");
			if (n != null)
			{
				reverseRPM = Float.parseFloat(n);
			}

		}
		catch(NumberFormatException e)
		{
			Base.logger.severe("Could not parse your xml wipe, please make sure everything is formatted correctly");

			System.err.println("Could not parse your xml wipe, please make sure everything is formatted correctly");
			e.printStackTrace();
		}
	}
	public int getIndex()
	{
		return index;
	}
	public float getX1()
	{
		return x1;
	}
	public float getX2()
	{
		return x2;
	}
	public float getY1()
	{
		return y1;
	}
	public float getY2()
	{
		return y2;
	}
	public float getZ1()
	{
		return z1;
	}
	public float getZ2()
	{
		return z2;
	}
	public float getWait()
	{
		return wait;
	}
	public int getPurgeDuration()
	{
		return purgeDuration;
	}
	public int getReverseDuration()
	{
		return reverseDuration;
	}
	public float getPurgeRPM()
	{
		return purgeRPM;
	}
	public float getReverseRPM()
	{
		return reverseRPM;
	}
}