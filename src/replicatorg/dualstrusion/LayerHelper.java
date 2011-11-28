package replicatorg.dualstrusion;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import replicatorg.app.Base;
import replicatorg.machine.model.WipeModel;

/**
 * 
 * @author Noah Levy, Ben Rockhold, Will Langford
 * This class does operations related to Layer manipulation, it employs Will and Ben's layer merging logic but is a complete rewrite of Will's code inorder to utilize the Layer object
 *
 */
public class LayerHelper {

	/**
	 * <code>currentToolhead</code> holds a Toolheads enum representing the current Toolhead, this is checked to see whether a toolchange is necessary
	 */
	private Toolheads currentToolhead;
	/**
	 * Holds the ArrayList of layers for the primary gcode
	 */
	private ArrayList<Layer> primaryLayers;
	/**
	 * Holds the ArrayList of layers for the secondary gcode
	 */
	private ArrayList<Layer> secondaryLayers;
	/**
	 * This float represents the maximum difference that two floats can have to be considered equal, it should always be less than the smallest possible layer height
	 */
	private float tolerance = .01f;
	//private  String currentFeedRate; // good default start speed
	/**
	 * This method has all the method calls in order to merge two gcodes, it is the only method that "needs" to be public
	 */
	private boolean mergeSupport, useWipes;
	
	public LayerHelper(ArrayList<String> primary, ArrayList<String> secondary, boolean useWipes, boolean mergeSupport)
	{
		this.primaryLayers = readLayers(primary);
		this.secondaryLayers = readLayers(secondary);
		//System.out.println("primary.size=" + primaryLayers.size() + " secondary.size=" + secondaryLayers.size());
		
		this.useWipes = useWipes;
		this.mergeSupport = mergeSupport;
		
		currentToolhead = Toolheads.PRIMARY;
		
	}
	/**
	 * This method is used to search through an ArrayList of Layers to find a Layer with height x, it is used by to check whether both gcodes have layers at a specified height
	 * @param height find layers within <code>tolerance</code> of this height
	 * @param searchme an ArrayList of layers to search
	 * @return if it finds something returns the layer, else returns null
	 */
	public Layer getByHeight(float height, ArrayList<Layer> searchme)
	{
		for(Layer l : searchme)
		{
			if(Math.abs(l.getHeight() - height) < tolerance/2)
			{
				return l;
			}
		}
		return null;
	}
	/**
	 * Used for debugging
	 * @param a
	 */
	private void seeLayerHeights(ArrayList<Layer> a)
	{
		for(Layer l : a)
		{
			//System.out.println(l.getHeight());
		}
	}
	/**
	 * This method starts to execute a toolChange, very little actually happens here, mostly it just calls Will's toolchange
	 * @param destinationTool a Toolheads enum of the tool to switch to
	 * @param LayerHeight the layer height to return to, important so as not to run into the print toolchanging
	 * @return
	 */
	
	//Why do these methods have different effects?
	//why does the second add getFirstMove()?
	public ArrayList<String> toolChange(Toolheads destinationTool, float LayerHeight)
	{
		 //calls will langfords toolchange
		//System.out.println("destination toolhead " + destinationTool + "curent toolhead " + currentToolhead);

		currentToolhead = destinationTool;

		return completeToolChange(destinationTool, LayerHeight);
	}
	public ArrayList<String> toolChange(Toolheads destinationTool, Layer a)
	{
		////System.out.println("dest " + destinationTool);
		currentToolhead = destinationTool;
		//System.out.println("new dest " + destinationTool);
		ArrayList<String> cmds = new ArrayList<String>();

		cmds.addAll(completeToolChange(destinationTool, a.getHeight())); //calls will langfords toolchange
		cmds.add(getFirstMove(a.getCommands()));
		return cmds;
	}
	/**
	 * This method merges two layers, it does so by iterating through in increments of <code>tolerance</code> and calling getByHeight to see if both gcodes, one gcode, or no gcodes have layers at that height.
	 * It then calls different methods to integrate the gcode in depending on the presence of layers at that height
	 */
	public ArrayList<String> mergeLayers()
	{
		ArrayList<String> merged = new ArrayList<String>();
		float maxHeight1 = primaryLayers.get(primaryLayers.size()-1).getHeight();
		float maxHeight0 = secondaryLayers.get(secondaryLayers.size()-1).getHeight();
		float maxHeight;
		//System.out.println("T0 maxheight: " + maxHeight0 + " T1 maxheight: " + maxHeight1 + "BetterMaxHeight" + maxHeight);
		if(maxHeight0 < maxHeight1)
		{
			maxHeight = maxHeight1;
		}
		else
		{
			maxHeight = maxHeight0;
		}
		//	System.out.println("T0 maxheight: " + maxHeight0 + " T1 maxheight: " + maxHeight1 + "BetterMaxHeight" + maxHeight);
		//merged.addAll(toolChange(currentToolhead, 0.45f));
		merged.addAll(toolChange(Toolheads.PRIMARY, 0.6f)); //insures we start with right offset and nozzles start supaclean

		for(float i = 0; i < maxHeight - .008; i += tolerance)
		{

			//System.out.println("checking " + i);
			Layer a = getByHeight(i, primaryLayers); //primary
			Layer b = getByHeight(i, secondaryLayers);//secondary
			if(a != null || b != null)
			{
				if(mergeSupport)
				{
					System.out.println("mergeSupport is: ON");
					//currentToolhead = Toolheads.Primary; //1=A=Primary=Left
					merged.addAll(toolChange(Toolheads.PRIMARY, i));
					//2=B=Primary=Right
				}
				//System.out.println("non null layers at " + i);
				//DualStrusionWorker.printArrayList(a.getCommands());
				//DualStrusionWorker.printArrayList(b.getCommands());
				//System.out.println("curTool = " + currentToolhead);
				if(a != null && b != null)
				{
					//System.out.println("both real" + i);
					//	System.out.println("this is called");
					merged.addAll(mergeLayer(a,b));
				}
				else if(a != null)
				{
					//System.out.println("a is real b is null" + i);
					//setCurrentFeedRate(b.getCommands());
					merged.addAll(parseLayer(a, Toolheads.PRIMARY));
				}
				else if(b != null)
				{
					//System.out.println("b is real a is null" + i);
					//setCurrentFeedRate(a.getCommands());
					merged.addAll(parseLayer(b, Toolheads.SECONDARY));
				}
			}
		}
		return merged;
	}
	/**
	 * This method is called  to add gcode to the combined gcode if a layer is only present in one of the sources,
	 * basically all it does  is check to see if it needs to toolchange, execute a toolchange if needed, and then add the gcode
	 * @param a layer to be added
	 * @param destTool Tool to add layer with
	 * @return
	 */
	private  String getFirstMove(ArrayList<String> cmds)
	{
		for(String s : cmds)
		{
			if(s.matches("G1.*"))
			{
				if(s.contains("F"))
				{
					int lastf = s.lastIndexOf("F");
					s = s.substring(0, lastf);
				}
				return s + " F1700.0 (added by getFirstMove)";
			}
		}
		return " ";
	}
	private  ArrayList<String> parseLayer(Layer a, Toolheads destTool)
	{
		//	setCurrentFeedRate(a.getCommands());
		ArrayList<String> completeLayer = new ArrayList<String>();
		//System.out.println("curTool " + currentToolhead + " desttool " + destTool + "linenum " + a.getHeight());
		if(destTool == Toolheads.PRIMARY)
		{
			if(currentToolhead == Toolheads.PRIMARY)
			{
				//System.out.println(a.getCommands());
				completeLayer.addAll(a.getCommands());
			}
			else if(currentToolhead == Toolheads.SECONDARY)
			{
				completeLayer.addAll(toolChange(Toolheads.PRIMARY, a));
				completeLayer.addAll(a.getCommands());
			}
		}
		else if(destTool == Toolheads.SECONDARY)
		{
			if(currentToolhead == Toolheads.SECONDARY)
			{
				completeLayer.addAll(a.getCommands());
			}
			else if(currentToolhead == Toolheads.PRIMARY)
			{
				completeLayer.addAll(toolChange(Toolheads.SECONDARY, a));
				completeLayer.addAll(a.getCommands());
			}
		}
		return completeLayer;
	}
	/**
	 * This method reads the arrayList of strings preprocessed in DualStrusionWorker into arrayLists of layers necessary for mergeLayers, it uses Regex
	 * @param readThis source ArrayList of strings
	 * @param dumphere destination ArrayList  of Layers
	 */
	public ArrayList<Layer> readLayers(ArrayList<String> readThis)
	{
		ArrayList<Layer> result = new ArrayList<Layer>();
		
		//System.out.println("readLayers - list size=" + readThis.size());
		
		for(int i = 0; i < readThis.size()-2;  i++)
		{
			if(readThis.get(i).matches("\\(\\<layer\\>.*\\)"))
			{
				//System.out.println("\t" + readThis.get(i));
				float layerHeight = 0;
				try
				{
					layerHeight = Float.parseFloat(readThis.get(i).split(" ")[1]);
				}
				catch(NumberFormatException e)
				{
					Base.logger.log(Level.SEVERE, "one of your layer heights was unparseable, " +
							"please check and make sure all of them are in the format (<layer> 0.00)");
				}

				int a = i + 1;
				while(true)
				{
					if(readThis.get(a).equalsIgnoreCase("(</layer>)"))
					{
						ArrayList<String> tempList = new ArrayList<String>(readThis.subList(i, a+1));
						result.add(new Layer(layerHeight, tempList)); 
						break;
					}
					a++;
				}
			}
			else
			{
				//System.out.println(gcode.get(i) + " does not ");
			}
		}
		//System.out.println("readLayers - result size=" + result.size());
		return result;
	}
	/**
	 * This method is called when layers exist at the same height in both gcodes, it determines the current toolhead, prints that layer first, executes a toolchange, and then prints the second layer
	 * This is different from will's code in that the two layers are actually merged and not just put at slightly different heights
	 * @param a primary layer
	 * @param b secodnary layer
	 * @return
	 */
	private ArrayList<String> mergeLayer(Layer a, Layer b) //This method makes it so that you dont switch toolheads unnessecarily a is primary layers b is secondary layers
	{
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		DecimalFormat nf = new DecimalFormat();
		nf.setDecimalFormatSymbols(dfs);
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		ArrayList<String> cmds = new ArrayList<String>();
		//System.out.println(a.getHeight());
		cmds.add("(<layer> " + nf.format(a.getHeight()) + " )");
		if(currentToolhead == Toolheads.PRIMARY)
		{
			//setCurrentFeedRate(b.getCommands());
			cmds.addAll(a.getCommandsWithoutLayerTag());
			cmds.addAll(toolChange(Toolheads.SECONDARY, a));
			cmds.addAll(b.getCommandsWithoutLayerTag());
		}
		else if(currentToolhead == Toolheads.SECONDARY)
		{
			//setCurrentFeedRate(a.getCommands());
			cmds.addAll(b.getCommandsWithoutLayerTag());
			cmds.addAll(toolChange(Toolheads.PRIMARY, b));
			cmds.addAll(a.getCommandsWithoutLayerTag());
		}
		cmds.add("(</layer>)");
		//Layer l = new Layer(a.getHeight(),cmds);
		return cmds;
	}
	/**
	 * This method is ported from Will's processing script, it was modified to reduce full reversal and eliminate partial reversal in order to reflect the MK7's tendency to pull its own filament out
	 * @param currentToolnum 0 if secondary 1 if primary
	 * @param nextToolnum 0 if secondary 1 if primary
	 * @param layer_height the layer height that the toolchange must start and end yet, this gives us the flexibility  to avoid smashing into the print
	 * @return
	 */
	public ArrayList<String> wipe(int currentToolnum, int nextToolnum, float layer_height) {

		WipeModel tool0Wipes = Base.getMachineLoader().getMachine().getModel().getWipeByIndex(0);
		WipeModel tool1Wipes = Base.getMachineLoader().getMachine().getModel().getWipeByIndex(1);
		
		if(tool0Wipes == null || tool1Wipes == null)
		{			
			String error = "Could not find wipes for the current machine," + 
				Base.getMachineLoader().getMachine().getModel().toString() + "please select a dualstrusion machine in the drivers menu.";
			JOptionPane.showConfirmDialog(null, error, 
					"Could not prepare Dualstrusion!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
		}
		
		ArrayList<String> result = new ArrayList<String>();
		
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		DecimalFormat nf = new DecimalFormat();
		nf.setDecimalFormatSymbols(dfs);
		
		nf.setMinimumFractionDigits(0); //Min no decimals
		nf.setMaximumFractionDigits(2); //Max 2 decimal placesa
		nf.setGroupingUsed(false); //NO commas!
		
		// define constants/parameters
		// note: the array position corresponds to toolhead number (ie. purge_x[0] is the x purge location for toolhead 0)
		String[] purge_x = 			{nf.format(tool0Wipes.getX1()), nf.format(tool1Wipes.getX1())}; 	// purge location with respect to machine coordinates (G53) (pre-wipe)
		String[] purge_y = 			{nf.format(tool0Wipes.getY1()), nf.format(tool1Wipes.getY1())};
		String[] purge_x_offset = 	{nf.format(tool0Wipes.getX2()), nf.format(tool1Wipes.getX2())}; 	// post-wipe purge location
		String[] purge_y_offset = 	{nf.format(tool0Wipes.getY2()), nf.format(tool1Wipes.getY2())}; 	// purge y = purge y offset (no y change)
		String[] purge_z = 			{nf.format(tool0Wipes.getZ1()), nf.format(tool1Wipes.getZ1())};
		String[] purge_z_offset = 	{nf.format(tool0Wipes.getZ2()), nf.format(tool1Wipes.getZ2())};
		String[] wait_time = 		{nf.format(tool0Wipes.getWait()), nf.format(tool1Wipes.getWait())};
		String[] feedrate = 		{"3000.0", "3000.0"};
		String[] flowrate = 		{nf.format(tool0Wipes.getPurgeRPM()), nf.format(tool1Wipes.getPurgeRPM())};  // pushback (purge) flowrate
		String[] full_reversal_flowrate = {nf.format(tool0Wipes.getReverseRPM()), nf.format(tool1Wipes.getReverseRPM())};  // reversal flowrate for stopping extrusion
//				 hop_height = 		{"3.0", "3.0"}, // nozzle lift before returning to layer
//				 partial_reversal_flowrate = {2.0f, 2.0f}, // reversal flowrate for temporarily stopping extrusion

		
		
		int[]   purge_duration = 			{tool0Wipes.getPurgeDuration(), tool1Wipes.getPurgeDuration()};  // durations (in msec)
		int[]   full_reversal_duration = 	{tool0Wipes.getReverseDuration(), tool1Wipes.getReverseDuration()}; //TWEAK THIS!!!!
//				partial_reversal_duration = {7, 7};

		// reverse current toolhead
		result.add("M108 R"+full_reversal_flowrate[nextToolnum]);
		//
		result.add("M102");
		//
		result.add("G04 P"+full_reversal_duration[nextToolnum]);
		result.add("M103");
		result.add("M108 R"+flowrate[nextToolnum]);

		if(useWipes)
		{
			// move to purge home
			result.add("G53");
			if (layer_height > Float.parseFloat(purge_z[nextToolnum])) {
				// if we're higher than the purge height go over and then down
				result.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
				result.add("G1 X" + purge_x[nextToolnum] +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
				result.add("G1 Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			} 
			else 
			{
				// otherwise go up and then over
				//targetCode.add("G1 Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
				result.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
				result.add("G1 X" + purge_x[nextToolnum] + " Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			}
			// purge upcoming nozzle
			result.add("M103 T"+nextToolnum);
			result.add("G1 X" + purge_x[nextToolnum] +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			result.add("M101");
			result.add("G04 P"+purge_duration[nextToolnum]);
			//targetCode.add("M108 R"+partial_reversal_flowrate[nextToolnum]);
			result.add("M102");
			//targetCode.add("G04 P"+partial_reversal_duration[nextToolnum]);
			result.add("M103");
			result.add("M108 R"+flowrate[nextToolnum]);
			result.add("G04 P" +wait_time[nextToolnum]);
			// wipe upcoming nozzle
			result.add("G1 X" + purge_x_offset[nextToolnum] +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			result.add("G1 X" + purge_x_offset[nextToolnum] +" Y" + purge_y_offset[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			// wipe current nozzle
			result.add("G1 X" + purge_x[currentToolnum] +" Y" + purge_y[currentToolnum] + " Z" + purge_z[currentToolnum] + " F" + feedrate[currentToolnum]);
			result.add("G1 X" + purge_x_offset[currentToolnum] +" Y" + purge_y[currentToolnum] + " Z" + purge_z[currentToolnum] + " F" + feedrate[currentToolnum]);
			result.add("G1 X" + purge_x_offset[currentToolnum] +" Y" + purge_y_offset[currentToolnum] + " Z" + purge_z_offset[currentToolnum] + " F" + feedrate[currentToolnum]);
			// return to purge home
			result.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
		}
		return result;
	}
	/**
	 * This is Will's toolchange method ported, it treats the actual wipe as a seperate method so in the future one could reduce the number of wipes by only making them happen every x number of toolchanges etc
	 * @param nextTool this is the Tool we would like to change into
	 * @param layer_height this is the layer height to do it at
	 * @return
	 */
	public  ArrayList<String> completeToolChange(Toolheads nextTool, float layer_height) {
		ArrayList<String> targetCode = new ArrayList<String>();
		Toolheads currentTool = null;

		if (nextTool == Toolheads.PRIMARY) { 
			currentTool = Toolheads.SECONDARY;
		} else if (nextTool == Toolheads.SECONDARY) { 
			currentTool = Toolheads.PRIMARY;
		}
		int nextToolnum = nextTool.number;
		int currentToolnum = currentTool.number;
		float hop_height = 7.0f;
		targetCode.add("(<toolchange>)");
		float purge_z = 6.5f;

		targetCode.add("M103");

		targetCode.add("G1 Z" + (layer_height+hop_height));
		targetCode.addAll(wipe(currentToolnum, nextToolnum,  layer_height));

		targetCode.add("M103");
//		targetCode.add("M18"); //Added Ben's M18
		targetCode.add("G5"+(5-nextToolnum));
		//	targetCode.add("M108 R"+currentFeedRate);

		// after the toolchange, go to the next position
		/*
		if (layer_height+hop_height < purge_z) {
			float h = layer_height+hop_height;
			targetCode.add("G1 Z"+ h +" F2000");
		} 
		else {
			float h = layer_height+hop_height;
			targetCode.add("G1 Z" + h + " F2000");
			targetCode.add(getNextPos(line_num,gcode).split("Z")[0]+"Z"+ h +" F2000");
		}
		 */
		float h = layer_height+hop_height;
		targetCode.add("G1 Z"+ h +" F2000");

		targetCode.add("(</toolchange>)");
		//System.out.println(currentFeedRate);
		//targetCode.add(currentFeedRate);
		return targetCode;
	}

}
