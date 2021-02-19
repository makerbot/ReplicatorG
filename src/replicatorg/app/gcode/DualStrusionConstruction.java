package replicatorg.app.gcode;

import java.io.File;
import java.text.DecimalFormat;
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
import replicatorg.plugin.toolpath.skeinforge.SkeinforgePostProcessor;
import replicatorg.util.Point5d;


/**
 * This class takes two existing gcode files and merges them into a single gcode that can be run on a dualstrusion printer
 * 
 * TODO:
 * some small changes to try, see what they do to the print:
 *   tiny hops (~1mm)
 *   in toolchange - get max(nextFeed, lastFeed) 
 * 
 * @author Noah Levy
 * @maintained Ted
 * Modified by Richard Parkins
 */
public class DualStrusionConstruction
{
	private final File leftFile, rightFile;
	private final boolean useWipes;
	private final WipeModel leftWipe;
	private final WipeModel rightWipe;
	private final MachineType machineType;
    private final boolean pauseOnChange;
	private MutableGCodeSource  result;
	/* If we see no comments before the first <layer>, we assume that we're
	 * generating for a printer that doesn't handle comments and the user
	 * has removed all except the <layer> and </layer> that we need, so we
	 * don't copy those to the output or put in any of our own.
	 */
	private boolean seenComment;
	private Point5d homePos;
	
	public DualStrusionConstruction(File leftFile, File rightFile,
									MachineType type, boolean useWipes)
	{
		this.leftFile = leftFile;
		this.rightFile = rightFile;
		this.useWipes = useWipes;
		this.machineType = type;
		this.seenComment = false;
		this.homePos = null;
		this.pauseOnChange =
            Base.preferences.getBoolean("dualstrusionwindow.pauseonchange",
                                        false);
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
	 * preprocessing and then hands the gcodes off to doMerge()
	 * 
	 */
	public void combine()
	{
		MutableGCodeSource left, right;

		// load our layers into something we can use 
		left = new MutableGCodeSource(leftFile);
		right = new MutableGCodeSource(rightFile);

		// load our layers into something we can *really* use
		LinkedList<Layer> leftLayers = parseLayers(left, 1);
		LinkedList<Layer> rightLayers = parseLayers(right, 0);

		// merge our layers into one list
		final LinkedList<Layer> layers = doMerge(leftLayers, rightLayers);

		// refresh and repopulate our result
		result = new MutableGCodeSource();
		for(Layer l : layers) {
			result.add(l.getCommands());
		}
		// interlace progress updates
		result.addProgressUpdates();
	}

	/**
	 * parseLayers is an improvement on the old parseLayers from Noah, etc. 's dualstrusion,
	 * but uses the same basic method because skeinforge is what it is.
	 * look for layer tags, break up the file using those tags.
	 * @param source
	 * @return
	 */
	private LinkedList<Layer> parseLayers(GCodeSource source, int toolNum)
	{
		final LinkedList<Layer> result = new LinkedList<Layer>();
		String line;

		String prefix;
		String oldTool;
		String newTool;
		String oldOffset;
		String newOffset;
		if (toolNum == 0) {
			prefix = new String("(<right> ");
			oldTool = ToolheadAlias.LEFT.getTcode();
			newTool = ToolheadAlias.RIGHT.getTcode();
			oldOffset = ToolheadAlias.LEFT.getRecallOffsetGcodeCommand();
			newOffset = ToolheadAlias.RIGHT.getRecallOffsetGcodeCommand();
		}
		else { // assume must be 1
			prefix = new String("(<left> ");
			oldTool = ToolheadAlias.RIGHT.getTcode();
			newTool = ToolheadAlias.LEFT.getTcode();
			oldOffset = ToolheadAlias.RIGHT.getRecallOffsetGcodeCommand();
			newOffset = ToolheadAlias.LEFT.getRecallOffsetGcodeCommand();
		}

		// start the first layer 
		List<String> accumulate = new ArrayList<String>();

		float layerHeight = 0;
		for(Iterator<String> it = source.iterator(); it.hasNext();)
		{
			// get a source line
			line = it.next();
			if (line.startsWith("(")) {
				// it's a comment
				if (line.startsWith("(</layer>")) {
					// end layer
					if (layerHeight < 0) {
						Base.logger.log(Level.SEVERE, "End layer with no start\n");
						layerHeight = 0;
					}
					// accumulate layer if not empty
					if (accumulate.size() > 0) {
						if (this.seenComment) {
							accumulate.add(prefix + line.substring(1));
						}
						result.add(new Layer(layerHeight, accumulate));
						accumulate = new ArrayList<String>();
					}
					layerHeight = -1;
				}
				else if (line.startsWith("(<layer>")) {
					// Get the layer height (or whatever SF claims it is)
					layerHeight = 0;
					try
					{
						layerHeight = Float.parseFloat(line.split(" ")[1]);
					}
					catch(NumberFormatException e)
					{
						Base.logger.log(Level.SEVERE, "one of your layer heights was unparseable, " +
								"please check and make sure all of them are in the format (<layer> 0.00)");
					}
					if (this.seenComment) {
						accumulate.add(prefix + line.substring(1));
					}
				}
				else {
				// other comment, just accumulate with prefix
					accumulate.add(prefix + line.substring(1));
					this.seenComment = true;
				}
			}
			else {
				// not a comment, accumulate without prefix
				if (!line.startsWith("G10")) {
					line = line.replace(oldTool, newTool);
					line = line.replace(oldOffset, newOffset);
				}
				accumulate.add(line);
			}
		}
		if (accumulate.size() > 0) {
			// save any end matter after last real layer with a silly height
			result.add(new Layer(999999, accumulate));
		}
		return result;
	}

	/**
	 * A toolchange is the code that goes in between commands for one head and commands for the other
	 * this function creates a toolchange from a tool doing one layer to a tool doing another layer
	 */
	private Layer toolchange(final ToolheadAlias fromTool, final Layer fromLayer, final ToolheadAlias toTool, final Layer toLayer)
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
		result.add("(*************start toolchange*************)");
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
		
        final DecimalFormat nf = (DecimalFormat)Base.getGcodeFormat();
        final Point5d firstPos = getFirstPosition(toLayer);
		if(firstPos != null)
		{
	        firstPos.setZ(getLayerZ(toLayer));
        }

        if (pauseOnChange)
        {
        	if (firstPos != null)
			{
				if (homePos == null)
				{
					homePos = new Point5d();
					homePos.setY(-70.0);
				}
	            // move up fairly quickly
	            result.add("G1 Y" + nf.format(homePos.y())
				           + " Z" + nf.format(firstPos.z() + 10.0)
			               + " F3000");
	        }
            result.add("M72 P2");
            result.add(
	            "M71 (Wait for oozing to  finish and then     press OK)");
            result.add("M70 P1 (Continuing...)");
        }
		// Offets deprecated nowadays, but this seems to be needed
		result.add(toTool.getRecallOffsetGcodeCommand());
		result.add("M108 "+toTool.getTcode() + " (Set tool)");
		
		// Ben's suggestion
		result.add("M18 A B");
		
		        //TODO: catch possible null pointer exceptions?
        // set the feedrate with an empty G1
        String feedrate = getFirstFeedrate(toLayer);
        if(feedrate.equals(""))
            feedrate = getLastFeedrate(fromLayer);
		
		if(firstPos != null)
		{
			// The F here is a magic number, you can read about it in the 'wipe()' function
            if (!pauseOnChange)
            {
                // move up fairly quickly
                result.add("G1 Z" + nf.format(firstPos.z()) +" F3000");
            }
			// move to the next point
			result.add("G1 X" + nf.format(firstPos.x())
			           + " Y" + nf.format(firstPos.y())
			           + " Z" + nf.format(firstPos.z())
			           + " " + feedrate);
		}
//		else
//		{
////			System.err.print(toLayer);
//		}
		
		result.add("G1 " + feedrate);

		
		//debug code///////////////////////////
		result.add("(*************end toolchange*************)");
		//////////////////////////////////////
		// The 'height' of the toolchange. just the average of the surrounding layers because why not?
		final double height = (toLayer.getHeight() - fromLayer.getHeight())/2;
		
		return new Layer(height, result);
	}
	/**
	 * gets the first G1 from a layer, returns the position of X, Y, Z axes
	 * @param l
	 * @return
	 */
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
	
	/**
	 * Apparently skeinforge does not have all the moves in a layer at the same height.
	 * The first one is frequently lower than the following ones, so this function finds
	 * the last height listed in a layer
	 * @param l the layer in which to look
	 * @return the layer height (maybe)
	 */
	private Double getLayerZ(final Layer l)
	{
		final List<String> search = l.getCommands();
		GCodeCommand gcode;
		for(int i = search.size()-1; i >= 0; i--)
		{
			gcode = new GCodeCommand(search.get(i));
			if(gcode.getCodeValue('G') == 1 && gcode.hasCode('Z'))
			{
				return gcode.getCodeValue('Z');
			}
		}
		return null;
	}
	
	/**
	 * This gets the last feedrate used in a layer 
	 * @param l
	 * @return
	 */
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
	/**
	 * This gets the first feedrate used in a layer 
	 * @param l
	 * @return
	 */
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
	
	/**
	 * **CURRENTLY UNTESTED**
	 * A wipe is something that can be attached to a machine to rub the toolhead over and
	 * clear it of excess plastic. the WipeModel specifies a before position and an after position
	 * as well as some parameters for extruding some plastic before wiping to prime the nozzle.
	 * 
	 * this function will always return the same thing for a given wipe, we could easily cache
	 * that thing and make this much more efficient.
	 * @param toolWipe
	 * @return
	 */
	private ArrayList<String> wipe(final WipeModel toolWipe)
	{
		final ArrayList<String> result = new ArrayList<String>();

		//debug code///////////////////////////
		result.add("(*************start wipe*************)");
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
		result.add("M101");
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
		result.add("(*************end wipe*************)");
		//////////////////////////////////////
		return result;
	}
	
	/**
	 * This will consume two LinkedLists of Layers and return a combined List of Layers
	 * representing a dualstrusion print, with all the appropriate toolchanges inserted.
	 * First layer and any end matter after last are treated specially
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

		// special merge code for initial layers
		// This is a bit dependent on users not modifying start.gcode too much
		if ((!left.isEmpty()) && (!right.isEmpty())) {
			ArrayList<String> start = new ArrayList<String>();
			ArrayList<String> dominant;
			ArrayList<String> secondary;
			if (this.seenComment) {
				start.add("(**** Merged startup gcode for Dualstrusion ****)");
			}
			if (   (left.peek().getHeight() != 0)
				|| (right.peek().getHeight() != 0)) {
				Base.logger.log(Level.SEVERE,
					"One of your gcode files did not have the required"
					+ " </layer> at the end of its start.gcode.\n"
					+ "Please use a standard start.gcode.\n");
			}
			ToolheadAlias initialTool;
            ToolheadAlias otherTool;
			if(right.get(1).getHeight() < left.get(1).getHeight()) {
				initialTool = ToolheadAlias.RIGHT;
				otherTool = ToolheadAlias.LEFT;
				dominant = (ArrayList<String>)right.pop().getCommands();
				secondary = (ArrayList<String>)left.pop().getCommands();
			}
			else {
				initialTool = ToolheadAlias.LEFT;
				otherTool = ToolheadAlias.RIGHT;
				dominant = (ArrayList<String>)left.pop().getCommands();
				secondary = (ArrayList<String>)right.pop().getCommands();
			}
			while (!dominant.isEmpty()) {
				String s = dominant.get(0);
				if (s.startsWith("M6")) {
					break;
				}
				else {
					start.add(s);
					dominant.remove(0);
				}
			}
			while (!secondary.isEmpty()) {
				String s = secondary.get(0);
				if (s.startsWith("(")) {
					start.add(s);
				}
				else if (s.startsWith("M104")) {
					start.add(s);
				}
				else if (s.startsWith("M6")) {
					break;
				}
				secondary.remove(0);
			}
			if (!dominant.isEmpty()) {
				start.add(dominant.get(0));
				dominant.remove(0);
			}
			if (!secondary.isEmpty()) {
				start.add(secondary.get(0));
				secondary.remove(0);
			}
			start.add("M108 " + initialTool.getTcode() + " (Set tool)");
			while (!dominant.isEmpty()) {
				String s = dominant.get(0);
				if ((homePos == null) && s.startsWith("G0")) {
					GCodeCommand gcode = new GCodeCommand(s);
					homePos = new Point5d();
					homePos.setY(gcode.getCodeValue('Y'));
				}
				start.add(s);
				dominant.remove(0);
			}
            start.add("M108 " + otherTool.getTcode() + " (Set tool)");
			while (!secondary.isEmpty()) {
				start.add(secondary.get(0));
				secondary.remove(0);
			}
            start.add("M108 " + initialTool.getTcode() + " (Set tool)");
			result.add(new Layer(0.0, start));
		}

		// loop while we still have layers to merge
		boolean doneEnd = false;
		while((!left.isEmpty()) || (!right.isEmpty()))
		{
			// if we've used all of our right layers, keep grabbing from left
			if(right.isEmpty())
			{
				if (left.peek().getHeight() < 999999) {
					// if last layer tool != next layer tool, add a toolchange
					if(right.equals(lastLayer)) {
						result.add(toolchange(ToolheadAlias.RIGHT, result.getLast(), ToolheadAlias.LEFT, left.peek()));
					}
					result.add(left.pop());
					lastLayer = left;
				}
				else {
					ArrayList<String> finish = new ArrayList<String>();
					ArrayList<String> commands;
					commands = (ArrayList<String>)left.pop().getCommands();
					while (!commands.isEmpty()) {
						String s = commands.get(0);
						if (   s.startsWith("(")
							|| (   (!s.startsWith("M104"))
								&& !doneEnd)) {
							finish.add(s);
						}
						commands.remove(0);
					}
					result.add(new Layer(999999, finish));
					doneEnd = true;
				}
			}
			else if(left.isEmpty()) // used all left layers, keep grabbing from right
			{
				if (right.peek().getHeight() < 999999) {
					// if last layer tool != next layer tool, add a toolchange
					if(left.equals(lastLayer)) {
						result.add(toolchange(ToolheadAlias.LEFT, result.getLast(), ToolheadAlias.RIGHT, right.peek()));
					}
					result.add(right.pop());
					lastLayer = right;
				}
				else {
					ArrayList<String> finish = new ArrayList<String>();
					ArrayList<String> commands;
					commands = (ArrayList<String>)right.pop().getCommands();
					while (!commands.isEmpty()) {
						String s = commands.get(0);
						if (   s.startsWith("(")
							|| (   (!s.startsWith("M104"))
								&& !doneEnd)) {
							finish.add(s);
						}
						commands.remove(0);
					}
					result.add(new Layer(999999, finish));
					doneEnd = true;
				}
			}
			else if (left.peek().getHeight() < right.peek().getHeight())
			{
				// left has a lower layer, grab it
				// if last layer tool != next layer tool, add a toolchange
				if (!left.equals(lastLayer)) {
					result.add(toolchange(ToolheadAlias.RIGHT, result.getLast(), ToolheadAlias.LEFT, left.peek()));
				}
				result.add(left.pop());
				lastLayer = left;
				if (left.peek().getHeight() == 999999) {
					ArrayList<String> finish = new ArrayList<String>();
					finish.add("M104 S0 " + ToolheadAlias.LEFT.getTcode()
										  + " (done with left extruder, cool it");
					result.add(new Layer(999999, finish));
				}
			}
			else if (right.peek().getHeight() < left.peek().getHeight())
			{
				// right has lower layer
				// if last layer tool != next layer tool, add a toolchange
				if (!right.equals(lastLayer)) {
					result.add(toolchange(ToolheadAlias.LEFT, result.getLast(), ToolheadAlias.RIGHT, right.peek()));

				}
				result.add(right.pop());
				lastLayer = right;
				if (right.peek().getHeight() == 999999) {
					ArrayList<String> finish = new ArrayList<String>();
					finish.add("M104 S0 " + ToolheadAlias.RIGHT.getTcode()
										  + " (done with right extruder, cool it");
					result.add(new Layer(999999, finish));
				}
			}
			else //equal height
			{
				if (lastLayer == null)
				{
					result.add(toolchange(ToolheadAlias.RIGHT, result.getLast(), ToolheadAlias.LEFT, left.peek()));
					lastLayer = left;
				}
				if (lastLayer == left)
				{
					if (left.peek().getHeight() == 999999) {
						ArrayList<String> finish = new ArrayList<String>();
						ArrayList<String> commands;
						commands = (ArrayList<String>)left.pop().getCommands();
						while (!commands.isEmpty()) {
							String s = commands.get(0);
							if (   s.startsWith("(")
								|| (   (!s.startsWith("M104"))
									&& !doneEnd)) {
								finish.add(s);
							}
							commands.remove(0);
						}
						result.add(new Layer(999999, finish));
						doneEnd = true;
					}
					else
					{
						result.add(left.pop());
						lastLayer = left;
						if (left.peek().getHeight() == 999999) {
							ArrayList<String> finish = new ArrayList<String>();
							finish.add("M104 S0 " + ToolheadAlias.LEFT.getTcode()
												+ " (done with left extruder, cool it");
							result.add(new Layer(999999, finish));
						}
					}
				}
				else
				{
					if (right.peek().getHeight() == 999999) {
						ArrayList<String> finish = new ArrayList<String>();
						ArrayList<String> commands;
						commands = (ArrayList<String>)right.pop().getCommands();
						while (!commands.isEmpty()) {
							String s = commands.get(0);
							if (   s.startsWith("(")
								|| (   (!s.startsWith("M104"))
									&& !doneEnd)) {
								finish.add(s);
							}
							commands.remove(0);
						}
						result.add(new Layer(999999, finish));
						doneEnd = true;
					}
					else {
						result.add(right.pop());
						if (right.peek().getHeight() == 999999) {
							ArrayList<String> finish = new ArrayList<String>();
							finish.add("M104 S0 " + ToolheadAlias.RIGHT.getTcode()
										+ " (done with right extruder, cool it");
							result.add(new Layer(999999, finish));
						}
					}
				}
			}
		}
		return result;
	}
}
