package replicatorg.dualstrusion;

import java.text.NumberFormat;
import java.util.ArrayList;

public class Layer_Helper {

	private static Toolheads currentToolhead = Toolheads.Primary;
	public static ArrayList<Layer> PrimaryLayers = new ArrayList<Layer>();
	public static ArrayList<Layer> SecondaryLayers = new ArrayList<Layer>();
	private static float tolerance = .01f;
	//private static String currentFeedRate; // good default start speed
	public static ArrayList<String> doMerge(ArrayList<String> prime, ArrayList<String> second)
	{
		readLayers(prime, PrimaryLayers);
		readLayers(second, SecondaryLayers);
		seeLayerHeights(SecondaryLayers);
		System.out.println(PrimaryLayers.size() + " " + SecondaryLayers.size());
		return mergeLayers(PrimaryLayers, SecondaryLayers);

	}
	public static void replaceStart()
	{
		
	}
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
	private static void seeLayerHeights(ArrayList<Layer> a)
	{
		for(Layer l : a)
		{
			System.out.println(l.getHeight());
		}
	}
	public static ArrayList<String> toolChange(Toolheads destinationTool, float LayerHeight)
	{
		//ArrayList<String> tempToolChange;
		/*
		if(destinationTool == Toolheads.Primary)
		{
			currentToolhead = Toolheads.Primary;
			return completeToolChange;
		}
		else if(destinationTool == Toolheads.Secondary)
		{
			currentToolhead = Toolheads.Secondary;
			tempToolChange = new ArrayList<String>(ToolChangeToSecondary);
			tempToolChange.add(currentFeedRate);
			return tempToolChange;
		}
	
		return null;
	*/
		currentToolhead = destinationTool;
		return completeToolChange(destinationTool, LayerHeight);
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
		for(float i = 0; i < maxHeight - .008; i += tolerance)
		{
			//System.out.println("checking " + i);
			Layer a = getByHeight(i, primary); //primary
			Layer b = getByHeight(i, secondary);//secondary
			if(a != null || b != null)
			{
				System.out.println("non null layers at " + i);
				//DualStrusionWorker.printArrayList(a.getCommands());
				//DualStrusionWorker.printArrayList(b.getCommands());
				if(a != null && b != null)
				{
					System.out.println("this is called");
					merged.addAll(mergeLayer(a,b));
				}
				else if(a != null)
				{
					//setCurrentFeedRate(b.getCommands());
					merged.addAll(parseLayer(a, Toolheads.Primary));
				}
				else if(b != null)
				{
					//setCurrentFeedRate(a.getCommands());
					merged.addAll(parseLayer(b, Toolheads.Secondary));
				}
			}
		}
		return merged;
	}
	private static ArrayList<String> parseLayer(Layer a, Toolheads destTool)
	{
	//	setCurrentFeedRate(a.getCommands());
		ArrayList<String> completeLayer = new ArrayList<String>();
		if(destTool == Toolheads.Primary)
		{
			if(currentToolhead == Toolheads.Primary)
			{
				//System.out.println(a.getCommands());
				completeLayer.addAll(a.getCommands());
			}
			else if(currentToolhead == Toolheads.Secondary)
			{
				completeLayer.addAll(toolChange(Toolheads.Primary, a.getHeight()));
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
				completeLayer.addAll(toolChange(Toolheads.Secondary, a.getHeight()));
				completeLayer.addAll(a.getCommands());
			}
		}
		return completeLayer;
	}
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

						System.out.println("reading in layer at height " + layerHeight);
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
			cmds.addAll(toolChange(Toolheads.Secondary, a.getHeight()));
			cmds.addAll(b.getCommandsWithoutLayerTag());
		}
		else if(currentToolhead == Toolheads.Secondary)
		{
			//setCurrentFeedRate(a.getCommands());
			cmds.addAll(b.getCommandsWithoutLayerTag());
			cmds.addAll(toolChange(Toolheads.Primary, b.getHeight()));
			cmds.addAll(a.getCommandsWithoutLayerTag());
		}
		cmds.add("(</layer>)");
		//Layer l = new Layer(a.getHeight(),cmds);
		return cmds;
	}
	public static ArrayList<String> wipe(int currentToolnum, int nextToolnum, float layer_height) {
		ArrayList<String> targetCode = new ArrayList<String>();
		
		
		// define constants/parameters
		// note: the array position corresponds to toolhead number (ie. purge_x[0] is the x purge location for toolhead 0)
		float[] purge_x = {38.0f, -38.0f}, // purge location with respect to machine coordinates (G53) (pre-wipe)
		purge_y = {55.0f, 55.0f},
		purge_x_offset = {45.0f, -45.0f},  // post-wipe purge location
		purge_y_offset = {55.0f, 55.0f},
		purge_z = {6.5f, 6.5f},  
		feedrate = {3000.0f, 3000.0f},
		flowrate = {5.0f, 5.0f},  // pushback (purge) flowrate
		full_reversal_flowrate = {25.0f, 25.0f},  // reversal flowrate for stopping extrusion
		//partial_reversal_flowrate = {2.0f, 2.0f}, // reversal flowrate for temporarily stopping extrusion
		hop_height = {3.0f, 3.0f}; // nozzle lift before returning to layer

		int[] purge_duration = {1000, 1000},  // durations (in msec)
		full_reversal_duration = {15, 15}; //TWEAK THIS!!!!
		//partial_reversal_duration = {7, 7};

		// reverse current toolhead
		targetCode.add("M108 R"+full_reversal_flowrate[nextToolnum]);
		//
		targetCode.add("M102");
		//
		targetCode.add("G04 P"+full_reversal_duration[nextToolnum]);
		targetCode.add("M103");
		targetCode.add("M108 R"+flowrate[nextToolnum]);

		// move to purge home
		targetCode.add("G53");
		if (layer_height > purge_z[nextToolnum]) {
			// if we're higher than the purge height go over and then down
			targetCode.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
			targetCode.add("G1 X" + purge_x[nextToolnum] +" Y" + purge_y[nextToolnum] + " F" + feedrate[nextToolnum]);
			targetCode.add("G1 Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
		} 
		else {
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
		targetCode.add("G04 P2000");
		// wipe upcoming nozzle
		targetCode.add("G1 X" + purge_x_offset[nextToolnum] +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
		targetCode.add("G1 X" + purge_x_offset[nextToolnum] +" Y" + purge_y_offset[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);
		// wipe current nozzle
		targetCode.add("G1 X" + purge_x[currentToolnum] +" Y" + purge_y[currentToolnum] + " Z" + purge_z[currentToolnum] + " F" + feedrate[currentToolnum]);
		targetCode.add("G1 X" + purge_x_offset[currentToolnum] +" Y" + purge_y[currentToolnum] + " Z" + purge_z[currentToolnum] + " F" + feedrate[currentToolnum]);
		targetCode.add("G1 X" + purge_x_offset[currentToolnum] +" Y" + purge_y_offset[currentToolnum] + " Z" + purge_z[currentToolnum] + " F" + feedrate[currentToolnum]);
		// return to purge home
		targetCode.add("G1 X" + 0 +" Y" + purge_y[nextToolnum] + " Z" + purge_z[nextToolnum] + " F" + feedrate[nextToolnum]);

		return targetCode;
	}
	public static ArrayList<String> completeToolChange(Toolheads nextTool, float layer_height) {
		ArrayList<String> targetCode = new ArrayList<String>();
		Toolheads currentTool = Toolheads.Secondary;

		if (nextTool == Toolheads.Primary) { 
			currentTool = Toolheads.Secondary;
		} else if (nextTool == Toolheads.Secondary) { 
			currentTool = Toolheads.Primary;
		}
		int nextToolnum = nextTool.ordinal();
		int currentToolnum = currentTool.ordinal();
		float hop_height = 7.0f;
		targetCode.add("(<toolchange>)");
		float purge_z = 6.5f;
		targetCode.add("M103");
		targetCode.add("G1 Z" + (layer_height+hop_height));
		targetCode.addAll(wipe(currentToolnum, nextToolnum,  layer_height));
		targetCode.add("M103");
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
