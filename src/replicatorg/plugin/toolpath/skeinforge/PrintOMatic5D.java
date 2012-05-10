package replicatorg.plugin.toolpath.skeinforge;

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
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgeOption;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator.SkeinforgePreference;

public class PrintOMatic5D implements SkeinforgePreference {
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
	
	public PrintOMatic5D() {
		component = new JPanel(new MigLayout("ins 0, fillx, hidemode 1"));
		
		baseName = "replicatorg.skeinforge.printOMatic5D.";

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

	
	/** 
	// add the specified Text Parameter to this display window
	// Note: This could be better represented as a separate class, 
	// however we want to be able to line up the text boxes and 
	// input fields in the main print-o-matic dialog. So they stay here!
	**/
	private void addTextParameter(JComponent target, String name, 
			String description,	String defaultValue, String toolTip) {
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
		
		// TODO: record the default values for optionName somewhere, 
		// so that we can retrieve them here!
		
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
	
	
	private JComponent generatePrintPanel() {
		
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

		String desiredFeedrate = situationBestFit("desiredFeedrate", "40");
		addTextParameter(printPanel, "desiredFeedrate",
				"Feedrate (mm/s)", desiredFeedrate, 
				"slow: 0-20, default: 30, Fast: 40+, Acclerated-fast:100");
		
		String travelFeedrate = situationBestFit("travelFeedrate","55");
		addTextParameter(printPanel, "travelFeedrate",
				"Travel Feedrate", travelFeedrate,
				"slow: 0-20, default: 30, Fast: 50+, Acclerated-fast:150");
		
		return printPanel;
	}
	
	/// use some install/situation related tools to decide on a best value
	/// for a specific setting
	private String situationBestFit(String valueKey,String baseline)
	{
		//Base.logger.severe("Print-o-Matic fetching from base");
		return Base.situationBestFit(valueKey, baseline);
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
				"Filament Diameter (mm)", "1.82",
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

		final JButton def = new JButton("Load Replicator Defaults");

		
		ActionListener loadDefaults = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent evt) {
				
				// Set all the values based on the selected default
				// Keep this up to date! if the set of defaults changes, so does this set of calls!
				setValue("infillPercent", "10");
				setValue("desiredLayerHeight", ".27");
				setValue("numberOfShells", "1");
				String desiredFeedrate = situationBestFit("desiredFeedrate", "40");
				String travelFeedrate = situationBestFit("travelFeedrate","55");
				setValue("desiredFeedrate", desiredFeedrate);
				setValue("travelFeedrate", travelFeedrate );
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
		printOMatic5D.addTab("Settings", generatePrintPanel());
		printOMatic5D.addTab("Plastic", materialPanel());
		printOMatic5D.addTab("Extruder", machinePanel());
		printOMatic5D.addTab("Defaults", defaultsPanel());
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
	
	public List<SkeinforgeOption> getOptions() {
		
		List<SkeinforgeOption> options = new LinkedList<SkeinforgeOption>();

		if (enabled.isSelected()) {
		
			double  infillRatio                        = getValue("infillPercent")/100;
			double  filamentDiameter                   = getValue("filamentDiameter");
//			double  packingDensity                     = getValue("packingDensity")/100;
			double  perimeterWidthOverThickness        = getValue("desiredPathWidth")/getValue("desiredLayerHeight");
			double  infillWidthOverThickness           = perimeterWidthOverThickness;
			double  feedRate                           = getValue("desiredFeedrate");
			double  travelFeedRate                     = getValue("travelFeedrate");
			double  layerHeight                        = getValue("desiredLayerHeight");
			double  extraShellsOnAlternatingSolidLayer = getValue("numberOfShells");
			double  extraShellsOnBase                  = getValue("numberOfShells");
			double  extraShellsOnSparseLayer           = getValue("numberOfShells");

			Base.logger.fine("Print-O-Matic settings:"
					+ "\n infillRatio=" + infillRatio
					+ "\n filamentDiameter=" + filamentDiameter
//					+ "\n packingDensity=" + packingDensity
					+ "\n perimeterWidthOverThickness=" + perimeterWidthOverThickness
					+ "\n infillWidthOverThickness=" + infillWidthOverThickness
					+ "\n feedRate=" + feedRate
					+ "\n layerHeight=" + layerHeight
					+ "\n extraShellsOnAlternatingSolidLayer=" + extraShellsOnAlternatingSolidLayer
					+ "\n extraShellsOnBase=" + extraShellsOnBase
					+ "\n extraShellsOnSparseLayer=" + extraShellsOnSparseLayer
					);
			
			options.add(new SkeinforgeOption("fill.csv", "Infill Solidity (ratio):", Double.toString(infillRatio)));
			options.add(new SkeinforgeOption("speed.csv", "Feed Rate (mm/s):", Double.toString(feedRate)));
			options.add(new SkeinforgeOption("speed.csv", "Travel Feed Rate (mm/s):", Double.toString(travelFeedRate)));
			options.add(new SkeinforgeOption("speed.csv", "Flow Rate Setting (float):", Double.toString(feedRate)));
			options.add(new SkeinforgeOption("dimension.csv", "Filament Diameter (mm):", Double.toString(filamentDiameter)));
//			options.add(new SkeinforgeOption("dimension.csv", "Filament Packing Density (ratio):", Double.toString(packingDensity)));
			options.add(new SkeinforgeOption("carve.csv", "Perimeter Width over Thickness (ratio):", Double.toString(perimeterWidthOverThickness)));
			options.add(new SkeinforgeOption("fill.csv", "Infill Width over Thickness (ratio):", Double.toString(infillWidthOverThickness)));
			options.add(new SkeinforgeOption("carve.csv", "Layer Thickness (mm):", Double.toString(layerHeight)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Alternating Solid Layer (layers):", Double.toString(extraShellsOnAlternatingSolidLayer)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Base (layers):", Double.toString(extraShellsOnBase)));
			options.add(new SkeinforgeOption("fill.csv", "Extra Shells on Sparse Layer (layers):", Double.toString(extraShellsOnSparseLayer)));
			
		}
		
		return options;
	}
	
	@Override
	public String getName() {
		return "Print-O-Matic (5D)";
	}
}
