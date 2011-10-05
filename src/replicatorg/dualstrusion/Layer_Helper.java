package replicatorg.dualstrusion;

import java.text.NumberFormat;
import java.util.ArrayList;

import replicatorg.app.Base;
import replicatorg.machine.model.WipeModel;

/**
 * 
 * @author Noah Levy, Ben Rockhold, Will Langford
 * This class does operations related to Layer manipulation, it employs Will and Ben's layer merging logic but is a complete rewrite of Will's code inorder to utilize the Layer object
 *
 */
public class Layer_Helper {

	/**
	 * <code>currentToolhead</code> holds a Toolheads enum representing the current Toolhead, this is checked to see whether a toolchange is necessary
	 */
	private static Toolheads currentToolhead = Toolheads.Primary;
	/**
	 * Holds the ArrayList of layers for the primary gcode
	 */
	public static ArrayList<Layer> PrimaryLayers = new ArrayList<Layer>();
	/**
	 * Holds the ArrayList of layers for the secondary gcode
	 */
	public static ArrayList<Layer> SecondaryLayers = new ArrayList<Layer>();
	/**
	 * This float represents the maximum difference that two floats can have to be considered equal, it should always be less than the smallest possible layer height
	 */
	private static float tolerance = .01f;
	//private static String currentFeedRate; // good default start speed
	/**
	 * This method has all the method calls in order to merge two gcodes, it is the only method that "needs" to be public
	 */
	private static boolean mergeSupport, useWipes;
	public static ArrayList<String> doMerge(ArrayList<String> prime, ArrayList<String> second, boolean mergeSup, boolean uW)
	{
		useWipes = uW;
		currentToolhead = Toolheads.Primary;
		if(PrimaryLayers != null)
		{
			PrimaryLayers.clear(); //cleanse these arrays just in case

		}
		if(SecondaryLayers != null)
		{
			SecondaryLayers.clear();

		}
		mergeSupport = mergeSup;
		System.out.println("domerge");
		readLayers(prime, PrimaryLayers);
		readLayers(second, SecondaryLayers);
		//seeLayerHeights(SecondaryLayers);
		System.out.println(PrimaryLayers.size() + " " + SecondaryLayers.size());
		return mergeLayers(PrimaryLayers, SecondaryLayers);

	}
	/**
	 * This method is used to search through an ArrayList of Layers to find a Layer with height x, it is used by to check whether both gcodes have layers at a specified height
	 * @param height find layers within <code>tolerance</code> of this height
	 * @param searchme an ArrayList of layers to search
	 * @return if it finds something returns the layer, else returns null
	 */
	public static Layer getByHeight(float height, ArrayList<Layer> searchme)
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
	private static void seeLayerHeights(ArrayList<Layer> a)
	{
		for(Layer l : a)
		{
			System.out.println(l.getHeight());
		}
	}
	/**
	 * This method starts to execute a toolChange, very little actually happens here, mostly it just calls Will's toolchange
	 * @param destinationTool a Toolheads enum of the tool to switch to
	 * @param LayerHeight the layer height to return to, important so as not to run into the print toolchanging
	 * @return
	 */
	public static ArrayList<String> toolChange(Toolheads destinationTool, float LayerHeight)
	{
		System.out.println("dest " + destinationTool + "cur " + currentToolhead);

		currentToolhead = destinationTool;

		return completeToolChange(destinationTool, LayerHeight); //calls will langfords toolchange
	}
	public static ArrayList<String> toolChange(Toolheads destinationTool, Layer a)
	{
		//System.out.println("dest " + destinationTool);
		currentToolhead = destinationTool;
		//System.out.println("new dest " + destinationTool);
		ArrayList<String> cmds = new ArrayList<String>();

		cmds.addAll(completeToolChange(destinationTool, a.getHeight())); //calls will langfords toolchange
		cmds.add(getFirstMove(a.getCommands()));
		return cmds;
	}
	/*
	public static void setCurrentFeedRate(ArrayList<String> commands)
	{
		for(String s : commands)
		{
			if(s.matches("M108.*"))
			{
				System.out.println("hit, new feedrate : " + s);
				currentFeedRate  = s;
			}
		}
	}
	 */
	/**
	 * This method merges two layers, it does so by iterating through in increments of <code>tolerance</code> and calling getByHeight to see if both gcodes, one gcode, or no gcodes have layers at that height.
	 * It then calls different methods to integrate the gcode in depending on the presence of layers at that height
	 */
	public static ArrayList<String> mergeLayers(ArrayList<Layer> primary, ArrayList<Layer> secondary)
	{
		ArrayList<String> merged = new ArrayList<String>();
		float maxHeight1 = primary.get(primary.size()-1).getHeight();
		float maxHeight0 = secondary.get(secondary.size()-1).getHeight();
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
		merged.addAll(toolChange(Toolheads.Primary, 0.6f)); //insures we start with right offset and nozzles start supaclean

		for(float i = 0; i < maxHeight - .008; i += tolerance)
		{

			//System.out.println("checking " + i);
			Layer a = getByHeight(i, primary); //primary
			Layer b = getByHeight(i, secondary);//secondary
			if(a != null || b != null)
			{
				if(mergeSupport)
				{
					System.out.println("mergeSupport is: ON");
					//currentToolhead = Toolheads.Primary; //1=A=Primary=Left
					merged.addAll(toolChange(Toolheads.Primary, i));
					//2=B=Primary=Right
				}
				//System.out.println("non null layers at " + i);
				//DualStrusionWorker.printArrayList(a.getCommands());
				//DualStrusionWorker.printArrayList(b.getCommands());
				System.out.println("curTool = " + currentToolhead);
				if(a != null && b != null)
				{
					System.out.println("both real" + i);
					//	System.out.println("this is called");
					merged.addAll(mergeLayer(a,b));
				}
				else if(a != null)
				{
					System.out.println("a is real b is null" + i);
					//setCurrentFeedRate(b.getCommands());
					merged.addAll(parseLayer(a, Toolheads.Primary));
				}
				else if(b != null)
				{
					System.out.println("b is real a is null" + i);
					//setCurrentFeedRate(a.getCommands());
					merged.addAll(parseLayer(b, Toolheads.Secondary));
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
	private static String getFirstMove(ArrayList<String> cmds)
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
	private static ArrayList<String> parseLayer(Layer a, Toolheads destTool)
	{
		//	setCurrentFeedRate(a.getCommands());
		ArrayList<String> completeLayer = new ArrayList<String>();
		System.out.println("curTool " + currentToolhead + " desttool " + destTool + "linenum " + a.getHeight());
		if(destTool == Toolheads.Primary)
		{
			if(currentToolhead == Toolheads.Primary)
			{
				//System.out.println(a.getCommands());
				completeLayer.addAll(a.getCommands());
			}
			else if(currentToolhead == Toolheads.Secondary)
			{
				completeLayer.addAll(toolChange(Toolheads.Primary, a));
				completeLayer.addAll(a.getCommands());
			}
		}
		else if(destTool == Toolheads.Secondary)
		{
			if(currentToolhead == Toolheads.Secondary)
			{
				completeLayer.addAll(a.getCommands());
			}
			else if(currentToolhead == Toolheads.Primary)
			{
				completeLayer.addAll(toolChange(Toolheads.Secondary, a));
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
	public static void readLayers(ArrayList<String> readThis, ArrayList<Layer> dumphere)
	{
		//for(int i = 0; i < gcode.size()-2;  i++)
		for(int i = 0; i < readThis.size()-2;  i++)
		{
			if(readThis.get(i).matches("\\(\\<layer\\>.*\\)"))
			{
				//System.out.println("matched layer");
				float layerHeight = 0;
				try
				{
					layerHeight = Float.parseFloat(readThis.get(i).split(" ")[1]);
				}
				catch(NumberFormatException e)
				{
					System.err.println("one of your layer heights was unparseable, please check and make sure all of them are in the format (<layer> 0.00)");
				}
				//System.out.println("height" + layerHeight);
				int a = i + 1;
				while(true)
				{
					if(readThis.get(a).equalsIgnoreCase("(</layer>)"))
					{

						//System.out.println("reading in layer at height " + layerHeight);
						ArrayList<String> tempList = new ArrayList<String>(readThis.subList(i, a+1));
						//DualStrusionWorker.printArrayList(tempList);
						Layer l = new Layer(layerHeight, tempList);
						dumphere.add(l); 
						//a++;
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
		//return gcode;
	}
	/**
	 * This method is called when layers exist at the same height in both gcodes, it determines the current toolhead, prints that layer first, executes a toolchange, and then prints the second layer
	 * This is different from will's code in that the two layers are actually merged and not just put at slightly different heights
	 * @param a primary layer
	 * @param b secodnary layer
	 * @return
	 */
	private static ArrayList<String> mergeLayer(Layer a, Layer b) //This method makes it so that you dont switch toolheads unnessecarily a is primary layers b is secondary layers
	{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		ArrayList<String> cmds = new ArrayList<String>();
		//System.out.println(a.getHeight());
		cmds.add("(<layer> " + nf.format(a.getHeight()) + " )");
		if(currentToolhead == Toolheads.Primary)
		{
			//setCurrentFeedRate(b.getCommands());
			cmds.addAll(a.getCommandsWithoutLayerTag());
			cmds.addAll(toolChange(Toolheads.Secondary, a));
			cmds.addAll(b.getCommandsWithoutLayerTag());
		}
		else if(currentToolhead == Toolheads.Secondary)
		{
			//setCurrentFeedRate(a.getCommands());
			cmds.addAll(b.getCommandsWithoutLayerTag());
			cmds.addAll(toolChange(Toolheads.Primary, b));
			cmds.addAll(a.getCommandsWithoutLayerTag());
		}
		cmds.add("(</layer>)");
		//Layer l = new Layer(a.getHeight(),cmds);
		return cmds;
	}
	/**
	 * This method is ported from Will's processing script, it was modified to reduce full reversal and eliminate partial reversal in order to reflect the MK7's tendency to pull its own filament out
	 * This is the method that is passed ints based on Toolheads.ordinal() so please please  please dont change the order of the toolheads.
	 * @param currentToolnum 0 if secondary 1 if primary, changing .ordinal() would change this.
	 * @param nextToolnum 0 if secondary 1 if primary, changing .ordinal() would change this.
	 * @param layer_height the layer height that the toolchange must start and end yet, this gives us the flexibility  to avoid smashing into the print
	 * @return
	 */
	public static ArrayList<String> wipe(int currentToolnum, int nextToolnum, float layer_height) {
		//System.out.println(Base.getMachineLoader().getMachine().getModel().getWipes().size());
		WipeModel tool0Wipes = Base.getMachineLoader().getMachine().getModel().getWipeByIndex(0);
		WipeModel tool1Wipes = Base.getMachineLoader().getMachine().getModel().getWipeByIndex(1);
		ArrayList<String> targetCode = new ArrayList<String>();
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0); //Min no decimals
		nf.setMaximumFractionDigits(2); //Max 2 decimal placesa
		nf.setGroupingUsed(false); //NO commas!
		// define constants/parameters
		// note: the array position corresponds to toolhead number (ie. purge_x[0] is the x purge location for toolhead 0)
		String[] purge_x = {nf.format(tool0Wipes.getX1()), nf.format(tool1Wipes.getX1())}, // purge location with respect to machine coordinates (G53) (pre-wipe)
		purge_y = {nf.format(tool0Wipes.getY1()),nf.format(tool1Wipes.getY1())},
		purge_x_offset = {nf.format(tool0Wipes.getX2()), nf.format(tool1Wipes.getX2())},  // post-wipe purge location
		purge_y_offset = {nf.format(tool0Wipes.getY1()), nf.format(tool1Wipes.getY2())}, // purge y = purge y offset (no y change)
		purge_z = {nf.format(tool0Wipes.getZ1()), nf.format(tool1Wipes.getZ1())},
		purge_z_offset = {nf.format(tool0Wipes.getZ2()),nf.format(tool1Wipes.getZ2())},
		wait_time = {nf.format(tool0Wipes.getWait()), nf.format(tool1Wipes.getWait())},
		feedrate = {"3000.0", "3000.0"},
		flowrate = {nf.format(tool0Wipes.getPurgeRPM()),nf.format(tool1Wipes.getPurgeRPM())},  // pushback (purge) flowrate
		full_reversal_flowrate = {nf.format(tool0Wipes.getReverseRPM()), nf.format(tool1Wipes.getReverseRPM())},  // reversal flowrate for stopping extrusion
		//partial_reversal_flowrate = {2.0f, 2.0f}, // reversal flowrate for temporarily stopping extrusion
		hop_height = {"3.0", "3.0"}; // nozzle lift before returning to layer

		int[] purge_duration = {tool0Wipes.getPurgeDuration(), tool1Wipes.getPurgeDuration()},  // durations (in msec)
		full_reversal_duration = {tool0Wipes.getReverseDuration(), tool1Wipes.getReverseDuration()}; //TWEAK THIS!!!!
		//partial_reversal_duration = {7, 7};

		// reverse current toolhead
		targetCode.add("M108 R"+full_reversal_flowrate[nextToolnum]);
		//
		targetCode.add("M102");
		//
		targetCode.add("G04 P"+full_reversal_duration[nextToolnum]);
		targetCode.add("M103");
		targetCode.add("M108 R"+flowrate[nextToolnum]);

		if(useWipes)
		{
			// move to purge home
			targetCode.add("G53");
			if (layer_height > Float.parseFloat(purge_z[nextToolnum])) {
				// if we're higher than the purge height go over and then down
				targetCode.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
				targetCode.add("G1 X" + purge_x[nextToolnum] +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
				targetCode.add("G1 Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			} 
			else 
			{
				// otherwise go up and then over
				//targetCode.add("G1 Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
				targetCode.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
				targetCode.add("G1 X" + purge_x[nextToolnum] + " Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			}
			// purge upcoming nozzle
			targetCode.add("M103 T"+nextToolnum);
			targetCode.add("G1 X" + purge_x[nextToolnum] +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			targetCode.add("M101");
			targetCode.add("G04 P"+purge_duration[nextToolnum]);
			//targetCode.add("M108 R"+partial_reversal_flowrate[nextToolnum]);
			targetCode.add("M102");
			//targetCode.add("G04 P"+partial_reversal_duration[nextToolnum]);
			targetCode.add("M103");
			targetCode.add("M108 R"+flowrate[nextToolnum]);
			targetCode.add("G04 P" +wait_time[nextToolnum]);
			// wipe upcoming nozzle
			targetCode.add("G1 X" + purge_x_offset[nextToolnum] +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			targetCode.add("G1 X" + purge_x_offset[nextToolnum] +" Y" + purge_y_offset[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
			// wipe current nozzle
			targetCode.add("G1 X" + purge_x[currentToolnum] +" Y" + purge_y[currentToolnum] + " Z" + purge_z[currentToolnum] + " F" + feedrate[currentToolnum]);
			targetCode.add("G1 X" + purge_x_offset[currentToolnum] +" Y" + purge_y[currentToolnum] + " Z" + purge_z[currentToolnum] + " F" + feedrate[currentToolnum]);
			targetCode.add("G1 X" + purge_x_offset[currentToolnum] +" Y" + purge_y_offset[currentToolnum] + " Z" + purge_z_offset[currentToolnum] + " F" + feedrate[currentToolnum]);
			// return to purge home
			targetCode.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
		}
		return targetCode;
	}
	/**
	 * This is Will's toolchange method ported, it treats the actual wipe as a seperate method so in the future one could reduce the number of wipes by only making them happen every x number of toolchanges etc
	 * @param nextTool this is the Tool we would like to change into
	 * @param layer_height this is the layer height to do it at
	 * @return
	 */
	private static float getLayerIncrement()
	{
		System.out.println(PrimaryLayers.get(1).getHeight() - PrimaryLayers.get(0).getHeight());
		return PrimaryLayers.get(1).getHeight() - PrimaryLayers.get(0).getHeight();
	}
	public static ArrayList<String> completeToolChange(Toolheads nextTool, float layer_height) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0); //Min no decimals
		nf.setMaximumFractionDigits(2); //Max 2 decimal placesa
		nf.setGroupingUsed(false); //NO commas!
		ArrayList<String> targetCode = new ArrayList<String>();
		Toolheads currentTool = null;

		if (nextTool == Toolheads.Primary) { 
			currentTool = Toolheads.Secondary;
		} else if (nextTool == Toolheads.Secondary) { 
			currentTool = Toolheads.Primary;
		}
		int nextToolnum = nextTool.ordinal();
		int currentToolnum = currentTool.ordinal();
		float hop_height = getLayerIncrement()*10f;
		targetCode.add("(<toolchange>)");
		float purge_z = 6.5f;

		targetCode.add("M103");



		targetCode.add("G1 Z" + nf.format((layer_height+hop_height)) + (" (modified by HopMultipleofLayerHeight)"));
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
		targetCode.add("G1 Z"+ nf.format(h) +" F2000");

		targetCode.add("(</toolchange>)");
		//System.out.println(currentFeedRate);
		//targetCode.add(currentFeedRate);
		return targetCode;
	}

}
