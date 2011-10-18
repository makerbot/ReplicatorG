package replicatorg.dualstrusion;

import java.util.ArrayList;

/**
 * 
 * @author Noah Levy
 * This object is used to represent individual layers in Gcode, this is a revision of Will Langford's design which had problems because he was unable to use objects
 */
public class Layer {
	
	/**
	 * <code>commands</code> holds the commands from given layer
	 */
	private ArrayList<String> commands = new ArrayList<String>();
	/**
	 * <code>height</code> holds the height of the given layer, instantiated to -1 for debugging purposes
	 */
	private float height =  - 1;
	/**
	 * The only constructor for <code>Layer</code> takes a layer height and an arrayList of the commands
	 * @param h height
	 * @param cmds commands
	 */
	public Layer(float h, ArrayList<String> cmds)
	{
		commands = cmds;	
		height = h;
	}
	/**
	 * This method returns the Layers commands
	 * @return commands
	 */
	public ArrayList<String> getCommands()
	{
		return commands;
	}
	
	/**
	 * This method returns the commands without the layer tags, this is used when merging layers
	 */
	public ArrayList<String> getCommandsWithoutLayerTag()
	{	
		ArrayList<String> result;
		
		//TODO document why < 3 commands doesn't fly
		if(commands.size() < 3)
		{
			result = new ArrayList<String>();
			result.add("");
		}
		else
		{
			result = new ArrayList<String>(commands.subList(1, commands.size()-2)); 
		}
		
		return result;
		
	}
	public float getHeight()
	{
		return height;
	}


}
