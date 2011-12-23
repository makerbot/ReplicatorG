package replicatorg.machine.model;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Level;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;
import replicatorg.app.tools.XML;

public class WipeModel {

	private ToolheadAlias tool;
	private double height;
	private String x1, y1, z1, x2, y2, z2;
	private String wait;
	private String purgeDuration, reverseDuration;
	private String purgeRPM, reverseRPM;

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
				int idx = Integer.parseInt(n);
				if(idx == ToolheadAlias.LEFT.number)
					tool = ToolheadAlias.LEFT;
				if(idx == ToolheadAlias.RIGHT.number)
					tool = ToolheadAlias.RIGHT; 
			}
			
			// Load the wipe values, if they're not present use an empty string
			// which will act as a sort of wildcard, leaving the gcode unaffected
			
			//load our x1.
			n = XML.getAttributeValue(xml, "X1");
			if (n != null)
				x1 = "X" + n;
			else
				x1 = "";
			//load our y1.
			n = XML.getAttributeValue(xml, "Y1");
			if (n != null)
				y1 = "Y" + n;
			else
				y1 = "";
			//load our z1
			n = XML.getAttributeValue(xml, "Z1");
			if (n != null)
				z1 = "Z" + n;
			else
				z1 = "";
			height = Base.getGcodeFormat().parse(n).doubleValue();
			
			//load our x2.
			n = XML.getAttributeValue(xml, "X2");
			if (n != null)
				x2 = "X" + n;
			else
				x2 = "";
			//load our y2.
			n = XML.getAttributeValue(xml, "Y2");
			if (n != null)
				y2 = "Y" + n;
			else
				y2 = "";
			//load our z2
			n = XML.getAttributeValue(xml, "Z2");
			if (n != null)
				z2 = "Z" + n;
			else
				z2 = "";
			
			n = XML.getAttributeValue(xml, "wait");
			if (n != null)
				wait = "P" + n;
			else
				wait = "";
			n = XML.getAttributeValue(xml, "purge_duration");
			if (n != null)
				purgeDuration = "P" + n;
			else
				purgeDuration = "";
			n = XML.getAttributeValue(xml, "reverse_duration");
			if (n != null)
				reverseDuration = "P" + n;
			else
				reverseDuration = "";
			n = XML.getAttributeValue(xml, "purge_rpm");
			if (n != null)
				purgeRPM = "R" + n;
			else
				purgeRPM = "";
			n = XML.getAttributeValue(xml, "reverse_rpm");
			if (n != null)
				reverseRPM = "R" + n;
			else
				reverseRPM = "";

		}
		catch(NumberFormatException e)
		{
			Base.logger.log(Level.SEVERE, "Could not parse your xml wipe, please make sure everything is formatted correctly", e);
			e.printStackTrace();
		} catch (ParseException e) {
			Base.logger.log(Level.SEVERE, "Could not parse your xml wipe, please make sure everything is formatted correctly", e);
			e.printStackTrace();
		}
	}
	public ToolheadAlias getTool()
	{
		return tool;
	}
	public String getX1()
	{
		return x1;
	}
	public String getX2()
	{
		return x2;
	}
	public String getY1()
	{
		return y1;
	}
	public String getY2()
	{
		return y2;
	}
	public String getZ1()
	{
		return z1;
	}
	public String getZ2()
	{
		return z2;
	}
	public String getWait()
	{
		return wait;
	}
	public String getPurgeDuration()
	{
		return purgeDuration;
	}
	public String getReverseDuration()
	{
		return reverseDuration;
	}
	public String getPurgeRPM()
	{
		return purgeRPM;
	}
	public String getReverseRPM()
	{
		return reverseRPM;
	}
	public double getHeight()
	{
		return height;
	}
}