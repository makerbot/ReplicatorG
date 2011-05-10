package replicatorg.plugin.toolpath.skeinforge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.SavingTextField;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeOption;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;

public class PrintOMatic implements SkeinforgePreference {
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
	
	private double getScalingFactor() {
		// TODO: record the default values somewhere, so that we can retrieve them here!
		String value = Base.preferences.get(baseName + "materialType", null);
		
		double scalingFactor = 1;
		
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
	
	public PrintOMatic() {
		component = new JPanel(new MigLayout("ins 0, fillx, hidemode 1"));
		
		baseName = "replicatorg.skeinforge.printOMatic.";

		// Add a checkbox to switch print-o-matic on and off
		final String enabledName = baseName + "enabled";
		enabled = new JCheckBox("Use Print-O-Matic", Base.preferences.getBoolean(enabledName,true));
		enabled.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (enabledName != null) {
					Base.preferences.putBoolean(enabledName,enabled.isSelected());
					
					printOMatic.setVisible(enabled.isSelected());
				}
			}
		});
		
		component.add(enabled, "wrap, spanx");
		
		// Make a tabbed pane to sort basic and advanced components 
		printOMatic = new JTabbedPane();
		
		JComponent printPanel = new JPanel(new MigLayout("fillx"));
		JComponent materialPanel = new JPanel(new MigLayout("fillx"));
		JComponent machinePanel = new JPanel(new MigLayout("fillx"));
		
		addTextParameter(printPanel, "infillPercent",
				"Object infill (%)", "30",
				"0= hollow object, 100=solid object");
		
		addTextParameter(printPanel, "desiredLayerHeight",
					"Layer Height (mm)", "0.35",
					"Set the desired feedrate");

		addTextParameter(printPanel, "numberOfShells",
				"Number of shells:", "1",
				"Number of shells to add to the perimeter of an object. Set this to 0 if you are printing a model with thin features.");
		
		addTextParameter(printPanel, "desiredFeedrate",
				"Feedrate (mm/s)", "30",
				"slow: 0-20, default: 30, Fast: 40+");
		
		
		Vector<String> materialTypes = new Vector<String>();
		materialTypes.add("ABS");
		materialTypes.add("PLA");
		
		addDropDownParameter(materialPanel, "materialType",
				"Material type:", materialTypes,
				"Select the type of plastic to use during print");
		
		addTextParameter(materialPanel, "filamentDiameter",
				"Filament Diameter (mm)", "2.94",
				"measure feedstock");
		
		
		addTextParameter(machinePanel, "nozzleDiameter",
				"Nozzle Diameter (mm)", "0.5",
				"exit hole diameter");
		
		addTextParameter(machinePanel, "driveGearDiameter",
				"Drive Gear Diameter (mm)", "10.58",
				"measure at teeth");

		printOMatic.addTab("Settings", printPanel);
		printOMatic.addTab("Plastic", materialPanel);
		printOMatic.addTab("Extruder", machinePanel);
		component.add(printOMatic, "spanx");
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
	
	public List<SkeinforgeOption> getOptions() {
		
		List<SkeinforgeOption> options = new LinkedList<SkeinforgeOption>();

		if (enabled.isSelected()) {
		
			double infillRatio = getValue("infillPercent")/100;
			double flowRate = Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI*getValue("desiredFeedrate")*60/(Math.pow(getValue("filamentDiameter")/2,2)*Math.PI*(getScalingFactor()*getValue("driveGearDiameter")*Math.PI));
			double perimeterWidthOverThickness = calculateWidthOverThickness();
			double infillWidthOverThickness = calculateWidthOverThickness();
			double feedRate =getValue("desiredFeedrate");
			double layerHeight =getValue("desiredLayerHeight");
			double extraShellsOnAlternatingSolidLayer =getValue("numberOfShells");
			double extraShellsOnSparseLayer =getValue("numberOfShells");

			Base.logger.fine("Print-O-Matic settings:"
					+ "\n infillRatio=" + infillRatio
					+ "\n flowRate=" + flowRate
					+ "\n perimeterWidthOverThickness=" + perimeterWidthOverThickness
					+ "\n infillWidthOverThickness=" + infillWidthOverThickness
					+ "\n feedRate=" + feedRate
					+ "\n layerHeight=" + layerHeight
					+ "\n extraShellsOnAlternatingSolidLayer=" + extraShellsOnAlternatingSolidLayer
					+ "\n extraShellsOnSparseLayer=" + extraShellsOnSparseLayer
					);
			
			options.add(new SkeinforgeOption("fill.csv", "Infill Solidity (ratio):", Double.toString(infillRatio)));
			options.add(new SkeinforgeOption("speed.csv", "Flow Rate Setting (float):", Double.toString(flowRate)));
			options.add(new SkeinforgeOption("carve.csv", "Perimeter Width over Thickness (ratio):", Double.toString(perimeterWidthOverThickness)));
			options.add(new SkeinforgeOption("fill.csv", "Infill Width over Thickness (ratio):", Double.toString(infillWidthOverThickness)));
			options.add(new SkeinforgeOption("speed.csv", "Feed Rate (mm/s):", Double.toString(feedRate)));
			options.add(new SkeinforgeOption("carve.csv", "Layer Thickness (mm):", Double.toString(layerHeight)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Alternating Solid Layer (layers):", Double.toString(extraShellsOnAlternatingSolidLayer)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Sparse Layer (layers):", Double.toString(extraShellsOnSparseLayer)));
		}
		
		return options;
	}
}