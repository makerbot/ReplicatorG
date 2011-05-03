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
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeOption;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;

public class PrintOMatic implements SkeinforgePreference {
	private JPanel component;
	private JCheckBox enabled;
	private String baseName;
	
	private class StoringActionListener implements ActionListener {
		final String name;
		final JTextField input;
		
		public StoringActionListener(JTextField input, String name) {
			this.input = input;
			this.name = name;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String value = (String)input.getText();
			
			if (name != null) {
				Base.logger.fine("here: " + name + "=" + value);
				Base.preferences.put(name, value);
			}
		}
	}
	
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
		
		JTextField input = new JTextField(value, 10);
		target.add(input, "wrap");
		
		input.addActionListener(new StoringActionListener(input, fullName));
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
		
		Base.logger.fine("Getting preference for " + baseName + optionName);
		
		return Double.valueOf(value);
	}
	
	
	JTabbedPane printOMatic;
	
	public PrintOMatic() {
		component = new JPanel(new MigLayout());
		
		baseName = "replicatorg.skeinforge.printOMatic.";
		

//		SkeinforgeBooleanPreference printOMaticPref =
//			new SkeinforgeBooleanPreference("Use Print-O-Matic",
//					"replicatorg.skeinforge.printOMaticPref", true,
//					"If this option is checked, skeinforge will use the values below to control the print");
//		printOMaticPref.addNegateableOption(new SkeinforgeOption("raft.csv", "Add Raft, Elevate Nozzle, Orbit and Set Altitude:", "true"));
//		prefs.add(printOMaticPref);

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
		
		JComponent basicPanel = new JPanel(new MigLayout());
		JComponent advancedPanel = new JPanel(new MigLayout());
		 
		addTextParameter(basicPanel, "desiredFeedrate",
					"Desired Feedrate (mm/s)", "30",
					"slow: 0-20, default: 30, Fast: 40+");
		
		addTextParameter(basicPanel, "desiredLayerHeight",
					"Desired Layer Height (mm)", "0.35",
					"Set the desired feedrate");

		addTextParameter(basicPanel, "filamentDiameter",
				"Filament Diameter (mm)", "2.98",
				"measure feedstock");

		addTextParameter(advancedPanel, "nozzleDiameter",
				"Nozzle Diameter (mm)", "0.5",
				"exit hole diameter");
		
		addTextParameter(advancedPanel, "driveGearDiameter",
				"Drive Gear Diameter (mm)", "10.58",
				"measure at teeth");
		
//		addTextParameter(advancedPanel, "driveGearScalingFactor",
//				"Gear Diameter Scaling Factor", "0.85",
//				"ABS = 0.85, PLA = 1");
		
		Vector<String> scalingOptions = new Vector<String>();
		scalingOptions.add("ABS = 0.85");
		scalingOptions.add("PLA = 1");
		
		addDropDownParameter(advancedPanel, "driveGearScalingFactor",
				"Gear Diameter Scaling Factor", scalingOptions,
				"0.85 (ABS), 1 (PLA)");
		

		addTextParameter(advancedPanel, "modelHasThinFeatures",
				"No thin features", "1",
				"Model does not contain any thin features (<2.5mm) (1=true, 0=false)");

		printOMatic.addTab("Basic", basicPanel);
		printOMatic.addTab("Advanced", advancedPanel);
		component.add(printOMatic);
		printOMatic.setVisible(enabled.isSelected());
	}

	public JComponent getUI() { return component; }
	
	public List<SkeinforgeOption> getOptions() {
		List<SkeinforgeOption> options = new LinkedList<SkeinforgeOption>();

		if (enabled.isSelected()) {
		
			double flowRate = Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI*getValue("desiredFeedrate")*60/(Math.pow(getValue("filamentDiameter")/2,2)*Math.PI*(getValue("driveGearScalingFactor")*getValue("driveGearDiameter")*Math.PI));
			double perimeterWidthOverThickness =((Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI)/getValue("desiredLayerHeight"))/getValue("desiredLayerHeight");
			double infillWidthOverThickness =((Math.pow(getValue("nozzleDiameter")/2,2)*Math.PI)/getValue("desiredLayerHeight"))/getValue("desiredLayerHeight");
			double feedRate =getValue("desiredFeedrate");
			double layerHeight =getValue("desiredLayerHeight");
			double extraShellsOnAlternatingSolidLayer =getValue("modelHasThinFeatures");
			double extraShellsOnSparseLayer =getValue("modelHasThinFeatures");

			Base.logger.fine("Print-O-Matic settings:"
					+ "\n flowRate=" + flowRate
					+ "\n perimeterWidthOverThickness=" + perimeterWidthOverThickness
					+ "\n infillWidthOverThickness=" + infillWidthOverThickness
					+ "\n feedRate=" + feedRate
					+ "\n layerHeight=" + layerHeight
					+ "\n extraShellsOnAlternatingSolidLayer=" + extraShellsOnAlternatingSolidLayer
					+ "\n extraShellsOnSparseLayer=" + extraShellsOnSparseLayer
					);
						
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