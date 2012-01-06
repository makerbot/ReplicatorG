package replicatorg.plugin.toolpath.skeinforge;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.gcode.MutableGCodeSource;
import replicatorg.machine.model.MachineType;
import replicatorg.machine.model.ToolheadAlias;
import replicatorg.model.BuildCode;
import replicatorg.model.GCodeSource;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeBooleanPreference;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeOption;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;


public class SkeinforgePostProcessor {
	
	private class PostProcessorPreference implements SkeinforgePreference {
		private final JPanel panel = new JPanel(new MigLayout("fill, ins 0"));
		private final SkeinforgePostProcessor processor;
		
		public PostProcessorPreference(SkeinforgePostProcessor spp) {
			processor = spp;
		}
		
		/**
		 * If some PostProcessor setting would affect the displayed preferences, 
		 * then this should be called in its setter
		 */
		public void refreshPreferences() {
			panel.removeAll();
			
			if(multiHead && !dualstruding)
			{
				Vector<String> extruders = new Vector<String>();
				extruders.add(ToolheadAlias.LEFT.guiName);
				extruders.add(ToolheadAlias.RIGHT.guiName);

				String value = Base.preferences.get("replicatorg.skeinforge.toolheadOrientation", extruders.firstElement());
				
				final DefaultComboBoxModel model= new DefaultComboBoxModel(extruders);
				
				JComboBox input = new JComboBox(model);
				panel.add(new JLabel("Extruder: "), "split");
				panel.add(input, "wrap");

				ActionListener toolSelected = new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(model.getSelectedItem().equals(ToolheadAlias.LEFT.guiName)) {
							processor.toolheadTarget = ToolheadAlias.LEFT;
							Base.preferences.put("replicatorg.skeinforge.toolheadOrientation", ToolheadAlias.LEFT.guiName);
						}
						else if(model.getSelectedItem().equals(ToolheadAlias.RIGHT.guiName)) {
							processor.toolheadTarget = ToolheadAlias.RIGHT;
							Base.preferences.put("replicatorg.skeinforge.toolheadOrientation", ToolheadAlias.RIGHT.guiName);
						}
					}
				};
				input.addActionListener(toolSelected);
				toolSelected.actionPerformed(null);

				model.setSelectedItem(value);
				
				input.setToolTipText("select which extruder this gcode prints on");
			}
		}
		
		@Override
		public JComponent getUI() {
			return panel;
		}
		@Override
		public List<SkeinforgeOption> getOptions() {
			return new ArrayList<SkeinforgeOption>();
		}
		@Override
		public String valueSanityCheck() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public String getName() {
			return "Post-Processor options";
		}
	}
	
	private final SkeinforgeGenerator generator;
	
	private MutableGCodeSource source;
	
	// options:
	private MutableGCodeSource startCode = null;
	private MutableGCodeSource endCode = null;
	private ToolheadAlias toolheadTarget = null;
	private MachineType machineType = null;
	private boolean dualstruding = false;
	private boolean prependStart = false;
	private boolean appendEnd = false;
	private boolean prependMetaInfo = false;
	private boolean multiHead = false;
	private boolean addPercentages = false;
	private PostProcessorPreference ppp;
	
	public SkeinforgePostProcessor(SkeinforgeGenerator generator)
	{
		this.generator = generator;
		
		// This allows us to display stuff in the configuration dialog,
		//and to send option overrides to skeinforge
		ppp = new PostProcessorPreference(this);
		generator.getPreferences().add(0, ppp);
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
					prependStart = true;
					appendEnd = true;
				}
			}
		}
		
		// Load our code to a source iterator
		source = new MutableGCodeSource(generator.output.file);
		
		if(!dualstruding)
		{
			if(prependStart)
				runPrepend(startCode);
			if(appendEnd)
				runAppend(endCode);
			
			if(toolheadTarget != null)
				runToolheadSwap(toolheadTarget);
		}
		
		if(addPercentages)
		{
			interlacePercentageUpdates();
		}
		
		if(prependMetaInfo)
		{
			MutableGCodeSource metaInfo = new MutableGCodeSource();
			
			metaInfo.add("(** This GCode was generated by ReplicatorG "+Base.VERSION_NAME+" **)");
			//TRICKY: calling a static method on an instance of a class is considered bad practice,
			//				but I'm not sure how to access displayName without it
			metaInfo.add("(*  using "+generator.displayName+"  *)");
			metaInfo.add("(*  for a "+machineType.getName()+"  *)");
			metaInfo.add("(*  on "+/*Calendar.getInstance().toString()*/"unknown date"+"  *)");
			
			runPrepend(metaInfo);
		}
		//Write the modified source back to our file
		source.writeToFile(generator.output.file);
		
		return generator.output;
	}
	
	/// Scans gcode for layer start/ends. Adds gcode for approx % done 
	/// by that layer via using line count
	private void interlacePercentageUpdates()
	{
		int index = 0;
		int sourceSize = source.getLineCount();
		MutableGCodeSource newSource = new MutableGCodeSource();
		for(String line : source)
		{
			if( line.startsWith("(<layer>") )
			{
				int percentDone = (int)(index*100)/sourceSize;
				newSource.add("M73 P"+percentDone+" (display progress)");
			}
			newSource.add(line);
			index++;
		}
		source = newSource;
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
	
	public void enableDualstrusion()
	{
		dualstruding = true;
		
		List<SkeinforgePreference> prefs = generator.getPreferences();
		
		// This allows us to display stuff in the configuration dialog,
		//and to send option overrides to skeinforge
		prefs.add(0, new SkeinforgePreference(){
			SkeinforgeBooleanPreference outlineActive;
			SkeinforgeBooleanPreference coolActive;
			//Static block
			{
				outlineActive = new SkeinforgeBooleanPreference("Outline Active", 
						"skeinforge.dualstrusion.outlineActive", false, "<html>Having Outline active for any layer" +
								" but the first layer<br/>for the first toolhead can damage dualstrusion prints.</html>");
				outlineActive.addNegateableOption(new SkeinforgeOption("outline.csv", "Activate Outline", "True"));
				

				coolActive = new SkeinforgeBooleanPreference("Cool Active", 
						"skeinforge.dualstrusion.coolActive", false, "<html>Cool makes the tool move slowly on very small " +
						"layers,<br/> with dualstrusion, those layers are usually supported by the other half of the print.</html>");
				coolActive.addNegateableOption(new SkeinforgeOption("cool.csv", "Activate Cool", "True"));
			}
			@Override
			public JComponent getUI() {
				JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				panel.setLayout(new MigLayout("fillx, filly"));
				
				panel.add(new JLabel("Dualstruding..."), "growx, wrap");
				panel.add(outlineActive.getUI(), "growx, wrap");
				panel.add(coolActive.getUI(), "growx, wrap");
				
				return panel;
			}
			@Override
			public List<SkeinforgeOption> getOptions() {
				List<SkeinforgeOption> result = new ArrayList<SkeinforgeOption>();
				result.add(new SkeinforgeOption("preface.csv", "Name of Start File:", ""));
				result.add(new SkeinforgeOption("preface.csv", "Name of End File:", ""));
				result.addAll(outlineActive.getOptions());
				result.addAll(coolActive.getOptions());
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
	
	public void setToolheadTarget(ToolheadAlias tool)
	{
		toolheadTarget = tool;
	}
	
	public void setMachineType(MachineType type)
	{
		machineType = type;
	}

	public void setStartCode(GCodeSource source)
	{
		if(source instanceof MutableGCodeSource)
			startCode = (MutableGCodeSource)source;
		else
			startCode = new MutableGCodeSource(source);
	}
	
	public void setEndCode(GCodeSource source)
	{
		if(source instanceof MutableGCodeSource)
			endCode = (MutableGCodeSource)source;
		else
			endCode = new MutableGCodeSource(source);
	}
	
	public void setPrependStart(boolean doPrepend)
	{
		prependStart = doPrepend;
	}
	
	public void setAppendEnd(boolean doAppend)
	{
		appendEnd = doAppend;
	}

	public void setPrependMetaInfo(boolean doPrepend)
	{
		prependMetaInfo = doPrepend;
	}
	public void setMultiHead(boolean isMulti)
	{
		multiHead = isMulti;
		ppp.refreshPreferences();
	}
	public void setAddPercentages(boolean doAdd)
	{
		addPercentages = doAdd;
	}
}
