package replicatorg.app.gcode;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import replicatorg.app.Base;
import replicatorg.machine.model.MachineType;
import replicatorg.machine.model.ToolheadAlias;
import replicatorg.machine.model.WipeModel;
import replicatorg.model.GCodeSource;
import replicatorg.util.Point5d;


/**
 * This class takes two existing gcode files and merges them into a single gcode that can be run on a dualstrusion printer
 * 
 * @author Noah Levy
 * @maintained Ted
 */
public class DualStrusionConstruction
{

	private MutableGCodeSource result;
	private final MutableGCodeSource left, right;
	private final MutableGCodeSource start, end;
	private final boolean useWipes;
	private final WipeModel leftWipe;
	private final WipeModel rightWipe;
	private final MachineType machineType;

	public DualStrusionConstruction(MutableGCodeSource left, MutableGCodeSource right,
									MutableGCodeSource startSource, MutableGCodeSource endSource,
									MachineType type, boolean useWipes)
	{
		this.left = left;
		this.right = right;
		this.useWipes = useWipes;
		this.machineType = type;
		start = startSource.copy();
		end = endSource.copy();
		if(useWipes)
		{
			leftWipe = Base.getMachineLoader().getMachineInterface().getModel().getWipeFor(ToolheadAlias.LEFT);
			rightWipe = Base.getMachineLoader().getMachineInterface().getModel().getWipeFor(ToolheadAlias.RIGHT);
			
			if(leftWipe == null || rightWipe == null)
			{			
				String error = "Could not find wipes for the current machine: " + 
					Base.getMachineLoader().getMachineInterface().getModel().toString() + ". Continuing without wipes.";
				JOptionPane.showConfirmDialog(null, error, 
						"Could not find wipes!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);

				useWipes = false;
			}
		}
		else
		{
			leftWipe = null;
			rightWipe = null;
		}
	}
	public MutableGCodeSource getCombinedFile()
	{
		return result;	
	}

	/**
	 * This method handles shuffling together two gcodes, it first executes
	 * preprocessing and then hands the gcodes off to Layer_Helper
	 * 
	 */
	public void combine()
	{

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
		 * do we need special setup code based on which layer is the first?
		 * 
		 * 
		 * add start and end gcode
		 * 
		 * 
		 */

		//we're doing this in DSWindow for gcode
		// not needed for stl
//		left.stripStartEndBestEffort();
//		right.stripStartEndBestEffort();
		
		stripNonLayerTagComments(left);
		stripNonLayerTagComments(right);

System.out.print("dcs.combine.parselayers.left");
		LinkedList<Layer> leftLayers = newOldParseLayers(left);
System.out.print("dcs.combine.parselayers.right");
		LinkedList<Layer> rightLayers = newOldParseLayers(right);

System.out.print("dcs.combine.setlayerstart");
		//make sure every layer starts by setting the correct toolhead
		for(Layer l : leftLayers)
		{
			l.getCommands().add(0, ToolheadAlias.LEFT.getRecallOffsetGcodeCommand());
			l.getCommands().add(0, "M6 P1 "+ToolheadAlias.LEFT.getTcode() + "(Set tool)");
		}
		for(Layer l : rightLayers)
		{
			l.getCommands().add(0, ToolheadAlias.RIGHT.getRecallOffsetGcodeCommand());
			l.getCommands().add(0, "M6 P1 "+ToolheadAlias.RIGHT.getTcode() + "(Set tool)");
		}

System.out.print("dcs.combine.domerge");
		final LinkedList<Layer> merged = doMerge(leftLayers, rightLayers);
		
		//process start & end before adding them
		duplicateToolheadLines(start);
		duplicateToolheadLines(end);

		//debug code///////////////////////////
		merged.add(0, new Layer(0d, new ArrayList<String>(){{add("(********************************************************************************post-start**************************************************************)");}}));
		//////////////////////////////////////
		merged.add(0, new Layer(0d, start.asList()));
		//debug code///////////////////////////
		merged.add(new Layer(0d, new ArrayList<String>(){{add("(********************************************************************************pre-start**************************************************************)");}}));
		//////////////////////////////////////
		merged.add(new Layer(Double.MAX_VALUE, end.asList()));

		result = new MutableGCodeSource();
		for(Layer l : merged)
		{
			result.add(l.getCommands());
		}
		
	}
	
	/**
	 * removes all lines that are skeinforge tag comments, but not layer tags.
	 */
	public void stripNonLayerTagComments(MutableGCodeSource source) {
		String line;
		for(Iterator<String> i = source.iterator(); i.hasNext();)
		{
			line = i.next();
			
			if(line.startsWith("(<") &&	!(line.startsWith("(<layer>") || line.startsWith("(</layer")))
			{
				i.remove();
			}
		}
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
	private LinkedList<Layer> parseLayers(final GCodeSource source)
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

		final LinkedList<Layer> layers = new LinkedList<Layer>();	
		final Queue<String> read = new LinkedList<String>();

		//debug code///////////////////////////
		layers.add(new Layer(0d, new ArrayList<String>(){{add("(********************************************************************************start layer**************************************************************)");}}));
		//////////////////////////////////////
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

		//debug code///////////////////////////
		layers.add(new Layer(0d, new ArrayList<String>(){{add("(********************************************************************************end layer**************************************************************)");}}));
		//////////////////////////////////////
		return layers;
	}
	
	private LinkedList<Layer> oldParseLayers(GCodeSource source)
	{
		final LinkedList<Layer> result = new LinkedList<Layer>();
		
		final List<String> sourceList = source.asList();
		
		for(int i = 0; i < sourceList.size();  i++)
		{
			if(sourceList.get(i).matches("\\(\\<layer\\>.*\\)"))
			{
				float layerHeight = 0;
				try
				{
					layerHeight = Float.parseFloat(sourceList.get(i).split(" ")[1]);
				}
				catch(NumberFormatException e)
				{
					Base.logger.log(Level.SEVERE, "one of your layer heights was unparseable, " +
							"please check and make sure all of them are in the format (<layer> 0.00)");
				}

				int a = i + 1;
				while(true)
				{
					if(sourceList.get(a).equalsIgnoreCase("(</layer>)"))
					{
						ArrayList<String> tempList = new ArrayList<String>(sourceList.subList(i, a+1));
						result.add(new Layer(layerHeight, tempList)); 
						break;
					}
					a++;
				}
			}
		}

		return result;
	}
	
	private LinkedList<Layer> newOldParseLayers(GCodeSource source)
	{
		final LinkedList<Layer> result = new LinkedList<Layer>();
		String line;
		for(Iterator<String> it = source.iterator(); it.hasNext();)
		{
			line = it.next();
			if(line.startsWith("(<layer>"))
			{
				float layerHeight = 0;
				try
				{
					layerHeight = Float.parseFloat(line.split(" ")[1]);
				}
				catch(NumberFormatException e)
				{
					Base.logger.log(Level.SEVERE, "one of your layer heights was unparseable, " +
							"please check and make sure all of them are in the format (<layer> 0.00)");
				}
				
				final List<String> accumulate = new ArrayList<String>();
				String next = it.next();
				while(!next.startsWith("(</layer>)"))
				{
					accumulate.add(next);
					next = it.next();
				}
				//skip empty layers
				if(!accumulate.isEmpty())
					result.add(new Layer(layerHeight, accumulate));
			}
		}

		return result;
	}
	private Layer toolchange(final ToolheadAlias from, final Layer fromLayer, final ToolheadAlias to, final Layer toLayer)
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
		final ArrayList<String> result = new ArrayList<String>();
		//debug code///////////////////////////
		result.add("(********************************************************************************start toolchange**************************************************************)");
		//////////////////////////////////////
		if(useWipes)
		{
			// The left/right distinction isn't actually important here
			// on a tom you have to wipe both heads, and on a replicator
			// wiping either does both
			result.addAll(wipe(leftWipe));
			if(machineType != MachineType.THE_REPLICATOR)
				result.addAll(wipe(rightWipe));
		}
		
		// Ben's suggestion
		result.add("M18 A B");
		
		final NumberFormat nf = Base.getGcodeFormat();
		final Point5d firstPos = getFirstPosition(toLayer);
		
		if(firstPos != null)
		{
			// The F here is a magic number, you can read about it in the 'wipe()' function
			// move up fairly quickly
			result.add("G1 Z" + nf.format(firstPos.z()) +" F3000");
			// move to the next point
			result.add("G1 X" + nf.format(firstPos.x()) + " Y" + nf.format(firstPos.y()) + " Z" + nf.format(firstPos.z()) +" F3000");
		}
		else
		{
//			System.err.print(toLayer);
		}
		
		//TODO: catch possible null pointer exceptions?
		// set the feedrate with an empty G1
		String feedrate = getFirstFeedrate(toLayer);
		if(feedrate.equals(""))
			feedrate = getLastFeedrate(fromLayer);
		result.add("G1 " + feedrate);
		
		
		//debug code///////////////////////////
		result.add("(********************************************************************************end toolchange**************************************************************)");
		//////////////////////////////////////
		// The 'height' of the toolchange. just the average of the surrounding layers because why not?
		final double height = (toLayer.getHeight() - fromLayer.getHeight())/2;
		
		return new Layer(height, result);
	}
	
	private Point5d getFirstPosition(final Layer l)
	{
		final List<String> search = l.getCommands();
		GCodeCommand gcode;
		for(int i = 0; i < search.size(); i++)
		{
			gcode = new GCodeCommand(search.get(i));
			if(gcode.getCodeValue('G') == 1)
			{
				Point5d result = new Point5d();
				result.setX(gcode.getCodeValue('X'));
				result.setY(gcode.getCodeValue('Y'));
				result.setZ(gcode.getCodeValue('Z'));
				return result;
			}
		}
		return null;
	}
	
	private String getLastFeedrate(final Layer l)
	{
		final List<String> search = l.getCommands();
		GCodeCommand gcode;
		for(int i = search.size()-1; i >= 0; i--)
		{
			gcode = new GCodeCommand(search.get(i));
			if(gcode.getCodeValue('F') != -1)
				return "F"+Base.getGcodeFormat().format(gcode.getCodeValue('F'));
		}
		return "";
	}
	private String getFirstFeedrate(final Layer l)
	{
		final List<String> search = l.getCommands();
		GCodeCommand gcode;
		for(int i = 0; i < search.size(); i++)
		{
			gcode = new GCodeCommand(search.get(i));
			if(gcode.getCodeValue('F') != -1)
				return "F"+Base.getGcodeFormat().format(gcode.getCodeValue('F'));
		}
		return "";
	}
	
	private ArrayList<String> wipe(final WipeModel toolWipe)
	{
		final ArrayList<String> result = new ArrayList<String>();

		//debug code///////////////////////////
		result.add("(********************************************************************************start wipe**************************************************************)");
		//////////////////////////////////////

		// This is a not-entirely-arbitrarily chosen number
		// Ben or Noah may be able to explain it,
		// Ted might be able to by the time you ask
		final String feedrate = "F3000";
		// move to purge home
		result.add("G53");

		// Ben and Ted had a chat and believe that it is almost always safe to do the move for wipes in this order
		result.add("G1 " + toolWipe.getY1() +" "+ feedrate);
		result.add("G1 " + toolWipe.getZ1() +" "+ feedrate);
		result.add("G1 " + toolWipe.getX1() +" "+ feedrate);	

		// purge current toolhead
		result.add("M108 "+toolWipe.getPurgeRPM());
		result.add("M102");
		result.add("G04 "+toolWipe.getPurgeDuration());
		result.add("M103");
		
		// reverse current toolhead
		result.add("M108 "+toolWipe.getReverseRPM());
		result.add("M102");
		result.add("G04 "+toolWipe.getReverseDuration());
		result.add("M103");		
		// wait for leak
		result.add("G04 " + toolWipe.getWait());
		
		// move to second wipe position
		result.add("G1 " + toolWipe.getX2() +" "+ toolWipe.getY2() +" "+ toolWipe.getZ2() +" "+ feedrate);

		//debug code///////////////////////////
		result.add("(********************************************************************************end wipe**************************************************************)");
		//////////////////////////////////////
		return result;
	}
	
	/**
	 * This will consume two LinkedLists of Layers and return a combined List of Layers
	 * representing a dualstrusion print, with all the appropriate toolchanges inserted. 
	 * @param left
	 * @param right
	 */
	private LinkedList<Layer> doMerge(final LinkedList<Layer> left, final LinkedList<Layer> right)
	{
		/*
		 *   Merging layers should look something like this:
		 *   Queue<Layer> A, B;
		 *   List<Layer> result
		 *   A = layers from one file, sorted from least to greatest
		 *   B = layers from other file, sorted from least to greatest
		 *   last = null 
		 *   while A && B are not empty
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
		 *   // at this point one of them is empty
		 *   if A is not empty
		 *     if last == B
		 *       result.append(toolchange B to A)
		 *     result.appendAll(A)
		 *   if B is not empty
		 *     if last == A
		 *       result.append(toolchange A to B)
		 *     result.appendAll(B)
		 *     
		 *           
		 */
		// using a LinkedList means we can getLast()
		final LinkedList<Layer> result = new LinkedList<Layer>();

		// this is just a handy way to keep track of where our last layer came from
		Object lastLayer = null;
		
		while((!left.isEmpty()) || (!right.isEmpty()))
		{
			if(right.isEmpty())
			{
				if(right.equals(lastLayer))
					result.add(toolchange(ToolheadAlias.RIGHT, result.getLast(), ToolheadAlias.LEFT, left.peek()));
				result.add(left.pop());
				lastLayer = left;
			}
			else if(left.isEmpty())
			{
				if(left.equals(lastLayer))
					result.add(toolchange(ToolheadAlias.LEFT, result.getLast(), ToolheadAlias.RIGHT, right.peek()));
				result.add(right.pop());
				lastLayer = right;
			}
			else if(left.peek().getHeight() < right.peek().getHeight())
			{
				if(right.equals(lastLayer))
					result.add(toolchange(ToolheadAlias.RIGHT, result.getLast(), ToolheadAlias.LEFT, left.peek()));
				result.add(left.pop());
				lastLayer = left;
			}
			else if(right.peek().getHeight() < left.peek().getHeight())
			{
				if(left.equals(lastLayer))
					result.add(toolchange(ToolheadAlias.LEFT, result.getLast(), ToolheadAlias.RIGHT, right.peek()));
				result.add(right.pop());
				lastLayer = right;
			}
			else //equal height
			{
				if(lastLayer == null)
				{
					//arbitrary
					result.add(left.pop());
					lastLayer = left;
				}
				else
				{
					if(lastLayer == left)
						result.add(left.pop());
					else// if(lastLayer == right)
						result.add(right.pop());
				}
			}
		}
		
		return result;
	}
	
	private void duplicateToolheadLines(final MutableGCodeSource source)
	{
		int idx = 0;
		String line;
		double toolhead;
		final List<String> sourceList = source.asList();
		for(int i = 0; i < source.getLineCount(); i++)
		{
			line = sourceList.get(i);
			idx++;
			GCodeCommand gcode = new GCodeCommand(line);
			
			toolhead = gcode.getCodeValue('T');
			if(toolhead == 0)
				source.add(idx, line.replace("T0", "T1"));
			if(toolhead == 1)
				source.add(line.replace("T1", "T0"));
			if(toolhead != -1)
				i++;
			
		}
	}
}
