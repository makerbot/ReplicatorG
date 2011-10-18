package replicatorg.machine.model;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.tools.XML;

public class ExclusionZoneModel {
	private float[] topleft = new float[3];
	private float[] bottomright = new float[3];
	private String reason;
	Node xml;
	public ExclusionZoneModel(Node n)
	{
		xml = n;
		loadXML();

	}
	public void loadXML()
	{
		try
		{
			//topleft
			String n = XML.getAttributeValue(xml, "topleft");
			if (n != null)
			{
				String[] nsplit = n.split(",");
				for(int i = 0; i < nsplit.length; i++)
				{
					topleft[i] = Float.parseFloat(nsplit[i]);
				}
			}
			n = XML.getAttributeValue(xml, "bottomright");
			if (n != null)
			{
				String[] nsplit = n.split(",");
				for(int i = 0; i < nsplit.length; i++)
				{
					bottomright[i] = Float.parseFloat(nsplit[i]);
				}
			}
			n = XML.getAttributeValue(xml, "reason");
			if (n != null)
			{
			reason = n;	
			}


		}
		catch(NumberFormatException e)
		{
			Base.logger.severe("Could not parse your xml exclusionZone, please make sure everything is formatted correctly");

			System.err.println("Could not parse your xml exclusionZone, please make sure everything is formatted correctly");
			e.printStackTrace();
		}
	}
}
