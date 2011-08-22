package replicatorg.dualstrusion;

import java.util.ArrayList;


public class Layer {

	private ArrayList<String> commands = new ArrayList<String>();
	private float height =  - 1;
	public Layer(float h, ArrayList<String> cmds)
	{
		commands = cmds;	
		height = h;
		if(commands != null)
		{
			//System.out.println("has commands");
			//DuaprintArrayList(commands);
		}
		if(commands == null)
		{
			//System.out.println("has  no commands");
		}
	}
	public ArrayList<String> getCommands()
	{
		return commands;
	}
	public ArrayList<String> getCommandsWithoutLayerTag()
	{	
		if(commands.size() >= 3)
		{
		ArrayList<String> tList = new ArrayList<String>(commands.subList(1, commands.size()-2));
		
		//DualStrusionWorker.printArrayList(commands);
		return tList;
		}
		ArrayList<String> blankList = new ArrayList<String>();
		blankList.add("");
		return blankList;
		
	}
	public float getHeight()
	{
		return height;
	}


}
