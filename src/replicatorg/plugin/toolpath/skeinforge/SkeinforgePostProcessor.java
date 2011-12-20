package replicatorg.plugin.toolpath.skeinforge;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import replicatorg.app.gcode.GCodeHelper;
import replicatorg.machine.model.ToolheadAlias;
import replicatorg.machine.model.Toolheads;
import replicatorg.model.BuildCode;
import replicatorg.model.GCodeSource;

public class SkeinforgePostProcessor {

	/* try to keep these as descriptive as possible
	 * Also, the way this works right now you can specify conflicting options.
	 *   This can be fixed by switching to enums or something. or just doing a check
	 */
	public static final String TARGET_TOOLHEAD_LEFT = "target-toolhead-left";
	public static final String TARGET_TOOLHEAD_RIGHT = "target-toolhead-right";
	public static final String TARGET_TOOLHEAD_DUAL= "target-toolhead-dual";

	public static final String REPLACE_START = "replace-start";
	public static final String REPLACE_END = "replace-end";

	public static final String MACHINE_TYPE_REPLICATOR = "machine-type-replicator";
	public static final String MACHINE_TYPE_TOM = "machine-type-tom";
	public static final String MACHINE_TYPE_CUPCAKE = "machine-type-cupcake";
	
	private final SkeinforgeGenerator generator;
	private final Set<String> options;
	
	private GCodeSource source;
	
	public SkeinforgePostProcessor(SkeinforgeGenerator generator, String...ops)
	{
		this(generator, new TreeSet<String>(Arrays.asList(ops)));
	}
	public SkeinforgePostProcessor(SkeinforgeGenerator generator, Set<String> options)
	{
		this.generator = generator;
		this.options = options;
	}
	
	public BuildCode runPostProcessing()
	{
		// Load our code to a source iterator
		source = GCodeHelper.readFiletoGCodeSource(generator.output.file);

		System.out.println("***********************************");
		for(String s : options)
			System.out.println(s);
		
		if(options.contains(TARGET_TOOLHEAD_DUAL))
			;//NOP
		else if(options.contains(TARGET_TOOLHEAD_LEFT))
			runToolheadSwap(ToolheadAlias.LEFT);
		else if(options.contains(TARGET_TOOLHEAD_RIGHT))
			runToolheadSwap(ToolheadAlias.RIGHT);
		
		//Not sure if the start/end replacement should come before or after the toolhead swap
		if(options.contains(REPLACE_START))
			runStartReplacement();
		if(options.contains(REPLACE_END))
			runEndReplacement();

		System.out.println("***********************************");
		
		//Write the modified source back to our file
		GCodeHelper.writeGCodeSourcetoFile(source, generator.output.file);
		
		return generator.output;
	}
	
	private void runToolheadSwap(ToolheadAlias switchTo)
	{
		System.out.println("runToolheadSwap");

		source = GCodeHelper.changeToolHead(source, switchTo);
	}
	
	private void runStartReplacement()
	{
		System.out.println("runStartReplacement");
		//DANGER: start and end codes are not required to be called start/end.gcode
		// we can look up what the name of the thing being used is, but it's in 
		// different places in SF-35 and SF-44
		GCodeSource oldStart = GCodeHelper.readFiletoGCodeSource(new File(generator.profile +"/alterations/start.gcode"));
		
		GCodeSource newStart = null;
		if(options.contains(MACHINE_TYPE_REPLICATOR))
			newStart = GCodeHelper.readFiletoGCodeSource(new File("machines/replicator/start.gcode"));
		
		if(newStart == null)
			return;

		source = GCodeHelper.replaceStartOrEndGCode(source, oldStart, newStart);
	}
	
	private void runEndReplacement()
	{
		System.out.println("runEndReplacement");
		//DANGER: start and end codes are not required to be called start/end.gcode
		// we can look up what the name of the thing being used is, but it's in 
		// different places in SF-35 and SF-44
		GCodeSource oldEnd = GCodeHelper.readFiletoGCodeSource(new File(generator.profile +"/alterations/end.gcode"));

		GCodeSource newEnd = null;
		if(options.contains(MACHINE_TYPE_REPLICATOR))
			newEnd = GCodeHelper.readFiletoGCodeSource(new File("machines/replicator/end.gcode"));
		
		if(newEnd == null)
			return;
		
		source = GCodeHelper.replaceStartOrEndGCode(source, oldEnd, newEnd);
	}
	
}
