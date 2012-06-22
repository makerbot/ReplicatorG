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

public class PrintOMatic5D implements Slic3rPreference {
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

	
	private void addBooleanParameter(JComponent target, String name, String description, boolean defaultValue, String toolTip) {
		String fullName = baseName + name;
		boolean isSet = false;
		
		if (fullName != null) {
			isSet = Base.preferences.getBoolean(fullName, defaultValue);
			
			// Store it back so that we can be assured that it is set.
			Base.preferences.putBoolean(fullName, isSet);
		}
		target.add(new JLabel(description));
		
		JCheckBox input = new JCheckBox("", isSet);
		target.add(input, "wrap");
		
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
	
	private boolean getBooleanValue(String optionName) {
		// TODO: record the default values somewhere, so that we can retrieve them here!
		boolean value = Base.preferences.getBoolean(baseName + optionName, true);
		
		Base.logger.fine("Saved value for preference " + baseName + optionName + " is " + value);
		
		return value;
	}
	
	private String getStringValue(String optionName) {
		// TODO: record the default values somewhere, so that we can retrieve them here!
		String value = Base.preferences.get(baseName + optionName, null);
		
		Base.logger.fine("Saved value for preference " + baseName + optionName + " is " + value);
		
		return value;
	}
	
//	private double getScalingFactor() {
//		// TODO: record the default values somewhere, so that we can retrieve them here!
//		String value = Base.preferences.get(baseName + "materialType", null);
//		
//		double scalingFactor = 1;
//		
//		if (value.equals("ABS")) {
//			scalingFactor = .85;
//		}
//		else if (value.equals("PLA")) {
//			scalingFactor = 1;
//		}
//		else {
//			Base.logger.severe("Couldn't determine scaling factor for material " + value + ", defaulting to 1");
//		}
//		
//		return scalingFactor;
//	}
	
	
	JTabbedPane printOMatic5D;
	
	private JComponent printPanel() {
		
		JComponent printPanel = new JPanel(new MigLayout("fillx"));

		addTextParameter(printPanel, "infillPercent",
				"Object infill (%)", "10",
				"0= hollow object, 100=solid object");
		
		addTextParameter(printPanel, "desiredLayerHeight",
				"Layer Height (mm)", "0.27",
				"Set the desired layer height");

		addTextParameter(printPanel, "numberOfShells",
				"Number of shells:", "1",
				"Number of shells to add to the perimeter of an object. Set this to 0 if you are printing a model with thin features.");

		addTextParameter(printPanel, "desiredFeedrate",
				"Feedrate (mm/s)", "40",
				"slow: 0-20, default: 30, Fast: 40+");
		
		addTextParameter(printPanel, "travelFeedrate",
				"Travel Feedrate", "55",
				"slow: 0-20, default: 30, Fast: 40+");
		
		return printPanel;
	}
	
	private JComponent materialPanel() {
		
		JComponent materialPanel = new JPanel(new MigLayout("fillx"));
		
//		Vector<String> materialTypes = new Vector<String>();
//		materialTypes.add("ABS");
//		materialTypes.add("PLA");
//		
//		addDropDownParameter(materialPanel, "materialType",
//				"Material type:", materialTypes,
//				"Select the type of plastic to use during print");
		
		addTextParameter(materialPanel, "filamentDiameter",
				"Filament Diameter (mm)", "2.95",
				"measure feedstock");
                
                // TODO: Tie the materialType to this text box, so that switching the puldown changes this default
//		addTextParameter(materialPanel, "packingDensity",
//				"Packing Density", "85",
//				"% Final Volume");
		
		return materialPanel;
	}
	
	private JComponent machinePanel() {

		JComponent machinePanel = new JPanel(new MigLayout("fillx"));
		
		addTextParameter(machinePanel, "desiredPathWidth",
					"Nozzle Diameter (mm)", "0.4",
					"Set the desired path width");
		
		return machinePanel;
	}

	private JComponent defaultsPanel() {

		JComponent defaultsPanel = new JPanel(new MigLayout("fillx"));

		final JButton def = new JButton("Load Ultimaker Defaults");

		
		ActionListener loadDefaults = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent evt) {
				
				// Set all the values based on the selected default
				// Keep this up to date! if the set of defaults changes, so does this set of calls!
				setValue("infillPercent", "0.2");
				setValue("desiredLayerHeight", ".1");
				setValue("numberOfShells", "1");
				setValue("desiredFeedrate", "40");
				setValue("travelFeedrate", "55");
				setValue("filamentDiameter", "1.82");
//				setValue("packingDensity", "85");
				setValue("desiredPathWidth", ".4");
					
				// Refresh the other three tabs
				printOMatic5D.removeAll();
				makeTabs();
			}
		};
		def.addActionListener(loadDefaults);
		
		defaultsPanel.add(def, "growx, wrap");
		
		return defaultsPanel;
	}

	// Handles the creation of the various tabs and adds them to printOMatic
	private void makeTabs()
	{
		printOMatic5D.addTab("Settings", printPanel());
		printOMatic5D.addTab("Plastic", materialPanel());
		printOMatic5D.addTab("Extruder", machinePanel());
		printOMatic5D.addTab("Defaults", defaultsPanel());
	}
	
	public PrintOMatic5D() {
		component = new JPanel(new MigLayout("ins 0, fillx, hidemode 1"));
		
		baseName = "replicatorg.Slic3r.printOMatic5D.";

		// Add a checkbox to switch print-o-matic on and off
		final String enabledName = baseName + "enabled";
		enabled = new JCheckBox("Use Print-O-Matic (stepper extruders only)", Base.preferences.getBoolean(enabledName,true));
		enabled.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (enabledName != null) {
					Base.preferences.putBoolean(enabledName,enabled.isSelected());
					
					printOMatic5D.setVisible(enabled.isSelected());
					printOMatic5D.invalidate();
					Window w = SwingUtilities.getWindowAncestor(printOMatic5D);
					w.pack();
				
				}
			}
		});
		
		component.add(enabled, "wrap, spanx");
		
		// Make a tabbed pane to sort basic and advanced components 
		printOMatic5D = new JTabbedPane();

		// Handles the creation of the various tabs and adds them to printOMatic
		makeTabs();
		
		component.add(printOMatic5D, "spanx");
		printOMatic5D.setVisible(enabled.isSelected());
	}

	public JComponent getUI() { return component; }
	
	// Check the options to determine if they are in an acceptable range. Return null if
	// everything is ok, or a string describing the error if they are not ok.
	public String valueSanityCheck() {
		
		if (enabled.isSelected()) {
			// Check that width/thickness is ok
/*
                        double perimeterWidthOverThickness = getValue("desiredPathWidth")/getValue("desiredLayerHeight");
                        if (perimeterWidthOverThickness > 1.8) {
                                return "Layer height is smaller than recommended for the specified nozzle. Try increasing the layer height, or changing to a smaller nozzle.";
                        }
                        if (perimeterWidthOverThickness < 1.2) {
                                return "Layer height is larger than recommended for the specified nozzle. Try decreasing the layer height, or changing to a larger nozzle.";
                        }
*/
			
		}
		
		return null;
	}
	
	public List<Slic3rOption> getOptions() {
		
		List<Slic3rOption> options = new LinkedList<Slic3rOption>();

		if (enabled.isSelected()) {
		
			double  infillRatio                        = getValue("infillPercent")/100;
			double  filamentDiameter                   = getValue("filamentDiameter");
//			double  packingDensity                     = getValue("packingDensity")/100;
			//double  perimeterWidthOverThickness        = getValue("desiredPathWidth")/getValue("desiredLayerHeight");
			//double  infillWidthOverThickness           = perimeterWidthOverThickness;
			double  feedRate                           = getValue("desiredFeedrate");
			double  travelFeedRate                           = getValue("travelFeedrate");
			double  layerHeight                        = getValue("desiredLayerHeight");
			//double  extraShellsOnAlternatingSolidLayer = getValue("numberOfShells");
			double  extraShellsOnBase                  = getValue("numberOfShells");
			//double  extraShellsOnSparseLayer           = getValue("numberOfShells");

			Base.logger.fine("Print-O-Matic settings:"
					+ "\n infillRatio=" + infillRatio
					+ "\n filamentDiameter=" + filamentDiameter
//					+ "\n packingDensity=" + packingDensity
					//+ "\n perimeterWidthOverThickness=" + perimeterWidthOverThickness
					//+ "\n infillWidthOverThickness=" + infillWidthOverThickness
					+ "\n feedRate=" + feedRate
					+ "\n layerHeight=" + layerHeight
					//+ "\n extraShellsOnAlternatingSolidLayer=" + extraShellsOnAlternatingSolidLayer
					+ "\n extraShellsOnBase=" + extraShellsOnBase
					//+ "\n extraShellsOnSparseLayer=" + extraShellsOnSparseLayer
					);
			
			options.add(new Slic3rOption("--fill-density",Double.toString(infillRatio)));
			options.add(new Slic3rOption("--infill-speed", Double.toString(feedRate)));
			options.add(new Slic3rOption("--travel-speed", Double.toString(travelFeedRate)));
			options.add(new Slic3rOption("--perimeter-speed", Double.toString(feedRate)));
			options.add(new Slic3rOption("--filament-diameter", Double.toString(filamentDiameter)));
//			options.add(new Slic3rOption("dimension.csv", "Filament Packing Density (ratio):", Double.toString(packingDensity)));
			//options.add(new Slic3rOption("carve.csv", "Perimeter Width over Thickness (ratio):", Double.toString(perimeterWidthOverThickness)));
			//options.add(new Slic3rOption("fill.csv", "Infill Width over Thickness (ratio):", Double.toString(infillWidthOverThickness)));
			options.add(new Slic3rOption("--layer-height", Double.toString(layerHeight)));
			//options.add(new Slic3rOption("fill.csv", "Extra Shells on Alternating Solid Layer (layers):", Double.toString(extraShellsOnAlternatingSolidLayer)));
			options.add(new Slic3rOption("--perimeters", Double.toString(extraShellsOnBase)));
			//options.add(new Slic3rOption("fill.csv", "Extra Shells on Sparse Layer (layers):", Double.toString(extraShellsOnSparseLayer)));
			
		}
		
		return options;
	}
	
	@Override
	public String getName() {
		return "Print-O-Matic (5D)";
	}
}
