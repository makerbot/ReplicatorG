package replicatorg.app.gcode;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Noah Levy
 * This object is used to represent individual layers in Gcode, this is a revision of Will Langford's design
 */
public class Layer {
	
	/**
	 * <code>commands</code> holds the commands from given layer
	 */
	private List<String> commands;
	/**
	 * <code>height</code> holds the height of the given layer, instantiated to -1 for debugging purposes
	 */
	private double height =  - 1;
	/**
	 * The only constructor for <code>Layer</code> takes a layer height and an List of the commands
	 * @param h height
	 * @param cmds commands
	 */
	public Layer(double h, List<String> cmds)
	{
		commands = cmds;	
		height = h;
	}
	/**
	 * This method returns the Layers commands
	 * @return commands
	 */
	public List<String> getCommands()
	{
		return commands;
	}
	
	/**
	 * This method returns the commands without the layer tags, this is used when merging layers
	 */
	public List<String> getCommandsWithoutLayerTag()
	{	
		List<String> result;
		
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
	public double getHeight()
	{
		return height;
	}

	/**
	 * Debug Code, returns all the strings in this layer, with a \n appended to each
	 */
	@Override
	public String toString()
	{
		String result = "";
		for(String c : commands)
			result += c + '\n';
		return result;
	}

}
