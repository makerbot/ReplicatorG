package replicatorg.plugin.toolpath.skeinforge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JLabel;

import replicatorg.app.gcode.MutableGCodeSource;
import replicatorg.machine.model.ToolheadAlias;
import replicatorg.model.BuildCode;
import replicatorg.model.GCodeSource;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeOption;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;


public class SkeinforgePostProcessor {

	/* try to keep these as descriptive as possible
	 * Also, the way this works right now you can specify conflicting options.
	 *   This can be fixed by switching to enums or something. or just doing a check
	 */
	public static final String TARGET_TOOLHEAD_LEFT = "target-toolhead-left";
	public static final String TARGET_TOOLHEAD_RIGHT = "target-toolhead-right";
	public static final String TARGET_TOOLHEAD_DUAL= "target-toolhead-dual";

	public static final String PREPEND_START = "prepend-start";
	public static final String REPLACE_START = "replace-start";
	public static final String REPLACE_END = "replace-end";
	public static final String APPEND_END = "append-end";

	public static final String MACHINE_TYPE_REPLICATOR = "machine-type-replicator";
	public static final String MACHINE_TYPE_TOM = "machine-type-tom";
	public static final String MACHINE_TYPE_CUPCAKE = "machine-type-cupcake";
	
	private final SkeinforgeGenerator generator;
	private final Set<String> operations;
	
	private MutableGCodeSource source;
	
	//TODO:
	// Because I'm trying to do this quickly, I'm just throwing these in here. A better way to do it
	// would be to replace the Set<String> with a Set<SFPostProcessorOptions> that would say, 
	// for instance, {PREPEND, File toPrepend} or {REPLACE, GCodeSource toRemove, GCodeSource toAdd}
	private MutableGCodeSource startCode, endCode;
	public SkeinforgePostProcessor(SkeinforgeGenerator generator, MutableGCodeSource startCode, MutableGCodeSource endCode, String...ops)
	{
		this(generator, startCode, endCode, new TreeSet<String>(Arrays.asList(ops)));
	}
	public SkeinforgePostProcessor(SkeinforgeGenerator generator, MutableGCodeSource startCode, MutableGCodeSource endCode, Set<String> operations)
	{
		this.generator = generator;
		this.operations = operations;
		this.startCode = startCode;
		this.endCode = endCode;

		// Check to see if we need to do anything based on selected prefs
		List<SkeinforgePreference> prefs = generator.getPreferences();
	
		// If we're doing dualstrusion we always remove the beginning and end code
		//this actually creates some non-intuitive/unclear behavior for the user
		if(operations.contains(TARGET_TOOLHEAD_DUAL))
		{
			prefs.add(0, new SkeinforgePreference(){
				@Override
				public JComponent getUI() {
					return new JLabel("Dualstruding...");
				}
				@Override
				public List<SkeinforgeOption> getOptions() {
					List<SkeinforgeOption> result = new ArrayList<SkeinforgeOption>();
					result.add(new SkeinforgeOption("preface.csv", "Name of Start File:", ""));
					result.add(new SkeinforgeOption("preface.csv", "Name of End File:", ""));
					result.add(new SkeinforgeOption("outline.csv", "Activate Outline", "False"));
					return result;
				}
				@Override
				public String valueSanityCheck() {
					// TODO Auto-generated method stub
					return null;
				}
				@Override
				public String getName() {
					return "Dualstrusion options";
				}
			});
		}
	}
	
	public void addOperation(String operation)
	{
		operations.add(operation);
	}
	
	public BuildCode runPostProcessing()
	{

		// Check to see if we need to do anything based on selected prefs
		List<SkeinforgePreference> prefs = generator.getPreferences();
		
		// look for prefs we care about
		for(SkeinforgePreference sp : prefs)
		{
			// This works because we know that in ToolpathGeneratorFactory options
			// are only added for this pref if it's true
			if(sp.getName().equals("Use machine-specific start/end gcode"))
			{
				if(!sp.getOptions().isEmpty())
				{
					operations.add(PREPEND_START);
					operations.add(APPEND_END);
				}
			}
		}
		// Load our code to a source iterator
		source = new MutableGCodeSource(generator.output.file);

		System.out.println("***********************************");
		for(String s : operations)
			System.out.println(s);
		
		if(operations.contains(TARGET_TOOLHEAD_DUAL))
			;//NOP
		else if(operations.contains(TARGET_TOOLHEAD_LEFT))
			runToolheadSwap(ToolheadAlias.LEFT);
		else if(operations.contains(TARGET_TOOLHEAD_RIGHT))
			runToolheadSwap(ToolheadAlias.RIGHT);
		
		//Not sure if the start/end replacement should come before or after the toolhead swap
//		if(options.contains(REPLACE_START))
//			runStartReplacement();
//		if(options.contains(REPLACE_END))
//			runEndReplacement();
		if(! operations.contains(TARGET_TOOLHEAD_DUAL))
		{
			if(operations.contains(PREPEND_START))
				runPrepend(startCode);
			if(operations.contains(APPEND_END))
				runAppend(endCode);
		}

		System.out.println("***********************************");
		
		//Write the modified source back to our file
		source.writeToFile(generator.output.file);
		
		return generator.output;
	}
	
	private void runToolheadSwap(ToolheadAlias switchTo)
	{
		System.out.println("runToolheadSwap");
		source.changeToolhead(switchTo);
	}
	
	private void runPrepend(GCodeSource newCode)
	{
		source.add(0, newCode);
	}
	
	private void runAppend(GCodeSource newCode)
	{
		source.add(newCode);
	}
}
