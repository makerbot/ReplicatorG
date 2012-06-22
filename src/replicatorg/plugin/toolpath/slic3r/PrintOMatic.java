package replicatorg.plugin.toolpath.slic3r;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.SavingTextField;
import replicatorg.plugin.toolpath.slic3r.Slic3rGenerator.Slic3rOption;
import replicatorg.plugin.toolpath.slic3r.Slic3rGenerator.Slic3rPreference;

public class PrintOMatic implements Slic3rPreference {
	private JPanel component;
	private JCheckBox enabled;
	private String baseName;
	
	private class ComboListener implements ActionListener {
		final String name;
		final DefaultComboBoxModel input;
		
		public ComboListener(DefaultComboBoxModel input, String name) {
			this.input = input;
			this.name = name;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String value = (String)input.getSelectedItem().toString();
			
			if (name != null) {
				Base.logger.fine("here: " + name + "=" + value);
				Base.preferences.put(name, value);
			}
		}
	}
	
	// Note: This could be better represented as a separate class, however we want to be able to line up
	// the text boxes and input fields in the main print-o-matic dialog. So they stay here!
	private void addTextParameter(JComponent target, String name, String description, String defaultValue, String toolTip) {
		String fullName = baseName + name;
		String value = null;
		
		if (fullName != null) {
			value = Base.preferences.get(fullName, defaultValue);
			
			// Store it back so that we can be assured that it is set.
			Base.preferences.put(fullName, value);
		}
		target.add(new JLabel(description));
		
		JTextField input = new SavingTextField(fullName, value, 10);
		target.add(input, "wrap");
		
		if (toolTip != null) {
			// TODO: This is wrong.
			input.setToolTipText(toolTip);
		}
	}
	
	private void addDropDownParameter(JComponent target, String name, String description, Vector<String> options, String toolTip) {
		String fullName = baseName + name;
		String value = null;
		
		if (fullName != null) {
			value = Base.preferences.get(fullName, options.firstElement());
			
			// Store it back so that we can be assured that it is set.
			Base.preferences.put(fullName, value);
		}
		target.add(new JLabel(description));
		
		DefaultComboBoxModel model;
		model = new DefaultComboBoxModel(options);
		
		model.setSelectedItem(value);
		
		JComboBox input = new JComboBox(model);
		target.add(input, "wrap");
		
		input.addActionListener(new ComboListener(model, fullName));
		
		if (toolTip != null) {
			// TODO: This is wrong.
			input.setToolTipText(toolTip);
		}
		
	}
	
	private double getValue(String optionName) {
		// TODO: record the default values somewhere, so that we can retrieve them here!
		String value = Base.preferences.get(baseName + optionName, null);
		
		Base.logger.fine("Saved value for preference " + baseName + optionName + " is " + value);
		
		Double number = null;
		
		try {
			number = Double.valueOf(value);
		}
		catch (java.lang.NumberFormatException e) {
			Base.logger.severe("Print-O-Matic setting " + optionName + "does not contain a valid number, please correct this!");
		}
		
		return number;
	}
	
	private void setValue(String optionName, String value) {
		Base.preferences.put(baseName + optionName, value);
	}
	
	private double getScalingFactor() {
		// TODO: record the default values somewhere, so that we can retrieve them here!
		String value = Base.preferences.get(baseName + "materialType", null);
		
		double scalingFactor = 1;
		
		//MAGIC:
		if (value.equals("ABS")) {
			scalingFactor = .85;
		}
		else if (value.equals("PLA")) {
			scalingFactor = 1;
		}
		else {
			Base.logger.severe("Couldn't determine scaling factor for material " + value + ", defaulting to 1");
		}
		
		return scalingFactor;
	}
	
	
	JTabbedPane printOMatic;
	

	/*
	 * The default settings for different machines/nozzles
	 * 
	 * When adding anything to the defaults, make sure you update the "load defaults" panel
	 * so that it sets your new defaults
	 * 
	 * Also, whenever a new head is released, create a new subclass of this and make sure
	 * the field "defaults" points to an instance of it. 
	 *
	 */
	private abstract class Defaults {
		public String infillPercent;
		public String desiredLayerHeight;
		public String numberOfShells;
		public String desiredFeedrate;
		public String filamentDiameter;
		public String nozzleDiameter;
		public String driveGearDiameter;
	}
	

	// Double braces create a static block in which to declare the values of our variables
	private class Mk8Defaults extends Defaults {{
		infillPercent = "10";
		desiredLayerHeight = ".30";
		numberOfShells = "1";
		desiredFeedrate = "40";
		filamentDiameter = "1.82";
		nozzleDiameter = ".4";
		driveGearDiameter = "10.58";
	}}

	// Double braces create a static block in which to declare the values of our variables
	private class Mk7Defaults extends Defaults {{
		infillPercent = "15";
		desiredLayerHeight = ".30";
		numberOfShells = "1";
		desiredFeedrate = "30";
		filamentDiameter = "1.82";
		nozzleDiameter = ".4";
		driveGearDiameter = "10.58";
	}}
	
	private class Mk6Defaults extends Defaults {{
		infillPercent = "15";
		desiredLayerHeight = ".30";
		numberOfShells = "1";
		desiredFeedrate = "30";
		filamentDiameter = "2.94";
		nozzleDiameter = ".5";
		driveGearDiameter = "10.58";
	}}
	
	private class Mk6NewStyleDefaults extends Mk6Defaults {{
		nozzleDiameter = ".4";
	}}
	
	// This should be kept up to date, so that we always default to the newest kind of head
	private Defaults defaults = new Mk8Defaults();
	
	
	private JComponent printPanel() {

		JComponent printPanel = new JPanel(new MigLayout("fillx"));
		
		addTextParameter(printPanel, "infillPercent",
				"Object infill (%)", defaults.infillPercent,
				"0= hollow object, 100=solid object");
		
		addTextParameter(printPanel, "desiredLayerHeight",
				"Layer Height (mm)", defaults.desiredLayerHeight,
				"Sets thickness for plastic layers");

		addTextParameter(printPanel, "numberOfShells",
				"Number of shells:", defaults.numberOfShells,
				"Number of shells to add to the perimeter of an object. Set this to 0 if you are printing a model with thin features");
		
		addTextParameter(printPanel, "desiredFeedrate",
				"Feedrate (mm/s)", defaults.desiredFeedrate,
				"slow: 0-20, default: 30, Fast: 40+");
		
		return printPanel;
	}
	
	private JComponent materialPanel() {

		JComponent materialPanel = new JPanel(new MigLayout("fillx"));
		
		Vector<String> materialTypes = new Vector<String>();
		materialTypes.add("ABS");
		materialTypes.add("PLA");

		addDropDownParameter(materialPanel, "materialType",
				"Material type:", materialTypes,
				"Select the type of plastic to use during print");
		
		addTextParameter(materialPanel, "filamentDiameter",
				"Filament Diameter (mm)", defaults.filamentDiameter,
				"measure feedstock");
		
		return materialPanel;
	}
	
	private JComponent machinePanel() {

		JComponent machinePanel = new JPanel(new MigLayout("fillx"));
		
		addTextParameter(machinePanel, "nozzleDiameter",
				"Nozzle Diameter (mm)", defaults.nozzleDiameter,
				"exit hole diameter");
		
		addTextParameter(machinePanel, "driveGearDiameter",
				"Drive Gear Diameter (mm)", defaults.driveGearDiameter,
				"measure at teeth");
		
		return machinePanel;
	}
	
	private JComponent defaultsPanel() {

		JComponent defaultsPanel = new JPanel(new MigLayout("fillx"));

		final JButton mk8 = new JButton("Load Mk8 Defaults");
		final JButton mk7 = new JButton("Load Mk7 Defaults");
		final JButton mk6 = new JButton("Load Mk6 Defaults (0.5 nozzle)");
		final JButton mk6ns = new JButton("Load Mk6 Defaults (0.4 nozzle)");

		ActionListener loadDefaults = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent evt) {
				Defaults def = null;
				
				// Select the correct set of defaults based on the button pressed
				if(evt.getSource() == mk8)
					def = new Mk8Defaults();
				else if(evt.getSource() == mk7)
					def = new Mk7Defaults();
				else if(evt.getSource() == mk6)
					def = new Mk6Defaults();
				else if(evt.getSource() == mk6ns)
					def = new Mk6NewStyleDefaults();
				
				// Set all the values based on the selected default
				// Keep this up to date! if the set of defaults changes, so does this set of calls!
				setValue("infillPercent", def.infillPercent);
				setValue("desiredLayerHeight", def.desiredLayerHeight);
				setValue("numberOfShells", def.numberOfShells);
				setValue("desiredFeedrate", def.desiredFeedrate);
				setValue("filamentDiameter", def.filamentDiameter);
				setValue("nozzleDiameter", def.nozzleDiameter);
				setValue("driveGearDiameter", def.driveGearDiameter);
					
				// Refresh the other three tabs
				printOMatic.removeAll();
				makeTabs();
			}
		};
		mk8.addActionListener(loadDefaults);
		mk7.addActionListener(loadDefaults);
		mk6.addActionListener(loadDefaults);
		mk6ns.addActionListener(loadDefaults);
		
		defaultsPanel.add(mk8, "growx, wrap");
		defaultsPanel.add(mk7, "growx, wrap");
		defaultsPanel.add(mk6, "growx, Wrap");
		defaultsPanel.add(mk6ns, "growx, Wrap");
		
		return defaultsPanel;
	}

	// Handles the creation of the various tabs and adds them to printOMatic
	private void makeTabs()
	{
		printOMatic.addTab("Settings", printPanel());
		printOMatic.addTab("Plastic", materialPanel());
		printOMatic.addTab("Extruder", machinePanel());
		printOMatic.addTab("Defaults", defaultsPanel());
	}
	
	public PrintOMatic() {
		component = new JPanel(new MigLayout("ins 0, fillx, hidemode 1"));
		
		baseName = "replicatorg.slic3r.printOMatic.";

		// Add a checkbox to switch print-o-matic on and off
		final String enabledName = baseName + "enabled";
		enabled = new JCheckBox("Use Print-O-Matic (stepper extruders only)", Base.preferences.getBoolean(enabledName,true));
		enabled.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (enabledName != null) {
					Base.preferences.putBoolean(enabledName,enabled.isSelected());
					printOMatic.setVisible(enabled.isSelected());
					printOMatic.invalidate();
					Window w = SwingUtilities.getWindowAncestor(printOMatic);
					w.pack();
				}
			}
		});
		
		component.add(enabled, "wrap, spanx");
		
		// Make a tabbed pane to sort basic and advanced components 
		printOMatic = new JTabbedPane();
		
		// Handles the creation of the various tabs and adds them to printOMatic
		makeTabs();
		
		component.add(printOMatic, "spanx,hidemode 1");
		printOMatic.setVisible(enabled.isSelected());
	}

	public JComponent getUI() { return component; }
	
	// Check the options to determine if they are in an acceptable range. Return null if
	// everything is ok, or a string describing the error if they are not ok.
	public String valueSanityCheck() {
		
		if (enabled.isSelected()) {
			// Check that width/thickness is ok
			if (calculateWidthOverThickness() > 1.8) {
				return "Layer height is smaller than recommended for the specified nozzle. Try increasing the layer height, or changing to a smaller nozzle.";
			}
			if (calculateWidthOverThickness() < 1.2) {
				return "Layer height is larger than recommended for the specified nozzle. Try decreasing the layer height, or changing to a larger nozzle.";
			}
			
		}
		
		return null;
	}
	
	public double calculateWidthOverThickness() {
		return ((Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI)/getValue("desiredLayerHeight"))/getValue("desiredLayerHeight");
	}
	
	public List<Slic3rOption> getOptions() {
		
		List<Slic3rOption> options = new LinkedList<Slic3rOption>();

		if (enabled.isSelected()) {
		
			double infillRatio = getValue("infillPercent")/100;
			double flowRate = Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI*getValue("desiredFeedrate")*60/(Math.pow(getValue("filamentDiameter")/2,2)*Math.PI*(getScalingFactor()*getValue("driveGearDiameter")*Math.PI));
			//double perimeterWidthOverThickness = calculateWidthOverThickness();
			//double infillWidthOverThickness = calculateWidthOverThickness();
			double feedRate =getValue("desiredFeedrate");
			double layerHeight =getValue("desiredLayerHeight");
			//double extraShellsOnAlternatingSolidLayer =getValue("numberOfShells");
			//double extraShellsOnBase =getValue("numberOfShells");
			//double extraShellsOnSparseLayer =getValue("numberOfShells");

			Base.logger.fine("Print-O-Matic settings:"
					+ "\n infillRatio=" + infillRatio
					+ "\n flowRate=" + flowRate
					//+ "\n perimeterWidthOverThickness=" + perimeterWidthOverThickness
					//+ "\n infillWidthOverThickness=" + infillWidthOverThickness
					+ "\n feedRate=" + feedRate
					+ "\n layerHeight=" + layerHeight
					//+ "\n extraShellsOnAlternatingSolidLayer=" + extraShellsOnAlternatingSolidLayer
					//+ "\n extraShellsOnBase=" + extraShellsOnBase
					//+ "\n extraShellsOnSparseLayer=" + extraShellsOnSparseLayer
					);
			
			options.add(new Slic3rOption("--fill-density",Double.toString(infillRatio)));
			options.add(new Slic3rOption("--infill-speed",Double.toString(flowRate)));
			//options.add(new Slic3rOption("carve.csv", "Perimeter Width over Thickness (ratio):", Double.toString(perimeterWidthOverThickness)));
			//options.add(new Slic3rOption("fill.csv", "Infill Width over Thickness (ratio):", Double.toString(infillWidthOverThickness)));
			options.add(new Slic3rOption("--travel-speed", Double.toString(feedRate)));
			options.add(new Slic3rOption("--layer-height",Double.toString(layerHeight)));
			//options.add(new Slic3rOption("fill.csv", "Extra Shells on Alternating Solid Layer (layers):", Double.toString(extraShellsOnAlternatingSolidLayer)));
			//options.add(new Slic3rOption("fill.csv", "Extra Shells on Base (layers):", Double.toString(extraShellsOnBase)));
			//options.add(new Slic3rOption("fill.csv", "Extra Shells on Sparse Layer (layers):", Double.toString(extraShellsOnSparseLayer)));
		}
		
		return options;
	}
	
	@Override
	public String getName() {
		return "Print-O-Matic";
	}
	
}
