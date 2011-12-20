package replicatorg.app.gcode;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import replicatorg.machine.model.ToolheadAlias;
import replicatorg.model.GCodeSource;


/**
 * This class takes two existing gcode files and merges them into a single gcode that can be run on a dualstrusion printer
 * 
 * @author Noah Levy
 * @maintained Ted
 */
public class DualStrusionConstruction {

	private File result;
	private File primary, secondary;
	private boolean replaceStart, replaceEnd, useWipes;

	public DualStrusionConstruction(File primary, File secondary, File destination, boolean replaceStart, boolean replaceEnd, boolean useWipes)
	{
		this.primary = primary;
		this.secondary = secondary;
		this.result = destination;
		this.replaceStart = replaceStart;
		this.replaceEnd = replaceEnd;
		this.useWipes = useWipes;
	}
	public File getCombinedFile()
	{
		return result;	
	}

	/**
	 * This method handles shuffling together two gcodes, it first executes
	 * preprocessing and then hands the gcodes off to Layer_Helper
	 * 
	 */
	public void combine() {

		/* Potential order of things to do:
		 * 
		 * load up files (do we know if they're coming from old gcode or just-processed stl?
		 *   does it change how we do things? I think it's too hard to get that info here.
		 *   let's not bother.)
		 * 
		 * remove start/end if we can find it
		 * 
		 * parse into layers
		 * 
		 * make sure every layer starts by setting the correct toolhead
		 * 
		 * merge layers, adding any tweening code that's necessary
		 *   changing tools (tweening) is only necessary when the next layer is not the same toolhead as this one?
		 *   wipes need to be toggleable (really, "use machine's wipe" should be toggleable, there'll be other tween code)
		 * 
		 * add start and end gcode
		 * 
		 * 
		 */

//		GCodeSource primaryGcode = GCodeHelper.readFiletoGCodeSource(primary);
//		GCodeSource secondaryGcode = GCodeHelper.readFiletoGCodeSource(secondary);
//		ArrayList<String> master_layer = new ArrayList<String>();
//
//		GCodeSource startGcode = GCodeHelper.readFiletoGCodeSource(new File("DualStrusion_Snippets/start.gcode"));
//		GCodeSource endGcode = GCodeHelper.readFiletoGCodeSource(new File("DualStrusion_Snippets/end.gcode"));
//
//		// get our temp commands on a per-file basis
//		String primaryTemp = getTemperatureCommand(primaryGcode);
//		String secondaryTemp = getTemperatureCommand(secondaryGcode);
//
//		// TRICKY: since both files are 'default toolhead' (t1) swap primaryTemp
//		// to be 'first toolhead' (t0)
//		// this could be moved to DSW, by passing a different toolhead for each file?
//		// But what about when given gcode? we may have to check for which T a file uses
//		primaryTemp = swapToolhead(primaryTemp, Toolheads.LEFT.getTcode());
//		
//		// 
//		if (replaceStart) {
//			stripStartGcode(primaryGcode);
//		} else {
//			startGcode = saveStartGcode(primaryGcode); // startGcode comes from prior file
//		}
//
//		if (replaceEnd == false)
//			endGcode = saveEndGcode(primaryGcode);
//
//		// on combine, always strip start/end of secondaryGcode
//		stripStartGcode(secondaryGcode);
//		stripEndGcode(secondaryGcode);
//
//		primaryGcode = replaceToolHeadReferences(primaryGcode,
//				Toolheads.LEFT);
//		secondaryGcode = replaceToolHeadReferences(secondaryGcode,
//				Toolheads.RIGHT);
//
//		// interlace the layers for each toolhead
//		LayerHelper helper = new LayerHelper(primaryGcode, secondaryGcode, false, useWipes);
//		master_layer = helper.mergeLayers();
//
//		//modifyTempReferences(startGcode, primaryTemp, secondaryTemp);
//		modifyTempReference(startGcode, Toolheads.LEFT, primaryTemp);
//		modifyTempReference(startGcode, Toolheads.RIGHT, secondaryTemp);
//
//		// add start and end gcode
//		master_layer.addAll(0, startGcode);
//		master_layer.addAll(master_layer.size(), endGcode);
//
//		mayHaveWipeCrash(master_layer);
//		writeGCodeSourcetoFile(master_layer, dest);

//		try
//		{
//			result = DualStrusionWorker.combineGcode(primary, secondary, dest, replaceStart, replaceEnd, useWipes);
//			Base.getEditor().handleOpenFile(result);
//		}
//		catch(Exception e)
//		{
//			Base.logger.log(Level.SEVERE, "Could not finish combining gcodes for dualstrusion, Sorry.\n" +
//					"Dualstrusion is still very new functionality and is currently being improved.");
//			e.printStackTrace();
//		}
		
		// ************* Debug layer parsing ****************** //
//		System.out.println("File: " + primary.getAbsolutePath());
//
//		try {
//			FileWriter fileout = new FileWriter(new File("layers.txt"));
//			for(Layer l : parseLayers(GCodeHelper.readFiletoGCodeSource(primary)))
//			{
////				System.out.println("***************"+l.getHeight()+"**********");
////				System.out.println(l.toString());
////				System.out.println("****************************************");
//
//				fileout.append("***************"+l.getHeight()+"**********\n");
//				fileout.append(l.toString());
//				fileout.append("*******************************\n");
//			}
//			fileout.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	/**
	 * Takes a GCodeSource, assumed to be lacking any start- or end- specific blocks of code
	 * and to be in order of increasing layer height, and returns a LinkedList of Layers. Each Layer
	 * should contain codes for a single height. The list should be in sorted order from lowest
	 * height to highest.
	 * We use a LinkedList because all this is internal, so it doesn't change any interfaces if we 
	 * want to change it, and a LinkedList doubles as a Queue, which is handy for doMerge().
	 * 
	 * WARNING: This code assumes that the source gcode follows one of two formats:
	 *   Either the extruder is turned off at the end of each layer using an M103,
	 *   Or the gcode uses 5D, and there are no M103/M101/M108 commands
	 *   
	 * These should be safe assumptions for any code generated by Skeinforge
	 * @param source
	 * @return
	 */
	private LinkedList<Layer> parseLayers(GCodeSource source)
	{
		/*
		 * So this is a little more complicated than just breaking up stuff by Z height,
		 * there may be M commands between layers, some of which belong to the previous 
		 * layer, and some to the next. 
		 * To get around this we:
		 * Walk through the source
		 *   // this assumes that every layer ends with the extruder off
		 *   keep a trailing pointer to the last M103 we saw, and a count of the last layer height 
		 *   we saw when we see a new layer height, break off a new layer after the previous M103
		 *   
		 *   but, with 5d, there won't be any M103, layers should have no associated pre/post Mcodes
		 *   
		 */

		LinkedList<Layer> layers = new LinkedList<Layer>();	
		Queue<String> read = new LinkedList<String>();

		String lastM103 = null;
		double lastZHeight = Double.MIN_VALUE;
		for(String line : source)
		{
			GCodeCommand gcode = new GCodeCommand(line);
			
			if(gcode.getCodeValue('M') == 103)
				lastM103 = line;
			
			if(gcode.hasCode('Z'))
			{
				double newZ = gcode.getCodeValue('Z');
				
				// keeps us from creating an initial, empty layer
				if(lastZHeight == Double.MIN_VALUE)
				{
					lastZHeight = newZ;
				}
				else if(newZ > lastZHeight)
				{
					ArrayList<String> tmpLayer = new ArrayList<String>();
					
					// fill the tmpLayer with the accumulated lines, up to the 
					// most recent "stop extruding" or until the queue is empty (5D)
					while(read.peek() != null && read.peek() != lastM103)
						tmpLayer.add(read.poll());
					
					// Also grab the M103, if present
					if(read.peek() == lastM103)
						tmpLayer.add(read.poll());
					
					// put it in a new layer
					layers.add(new Layer(lastZHeight, tmpLayer));

					// record our next layer height
					lastZHeight = newZ;
				}
			}
			
			read.add(line);
		}
		
		return layers;
	}
	
	private Layer toolchange(ToolheadAlias from, ToolheadAlias to)
	{
		/*
		 * How does a toolchange work? Glad you asked:
		 * First we need to do any operations relating to the previous nozzle.
		 *   I think this is only a small reversal. It needs to be small because 
		 *   the previous layer may have ended with a reversal, and if we then 
		 *   reverse on top of that we'll lose the filament. 
		 * We need to prepare the nozzle that we're switching to, which means 
		 * doing a purge and wipe, if available.
		 *   The purge is to undo the reversal from before, the wipe rubs the 
		 *   nozzle across a special piece on the machine.
		 *   If wipes are turned off, do we still do purge? because that could
		 *   end us up with all kindsa junk on the outside of the object.
		 * For wipes: Since we're moving to another position to do the wipe, we
		 *   have to record the next position we want to be at, because if we 
		 *   start the next layer from a random place we might end up spewing 
		 *   plastic all the way to that point.
		 * At the end of a toolchange, we should disable whichever extruder is
		 *   not being used using M18 A B (on the next call to whichever axis 
		 *   it'll start up again)
		 *   
		 *   toolchange psudocode:
		 *   
		 *   Layer toolchange = new Layer
		 *   
		 *   toolchange.add(reversal code(currentTool)
		 *   
		 *   if newTool.zPosition < wipe height
		 *     layer.add(move up and over)
		 *   else
		 *     layer.add(move up, over, and down)
		 *     
		 *   if purge
		 *     layer.add(purge)
		 *     
		 *   if wipes
		 *     layer.add(wipes)
		 *     
		 *   nextPos = get next position (first G1 of next layer)
		 *   layer.add(move up, perhaps just above the next layer height, as quickly as is reasonable)
		 *   layer.add(move to nextPos, also fairly quickly)
		 *   layer.add(set speed to F from nextPos, or, 
		 *   								if that's not present, the last F from the previous layer)
		 *   
		 *   layer.add(M18 A B)
		 */
		return null;
	}
	
	private void doMerge(LinkedList<Layer> a, LinkedList<Layer> b)
	{
		/*
		 *   Merging layers should look something like this:
		 *   Queue<Layer> A, B;
		 *   List<Layer> result
		 *   A = layers from one file, sorted from least to greatest
		 *   B = layers from other file, sorted from least to greatest
		 *   last = null 
		 *   while A || B are not empty
		 *     if A.peek.height < B.peek.height
		 *       if last == B
		 *         result.append(toolchange B to A)
		 *       result.append(A.pop)
		 *       last = A
		 *     else if B.peek.height < A.peek.height
		 *       if last == A
		 *         result.append(toolchange A to B)
		 *       result.append(B.pop)
		 *       last = B
		 *     else // they're of equal height
		 *       if last != null
		 *         if last == A
		 *           result.append(A.pop)
		 *         else if last == B
		 *           result.append(B.pop)
		 *       else
		 *         result.append(A.pop)
		 *           
		 */
		
	}
}
