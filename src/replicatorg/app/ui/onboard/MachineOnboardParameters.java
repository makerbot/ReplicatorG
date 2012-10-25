/**
  * 
  */
 package replicatorg.app.ui.onboard;

 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.Color;
 import java.text.NumberFormat;
 import java.util.EnumMap;
 import java.util.EnumSet;
 import java.util.Arrays;

 import java.util.prefs.BackingStoreException;
 import javax.swing.*;
 import javax.swing.text.InternationalFormatter;

 import net.miginfocom.swing.MigLayout;
 import replicatorg.app.Base;
 import replicatorg.drivers.Driver;
 import replicatorg.drivers.OnboardParameters;
 import replicatorg.machine.model.AxisId;

 /**
  * A panel for editing the options stored onboard a machine.
  * @author phooky
  *
  */
 public class MachineOnboardParameters extends JPanel {
	 private static final long serialVersionUID = 7876192459063774731L;
	 private final OnboardParameters target;
	 private final Driver driver;
	 private final JFrame parent;
	 private final JTabbedPane subTabs;
	 private final MachineOnboardAccelerationParameters accelUI;

	 private boolean isMightyBoard = false;
	 private boolean isSailfish = false;
	 private JTextField machineNameField = new JTextField();
	 private static final String[] toolCountChoices = {"unavailable","1", "2"};
	 private JComboBox toolCountField = new JComboBox(toolCountChoices);
	 private JCheckBox hbpToggleBox = new JCheckBox();
	 private JCheckBox xAxisInvertBox = new JCheckBox();
	 private JCheckBox yAxisInvertBox = new JCheckBox();
	 private JCheckBox zAxisInvertBox = new JCheckBox();
	 private JCheckBox aAxisInvertBox = new JCheckBox();
	 private JCheckBox bAxisInvertBox = new JCheckBox();
	 private JCheckBox zHoldBox = new JCheckBox();
	 private JButton resetToFactoryButton = new JButton("Reset motherboard to factory settings");
	 private JButton resetToBlankButton = new JButton("Reset motherboard completely");
	 private JButton commitButton = new JButton("Commit Changes");
	 private static final String[]  endstopInversionChoices = {
		 "No endstops installed",
		 "Inverted (Default; Mechanical switch or H21LOB-based enstops)",
		 "Non-inverted (H21LOI-based endstops)"
	 };
	 private JComboBox endstopInversionSelection = new JComboBox(endstopInversionChoices);

	 private static final String[]  estopChoices = {
		 "No emergency stop installed",
		 "Active high emergency stop (safety cutoff kit)",
		 "Active low emergency stop (custom solution)"
	 };
	 private JComboBox estopSelection = new JComboBox(estopChoices);
	 private static final int MAX_NAME_LENGTH = 16;

	 private boolean disconnectNeededOnExit = false; ///

	 // Default column width for wrapping tool tip text
	 private static final int defaultToolTipWidth = 60;

	 // WARNING: the following code is poking the settings for the global localNF NumberFormat!
	 //          threePlaces is merely a reference to Base.localNF.  A copy of it which can be
	 //          manipulated without impacting Base.localNF itself can be had by doing
	 //          (NumberFormat)(Base.getLocalFormat().clone());

    private NumberFormat threePlaces = Base.getLocalFormat();
    {
        threePlaces.setMaximumFractionDigits(3);
    }

	 private JFormattedTextField xAxisHomeOffsetField = new JFormattedTextField(threePlaces);
	 private JFormattedTextField yAxisHomeOffsetField = new JFormattedTextField(threePlaces);
	 private JFormattedTextField zAxisHomeOffsetField = new JFormattedTextField(threePlaces);
	 private JFormattedTextField aAxisHomeOffsetField = new JFormattedTextField(threePlaces);
	 private JFormattedTextField bAxisHomeOffsetField = new JFormattedTextField(threePlaces);

	 private JFormattedTextField vref0 = new JFormattedTextField(threePlaces);
	 private JFormattedTextField vref1 = new JFormattedTextField(threePlaces);
	 private JFormattedTextField vref2 = new JFormattedTextField(threePlaces);
	 private JFormattedTextField vref3 = new JFormattedTextField(threePlaces);
	 private JFormattedTextField vref4 = new JFormattedTextField(threePlaces);

	 private JFormattedTextField xToolheadOffsetField = new JFormattedTextField(threePlaces);
	 private JFormattedTextField yToolheadOffsetField = new JFormattedTextField(threePlaces);
	 private JFormattedTextField zToolheadOffsetField = new JFormattedTextField(threePlaces);


	 /** Prompts the user to fire a bot  reset after the changes have been sent to the board.
	  */
	 private void requestResetFromUser(String extendedMessage) {

		 String message = "For these changes to take effect your motherboard needs to reset. <br/>"+
				 "This may take up to <b>10 seconds</b>.";
		 if(extendedMessage != null)
			 message = message + extendedMessage;

		 int confirm = JOptionPane.showConfirmDialog(this, 
				 "<html>" + message + "</html>",
				 "Reset board.", 
				 JOptionPane.DEFAULT_OPTION,
				 JOptionPane.INFORMATION_MESSAGE);
		 if (confirm == JOptionPane.OK_OPTION) {
			 this.disconnectNeededOnExit = true;
			 driver.reset();
		 }
		 else
			 this.disconnectNeededOnExit = false;

	 }

	 /** 
	  * commit machine onboard parameters 
	  **/
	 private void commit() {
		 String newName = machineNameField.getText();
		 if(newName.length() > MAX_NAME_LENGTH)
			 machineNameField.setText(newName.substring(0, MAX_NAME_LENGTH ) );
		 target.setMachineName(machineNameField.getText());

		 if( target.hasToolCountOnboard() ) {
			 if (toolCountField.getSelectedIndex() > 0) 
				 target.setToolCountOnboard( toolCountField.getSelectedIndex() );
			 else 
				 target.setToolCountOnboard( -1 );
		 }

      if(target.hasHbp())
      {
        if((target.currentHbpSetting() == 0) && hbpToggleBox.isSelected())
        {
          target.setHbpSetting(true);
        }
          
        else if(((target.currentHbpSetting() > 0) && !hbpToggleBox.isSelected()))
        {
          target.setHbpSetting(false);
        }
      }
	
		 EnumSet<AxisId> axesInverted = EnumSet.noneOf(AxisId.class);
		 if (xAxisInvertBox.isSelected()) axesInverted.add(AxisId.X);
		 if (yAxisInvertBox.isSelected()) axesInverted.add(AxisId.Y);
		 if (zAxisInvertBox.isSelected()) axesInverted.add(AxisId.Z);
		 if (aAxisInvertBox.isSelected()) axesInverted.add(AxisId.A);
		 if (bAxisInvertBox.isSelected()) axesInverted.add(AxisId.B);

		 // V is in the 7th bit position, and it's set to NOT hold Z
		 // From the firmware: "Bit 7 is used for HoldZ OFF: 1 = off, 0 = on"
		 if ( !zHoldBox.isSelected() )	axesInverted.add(AxisId.V);


		 target.setInvertedAxes(axesInverted);
		 {
			 int idx = endstopInversionSelection.getSelectedIndex();
			 OnboardParameters.EndstopType endstops = 
				 OnboardParameters.EndstopType.values()[idx]; 
			 target.setInvertedEndstops(endstops);
		 }
		 {
			 int idx = estopSelection.getSelectedIndex();
			 OnboardParameters.EstopType estop = 
				 OnboardParameters.EstopType.estopTypeForValue((byte)idx); 
			 target.setEstopConfig(estop);
		 }

		 target.setAxisHomeOffset(0, ((Number)xAxisHomeOffsetField.getValue()).doubleValue());
		 target.setAxisHomeOffset(1, ((Number)yAxisHomeOffsetField.getValue()).doubleValue());
		 target.setAxisHomeOffset(2, ((Number)zAxisHomeOffsetField.getValue()).doubleValue());
		 target.setAxisHomeOffset(3, ((Number)aAxisHomeOffsetField.getValue()).doubleValue());
		 target.setAxisHomeOffset(4, ((Number)bAxisHomeOffsetField.getValue()).doubleValue());

		 if(target.hasVrefSupport())
		 {
			 target.setStoredStepperVoltage(0, ((Number)vref0.getValue()).intValue());
			 target.setStoredStepperVoltage(1, ((Number)vref1.getValue()).intValue());
			 target.setStoredStepperVoltage(2, ((Number)vref2.getValue()).intValue());
			 target.setStoredStepperVoltage(3, ((Number)vref3.getValue()).intValue());
			 target.setStoredStepperVoltage(4, ((Number)vref4.getValue()).intValue());
		 }

		 if (target.hasToolheadsOffset()) {
			 target.eepromStoreToolDelta(0, ((Number)xToolheadOffsetField.getValue()).doubleValue());
			 target.eepromStoreToolDelta(1, ((Number)yToolheadOffsetField.getValue()).doubleValue());
			 target.eepromStoreToolDelta(2, ((Number)zToolheadOffsetField.getValue()).doubleValue());
		 }
    
		 // Set acceleration related parameters
		 accelUI.setEEPROMFromUI();

		 String extendedMessage = null;

		 if (target.hasAcceleration() && isMightyBoard) {
			 int feedrate = Base.preferences.getInt("replicatorg.skeinforge.printOMatic5D.desiredFeedrate", 40);
			 int travelRate = Base.preferences.getInt("replicatorg.skeinforge.printOMatic5D.travelFeedrate", 55);

			 if (accelUI.isAccelerationEnabled()) {
				 ///TRCIKY: hack, if enabling acceleration AND print-o-matic old feedrates are slow,
				 // for speed them up.         	
				 Base.logger.finest("forced skeinforge speedup");
				 if(feedrate <= 40 ) 
					 Base.preferences.put("replicatorg.skeinforge.printOMatic5D.desiredFeedrate", "100");
				 if( travelRate <= 55)
					 Base.preferences.put("replicatorg.skeinforge.printOMatic5D.travelFeedrate", "150");          

				 extendedMessage = "  <br/><b>Also updating Print-O-Matic speed settings!</b>";
			 }
			 else {
				 ///TRCIKY: hack, if enabling acceleration AND print-o-matic old feedrates are fast,
				 // for slow them down. 
				 Base.logger.finest("forced skeinforge slowdown");
				 if(feedrate > 40 )
					 Base.preferences.put("replicatorg.skeinforge.printOMatic5D.desiredFeedrate", "40");
				 if( travelRate > 55)
					 Base.preferences.put("replicatorg.skeinforge.printOMatic5D.travelFeedrate", "55");

				 int xJog = 0; 
				 int zJog = 0; 
				 try {  
					 if( Base.preferences.nodeExists("controlpanel.feedrate.z") )
						 zJog = Base.preferences.getInt("controlpanel.feedrate.z", 480);
					 if(Base.preferences.nodeExists("controlpanel.feedrate.y") )
						 xJog = Base.preferences.getInt("controlpanel.feedrate.x", 480);
					 if(zJog < 480)
						 Base.preferences.put("controlpanel.feedrate.z", "480");
					 if(xJog < 480)
						 Base.preferences.put("controlpanel.feedrate.x", "480");
				 }
				 catch (BackingStoreException e) {
					 Base.logger.severe(e.toString());
				 }

				 extendedMessage = "  <br/><b>Also updating Print-O-Matic speed settings!</b>";
			 }
		 }
		 requestResetFromUser(extendedMessage);
	 }

	
	/// Causes the EEPROM to be reset to a totally blank state, and during dispose
	/// tells caller to reset/reconnect the eeprom.
	private void resetToBlank()
	{
		try { 
			target.resetSettingsToBlank();
			requestResetFromUser("<b>Resetting EEPROM to completely blank</b>");
			MachineOnboardParameters.this.dispose();
		}
		catch (replicatorg.drivers.RetryException e){
			Base.logger.severe("reset to blank failed due to error" + e.toString());
			Base.logger.severe("Please restart your machine for safety");
		}		
	}
	
	/// Causes the EEPROM to be reset to a 'from the factory' state, and during dispose
	/// tells caller to reset/reconnect the eeprom.
	private void resetToFactory() {
		try { 
			target.resetSettingsToFactory();
			requestResetFromUser("<b>Resetting EEPROM to Factory Default.</b>");
			MachineOnboardParameters.this.dispose();
		}
		catch (replicatorg.drivers.RetryException e){
			Base.logger.severe("reset to blank failed due to error" + e.toString());
			Base.logger.severe("Please restart your machine for safety");
		}
	}
	

	private void loadParameters() {
		machineNameField.setText( this.target.getMachineName() );

		if(target.hasToolCountOnboard()){
			int toolCount = target.toolCountOnboard();
			if (toolCount == 1 || toolCount == 2) 
				toolCountField.setSelectedIndex(toolCount); //'1' or '2'
			else
				toolCountField.setSelectedIndex(0);//'unknown'
		}
		
		EnumSet<AxisId> invertedAxes = this.target.getInvertedAxes();
		
		xAxisInvertBox.setSelected(invertedAxes.contains(AxisId.X));
		yAxisInvertBox.setSelected(invertedAxes.contains(AxisId.Y));
		zAxisInvertBox.setSelected(invertedAxes.contains(AxisId.Z));
		aAxisInvertBox.setSelected(invertedAxes.contains(AxisId.A));
		bAxisInvertBox.setSelected(invertedAxes.contains(AxisId.B));
		zHoldBox.setSelected(     !invertedAxes.contains(AxisId.V));
		
		if(target.hasHbp()){
			byte hbp_setting = target.currentHbpSetting();
			if(hbp_setting > 0)
				hbpToggleBox.setSelected(true);
			else
				hbpToggleBox.setSelected(false);
		}

		// 0 == inverted, 1 == not inverted
		OnboardParameters.EndstopType endstops = this.target.getInvertedEndstops();
		endstopInversionSelection.setSelectedIndex(endstops.ordinal());

		OnboardParameters.EstopType estop = this.target.getEstopConfig();
		estopSelection.setSelectedIndex(estop.ordinal());
	   
		xAxisHomeOffsetField.setValue(this.target.getAxisHomeOffset(0));
		yAxisHomeOffsetField.setValue(this.target.getAxisHomeOffset(1));
		zAxisHomeOffsetField.setValue(this.target.getAxisHomeOffset(2));
		aAxisHomeOffsetField.setValue(this.target.getAxisHomeOffset(3));
		bAxisHomeOffsetField.setValue(this.target.getAxisHomeOffset(4));
		
		if(target.hasVrefSupport())
		{
			vref0.setValue(this.target.getStoredStepperVoltage(0));
			vref1.setValue(this.target.getStoredStepperVoltage(1));
			vref2.setValue(this.target.getStoredStepperVoltage(2));
			vref3.setValue(this.target.getStoredStepperVoltage(3));
			vref4.setValue(this.target.getStoredStepperVoltage(4));
		}

		if(target.hasToolheadsOffset()) {
			xToolheadOffsetField.setValue(this.target.getToolheadsOffset(0));
			yToolheadOffsetField.setValue(this.target.getToolheadsOffset(1));
			zToolheadOffsetField.setValue(this.target.getToolheadsOffset(2));
		}   
                
		accelUI.setUIFromEEPROM();
	}

	protected void dispose() {
		parent.dispose();
	}

	public MachineOnboardParameters(OnboardParameters target, Driver driver, JFrame parent) {
		this.target = target;
		this.driver = driver;
		this.parent = parent;

		String driverName = target.getDriverName();  // can return null
		isMightyBoard = (driverName != null) && driverName.equalsIgnoreCase("mightyboard");
		if (!isMightyBoard)
			isSailfish = (driverName != null) && driverName.equalsIgnoreCase("makerbot4gsailfish");
		else
			isSailfish = false;

    setLayout(new MigLayout("fill", "[r][l][r]"));

		add(new JLabel("Machine Name (max. "+Integer.toString(MAX_NAME_LENGTH)+" chars)"));
		machineNameField.setColumns(MAX_NAME_LENGTH);
		add(machineNameField,"span 2, wrap");

		subTabs = new JTabbedPane();
		add(subTabs, "span 3, wrap");

		JPanel endstopsTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
		subTabs.addTab("Endstops/Axis Inversion", endstopsTab);

		JPanel homeVrefsTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
                
                if(target.hasVrefSupport())
		{
			subTabs.addTab("Homing/VREFs", homeVrefsTab);
		} else {
			subTabs.addTab("Homing", homeVrefsTab);
		}
		
		EnumMap<AxisId, String> axesAltNamesMap = target.getAxisAlises();

  		if( target.hasToolCountOnboard() ) {
  			endstopsTab.add(new JLabel("Reported Tool Count:"));
  			endstopsTab.add(toolCountField, "span 2, wrap");
  		}
		if(target.hasHbp()){
			endstopsTab.add(new JLabel("HBP present"));
			endstopsTab.add(hbpToggleBox,"span 2, wrap");
		}

		endstopsTab.add(new JLabel("Invert X axis"));		
		endstopsTab.add(xAxisInvertBox,"span 2, wrap");
		
		endstopsTab.add(new JLabel("Invert Y axis"));
		endstopsTab.add(yAxisInvertBox,"span 2, wrap");
		
		endstopsTab.add(new JLabel("Invert Z axis"));
		endstopsTab.add(zAxisInvertBox,"span 2, wrap");

		String aName = "Invert A axis";
		if( axesAltNamesMap.containsKey(AxisId.A) )
			aName = aName + " (" + axesAltNamesMap.get(AxisId.A) + ") ";
		endstopsTab.add(new JLabel(aName));
		endstopsTab.add(aAxisInvertBox,"span 2, wrap");
		
		String bName = "Invert B axis";
		if( axesAltNamesMap.containsKey(AxisId.B) )
			bName = bName + " (" + axesAltNamesMap.get(AxisId.B) + ") ";
		endstopsTab.add(new JLabel(bName));
		
		endstopsTab.add(bAxisInvertBox,"span 2, wrap");
		endstopsTab.add(new JLabel("Hold Z axis"));
		
		endstopsTab.add(zHoldBox,"span 2, wrap");
		endstopsTab.add(new JLabel("Invert endstops"));
		
		endstopsTab.add(endstopInversionSelection,"span 2, wrap");
		endstopsTab.add(new JLabel("Emergency stop"));
		endstopsTab.add(estopSelection,"spanx, wrap");
		
		
		xAxisHomeOffsetField.setColumns(10);
		yAxisHomeOffsetField.setColumns(10);
		zAxisHomeOffsetField.setColumns(10);
		aAxisHomeOffsetField.setColumns(10);
		bAxisHomeOffsetField.setColumns(10);
		
		if(target.hasVrefSupport())
		{
			vref0.setColumns(4);
			vref1.setColumns(4);
			vref2.setColumns(4);
			vref3.setColumns(4);
			vref4.setColumns(4);
			homeVrefsTab.add(new JLabel("X home offset (mm)"));
			homeVrefsTab.add(xAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 0"), "split");
			homeVrefsTab.add(vref0, "wrap");
			homeVrefsTab.add(new JLabel("Y home offset (mm)"));
			homeVrefsTab.add(yAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 1"), "split");
			homeVrefsTab.add(vref1, "wrap");
			homeVrefsTab.add(new JLabel("Z home offset (mm)"));
			homeVrefsTab.add(zAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 2"), "split");
			homeVrefsTab.add(vref2, "wrap");
			homeVrefsTab.add(new JLabel("A home offset (mm)"));
			homeVrefsTab.add(aAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 3"), "split");
			homeVrefsTab.add(vref3, "wrap");
			homeVrefsTab.add(new JLabel("B home offset (mm)"));
			homeVrefsTab.add(bAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 4"), "split");
			homeVrefsTab.add(vref4, "wrap");
		}
		else
		{
			homeVrefsTab.add(new JLabel("X home offset (mm)"));
			homeVrefsTab.add(xAxisHomeOffsetField,"wrap");
			homeVrefsTab.add(new JLabel("Y home offset (mm)"));
			homeVrefsTab.add(yAxisHomeOffsetField,"wrap");
			homeVrefsTab.add(new JLabel("Z home offset (mm)"));
			homeVrefsTab.add(zAxisHomeOffsetField,"wrap");
			homeVrefsTab.add(new JLabel("A home offset (mm)"));
			homeVrefsTab.add(aAxisHomeOffsetField,"wrap");
			homeVrefsTab.add(new JLabel("B home offset (mm)"));
			homeVrefsTab.add(bAxisHomeOffsetField,"wrap");
                        
		}

		if(target.hasToolheadsOffset()) {
		    xToolheadOffsetField.setColumns(10);
		    yToolheadOffsetField.setColumns(10);
		    zToolheadOffsetField.setColumns(10);
		    
		    homeVrefsTab.add(new JLabel("X toolhead offset (mm)"));
		    homeVrefsTab.add(xToolheadOffsetField, "wrap");
		    
		    homeVrefsTab.add(new JLabel("Y toolhead offset (mm)"));
		    homeVrefsTab.add(yToolheadOffsetField, "wrap");
		    
		    homeVrefsTab.add(new JLabel("Z toolhead offset (mm)"));
		    homeVrefsTab.add(zToolheadOffsetField, "wrap");
                   
		}

		if (target.hasAcceleration()) {
			if (isMightyBoard) {
				if (target.hasJettyAcceleration())
					accelUI = new JettyMightyBoardMachineOnboardAccelerationParameters(target, driver, subTabs);
				else
					accelUI = new MightyBoardMachineOnboardAccelerationParameters(target, driver, subTabs);
			}
			else if (isSailfish)
				accelUI = new SailfishG3MachineOnboardAccelerationParameters(target, driver, subTabs);
			else
				accelUI = new G3FirmwareMachineOnboardAccelerationParameters(target, driver, subTabs);
		}
		else
			accelUI = new MachineOnboardAccelerationParameters(target, driver, subTabs);

		accelUI.buildUI();

		resetToFactoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MachineOnboardParameters.this.resetToFactory();
				// This gets called in resetToFactory()
//				loadParameters();
			}
		});
		resetToFactoryButton.setToolTipText("Reset the onboard settings to the factory defaults");
		add(resetToFactoryButton, "split 1");

		
		resetToBlankButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MachineOnboardParameters.this.resetToBlank();
				// This gets called in resetToFactory()
//				loadParameters();
			}
		});
		resetToBlankButton.setToolTipText("Reset the onboard settings to *completely blank*");
		add(resetToBlankButton);

		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MachineOnboardParameters.this.commit();
				disconnectNeededOnExit = true;
				MachineOnboardParameters.this.dispose();
			}
		});
		add(commitButton, "al right");
		loadParameters();
	}

	public boolean disconnectOnExit() {
		return disconnectNeededOnExit;
	}

	public boolean leavePreheatRunning() {
		return isMightyBoard;
	}

	 // If a string is longer than 'width' characters then convert it to minimal, headless,
	 // bodyless HTML with line breaks, <br>, placed such that no line is longer than 'width'
	 // characters if possible (and with line breaks at white space).

	 // This is useful for making tool tip text which doesn't run off the screen.

	 private static final String wrap2HTML(int width, String str) { return wrap2HTML(width, str, -1); }

	 private static final String wrap2HTML(int width, String str, int max) {

		 if (str.length() < width && max <= 0)
			 return str;

		 char[] s;
		 if (max <= 0)
			 s = str.toCharArray();
		 else {
			 str += "  This value must be non-negative and not exceed " + max + ".";
			 s = str.toCharArray();
		 }

		 StringBuilder html = new StringBuilder();
		 StringBuilder line = new StringBuilder();
		 StringBuilder word = new StringBuilder();

		 html.append("<HTML>");
		 for (int i = 0; i < s.length; i++) {
			 word.append(s[i]);
			 if (Character.isWhitespace(s[i])) {
				 if (line.length() == 0) {
					 // Just append word to line
					 line.append(word);
					 word.delete(0, word.length());
				 }
				 if ((line.length() + word.length()) > width) {
					 if (html.length() > 6)
						 html.append("<br>");
					 html.append(line);
					 line.delete(0, line.length());
				 }
				 if (word.length() > 0) {
					 line.append(word);
					 word.delete(0, word.length());
				 }
			 }
		 }

		 line.append(word);
		 if (html.length() > 6)
			 html.append("<br>");
		 html.append(line);
		 html.append("</HTML>");

		 return html.toString();
	 }

	 // JFormattedTextField which disallows negative values and no upper limit on positive values

	 // The range limits are enacted by going after the Formatter which gets associated with
	 // the text field.  Much easier than extending the NumberFormat and trying to filter the
	 // text associated with it or playing with the negation charactes.

	 private static final JFormattedTextField PositiveTextFieldDouble(NumberFormat nf, String tip)
	 {
		 JFormattedTextField textField = new JFormattedTextField(nf);
		 Object fmtr = textField.getFormatter();
		 if (fmtr instanceof InternationalFormatter) {
			 InternationalFormatter ifr = (InternationalFormatter)fmtr;
			 ifr.setMinimum(new Double(0d));
		 }
                 if (tip != null) textField.setToolTipText(wrap2HTML(defaultToolTipWidth, tip));
		 return textField;
	 }

	 // JFormattedTextField which disallows negative values and no upper limit on positive values

	 private static final JFormattedTextField PositiveTextFieldInt(NumberFormat nf, String tip)
	 {
		 JFormattedTextField textField = new JFormattedTextField(nf);
		 Object fmtr = textField.getFormatter();
		 if (fmtr instanceof InternationalFormatter) {
			 InternationalFormatter ifr = (InternationalFormatter)fmtr;
			 ifr.setMinimum(new Integer(0));
		 }
                 if (tip != null) textField.setToolTipText(wrap2HTML(defaultToolTipWidth, tip));
		 return textField;
	 }

	 // JFormattedTextField which disallows negative values "max" as an upper limit

	 private static final JFormattedTextField PositiveTextFieldInt(NumberFormat nf, int max, String tip)
	 {
		 JFormattedTextField textField = new JFormattedTextField(nf);
		 Object fmtr = textField.getFormatter();
		 if (fmtr instanceof InternationalFormatter) {
			 InternationalFormatter ifr = (InternationalFormatter)fmtr;
			 ifr.setMinimum(new Integer(0));
			 ifr.setMaximum(new Integer(max));
		 }
		 if (tip != null) textField.setToolTipText(wrap2HTML(defaultToolTipWidth, tip, max));
		 return textField;
	 }

	 // Give a label the same tool tip text as the field it is labelleling.  This is especially
	 // use for check boxes which seem to have a very tight hover-over area which triggers display
	 // of the tool tip.

	 private static final void addWithSharedToolTips(JPanel panel, String labelText, JComponent buddy)
	 {
		 JLabel label = new JLabel(labelText);
		 label.setToolTipText(buddy.getToolTipText());
		 panel.add(label);
		 panel.add(buddy);
	 }

	 private static final void addWithSharedToolTips(JPanel panel, String labelText, JComponent buddy,
					    String layoutGoodies2)
	 {
		 JLabel label = new JLabel(labelText);
		 label.setToolTipText(buddy.getToolTipText());
		 panel.add(label);
		 panel.add(buddy, layoutGoodies2);
	 }

	 private static final void addWithSharedToolTips(JPanel panel, String labelText, String layoutGoodies1,
					    JComponent buddy,
				  String layoutGoodies2)
	 {
		 JLabel label = new JLabel(labelText);
		 label.setToolTipText(buddy.getToolTipText());
		 panel.add(label, layoutGoodies1);
		 panel.add(buddy, layoutGoodies2);
	 }

	 private static final void addWithSharedToolTips(JPanel panel, String labelText, String layoutGoodies1,
					    JComponent buddy)
	 {
		 JLabel label = new JLabel(labelText);
		 label.setToolTipText(buddy.getToolTipText());
		 panel.add(label, layoutGoodies1);
		 panel.add(buddy);
	 }


	 // Class for containing additional subtabs of EEPROM acceleration parameters

	 // Use the base class for unaccelerated firmwares.  For accelerated firmwares,
	 // use the appropriate extension for the firmware.

	 private class MachineOnboardAccelerationParameters {

		 protected final OnboardParameters target;
		 protected final Driver driver;
		 protected final JTabbedPane subtabs;

		 public MachineOnboardAccelerationParameters(OnboardParameters target,
							     Driver driver,
							     JTabbedPane subtabs) {
			 this.target = target;
			 this.driver = driver;
			 this.subtabs = subtabs;
		 }

		 // See if the acceleration check box is checked
		 public boolean isAccelerationEnabled() { return false; }

		 // Build the UI (subtabs)
		 public void buildUI() { }

		 // Set the UI fields from the values stored in EEPROM
		 public void setUIFromEEPROM() { }

		 // Store the field values to the machine's onboard EEPROM space
		 public void setEEPROMFromUI() { }
	 }

	 // Replicator MightyBoard with pre-Jetty acceleration (v5.5 and earlier)

	 private class MightyBoardMachineOnboardAccelerationParameters extends MachineOnboardAccelerationParameters {

		 private JCheckBox accelerationBox = new JCheckBox();
		 private JFormattedTextField masterAcceleration = new JFormattedTextField(threePlaces);

		 private JFormattedTextField xAxisAcceleration = new JFormattedTextField(threePlaces);
		 private JFormattedTextField yAxisAcceleration = new JFormattedTextField(threePlaces);
		 private JFormattedTextField zAxisAcceleration = new JFormattedTextField(threePlaces);
		 private JFormattedTextField aAxisAcceleration = new JFormattedTextField(threePlaces);
		 private JFormattedTextField bAxisAcceleration = new JFormattedTextField(threePlaces);

		 private JFormattedTextField xyJunctionJerk = new JFormattedTextField(threePlaces);
		 private JFormattedTextField  zJunctionJerk = new JFormattedTextField(threePlaces);
		 private JFormattedTextField  aJunctionJerk = new JFormattedTextField(threePlaces);
		 private JFormattedTextField  bJunctionJerk = new JFormattedTextField(threePlaces);

		 private JFormattedTextField minimumSpeed = new JFormattedTextField(threePlaces);

		 public MightyBoardMachineOnboardAccelerationParameters(OnboardParameters target,
							     Driver driver,
							     JTabbedPane subtabs) {
			 super(target, driver, subtabs);
		 }
		 
		 @Override
		 public boolean isAccelerationEnabled() {
			 return accelerationBox.isSelected();
		 }

		 @Override
		 public void buildUI() {
			 JPanel accelerationTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			 subTabs.addTab("Acceleration", accelerationTab);

			 masterAcceleration.setColumns(4);

			 xAxisAcceleration.setColumns(8);
			 xyJunctionJerk.setColumns(4);

			 yAxisAcceleration.setColumns(8);

			 zAxisAcceleration.setColumns(8);
			 zJunctionJerk.setColumns(4);

			 aAxisAcceleration.setColumns(8);
			 aJunctionJerk.setColumns(4);

			 bAxisAcceleration.setColumns(8);
			 bJunctionJerk.setColumns(4);

			 accelerationTab.add(new JLabel("Acceleration On"));
			 accelerationTab.add(accelerationBox, "span 2, wrap");

			 accelerationTab.add(new JLabel("Master acceleration rate (mm/s/s)"));
			 accelerationTab.add(masterAcceleration, "span 2, wrap");

			 accelerationTab.add(new JLabel("X acceleration rate (mm/s/s)"));
			 accelerationTab.add(xAxisAcceleration);
			 accelerationTab.add(new JLabel("X/Y max junction jerk (mm/s)"));
			 accelerationTab.add(xyJunctionJerk, "wrap");

			 accelerationTab.add(new JLabel("Y acceleration rate (mm/s/s)"));
			 accelerationTab.add(yAxisAcceleration, "span 2, wrap");

			 accelerationTab.add(new JLabel("Z acceleration rate (mm/s/s)"));
			 accelerationTab.add(zAxisAcceleration);
			 accelerationTab.add(new JLabel("Z maximum junction jerk (mm/s)"));
			 accelerationTab.add(zJunctionJerk, "wrap");

			 accelerationTab.add(new JLabel("A acceleration rate (mm/s/s)"));
			 accelerationTab.add(aAxisAcceleration);
			 accelerationTab.add(new JLabel("A maximum junction jerk (mm/s)"));
			 accelerationTab.add(aJunctionJerk, "wrap");

			 accelerationTab.add(new JLabel("B acceleration rate (mm/s/s)"));
			 accelerationTab.add(bAxisAcceleration);
			 accelerationTab.add(new JLabel("B maximum junction jerk (mm/s)"));
			 accelerationTab.add(bJunctionJerk, "wrap");
                    
			 accelerationTab.add(new JLabel("Minimum Print Speed (mm/s)"));
			 accelerationTab.add(minimumSpeed, "wrap");
		 }

		 @Override
		 public void setEEPROMFromUI() {
			 target.setAccelerationStatus(accelerationBox.isSelected() ? (byte)1 : (byte)0);

			 target.setAccelerationRate(((Number)masterAcceleration.getValue()).intValue());

			 target.setAxisAccelerationRate(0, ((Number)xAxisAcceleration.getValue()).intValue());
			 target.setAxisAccelerationRate(1, ((Number)yAxisAcceleration.getValue()).intValue());
			 target.setAxisAccelerationRate(2, ((Number)zAxisAcceleration.getValue()).intValue());
			 target.setAxisAccelerationRate(3, ((Number)aAxisAcceleration.getValue()).intValue());
			 target.setAxisAccelerationRate(4, ((Number)bAxisAcceleration.getValue()).intValue());

			 target.setAxisJerk(0, ((Number)xyJunctionJerk.getValue()).doubleValue());
			 target.setAxisJerk(2, ((Number) zJunctionJerk.getValue()).doubleValue());
			 target.setAxisJerk(3, ((Number) aJunctionJerk.getValue()).doubleValue());
			 target.setAxisJerk(4, ((Number) bJunctionJerk.getValue()).doubleValue());

			 target.setAccelerationMinimumSpeed(((Number)minimumSpeed.getValue()).intValue());
		 }

		 @Override
		 public void setUIFromEEPROM() {
			 accelerationBox.setSelected(target.getAccelerationStatus() != 0);
			 masterAcceleration.setValue(target.getAccelerationRate());

			 xAxisAcceleration.setValue(target.getAxisAccelerationRate(0));
			 yAxisAcceleration.setValue(target.getAxisAccelerationRate(1));
			 zAxisAcceleration.setValue(target.getAxisAccelerationRate(2));
			 aAxisAcceleration.setValue(target.getAxisAccelerationRate(3));
			 bAxisAcceleration.setValue(target.getAxisAccelerationRate(4));

			 xyJunctionJerk.setValue(target.getAxisJerk(0));
			 zJunctionJerk.setValue(target.getAxisJerk(2));
			 aJunctionJerk.setValue(target.getAxisJerk(3));
			 bJunctionJerk.setValue(target.getAxisJerk(4));

			 minimumSpeed.setValue(target.getAccelerationMinimumSpeed());
		 }
	 }

	 // Replicator MightyBoard with Jetty acceleration (v5.6 and later)

	 private class JettyMightyBoardMachineOnboardAccelerationParameters extends MachineOnboardAccelerationParameters {

		 // Replicator Jetty Firmware specific acceleration parameters

		 // Bitmask bits indicating which tabs to set
		 // We worry about this aspect as part of supporting the "draft" and "quality" buttons
		 final int UI_TAB_1 = 0x01;
		 final int UI_TAB_2 = 0x02;

		 // Accel Parameters of Tab 1
		 class AccelParamsTab1 {
			 boolean accelerationEnabled;
			 int[] accelerations;
			 int[] maxAccelerations;
			 int[] maxSpeedChanges;

			 AccelParamsTab1(boolean accelerationEnabled,
				     int[] accelerations,
				     int[] maxAccelerations,
				     int[] maxSpeedChanges)
			 {
				 this.accelerationEnabled = accelerationEnabled;
				 this.accelerations       = accelerations;
				 this.maxAccelerations    = maxAccelerations;
				 this.maxSpeedChanges     = maxSpeedChanges;
			 }

			 boolean isEqual(AccelParamsTab1 params)
			 {
				 if (this == params)
					 return true;

				 return (accelerationEnabled == params.accelerationEnabled) &&
					 Arrays.equals(accelerations, params.accelerations) &&
					 Arrays.equals(maxAccelerations, params.maxAccelerations) &&
					 Arrays.equals(maxSpeedChanges, params.maxSpeedChanges);
			 }
		 }

		 // Accel Parameters of Tab 2
		 class AccelParamsTab2 {
			 boolean slowdownEnabled;
			 int[] deprime;
			 double[] JKNadvance;

			 AccelParamsTab2(boolean slowdownEnabled,
					 int[] deprime,
					 double[] JKNadvance)
			 {
				 this.slowdownEnabled           = slowdownEnabled;
				 this.deprime                   = deprime;
				 this.JKNadvance                = JKNadvance;
			 }
		 }

		 private class AccelParams {
			 public AccelParamsTab1 tab1;
			 public AccelParamsTab2 tab2;

			 AccelParams(AccelParamsTab1 params1, AccelParamsTab2 params2) {
				 this.tab1 = params1;
				 this.tab2 = params2;
			 }
		 }

		 AccelParamsTab1 draftParams = new AccelParamsTab1(true,                                   // acceleration enabled
								   new int[] {2000, 2000},                 // p_accel, p_retract_accel
								   new int[] {1000, 1000, 150, 2000, 2000}, // max accelerations x,y,z,a,b
								   new int[] {40, 40, 10, 40, 40});        // max speed changes x,y,z,a,b

		 AccelParamsTab1 qualityParams = new AccelParamsTab1(true,                                 // acceleration enabled
								     new int[] {2000, 2000},               // p_accel, p_retract_accel
								     new int[] {1000, 1000, 150, 2000, 2000}, // max accelerations x,y,z,a,b
								     new int[] {15, 15, 10, 20, 20});      // max speed changes x,y,z,a,b

		 // Column width for formatting tool tip text
		 final int width = defaultToolTipWidth;

		 // Many of the values stored in EEPROM for the replicator are uint16_t
		 //   So, we want 0 < val < 0xffff

		 private NumberFormat repNF = NumberFormat.getIntegerInstance();

		 private JCheckBox accelerationBox = new JCheckBox();
		 {
			 accelerationBox.setToolTipText(wrap2HTML(width, "Enable or disable printing with acceleration"));
		 }

		 private JButton draftButton = new JButton("Quick Draft");
		 {
			 draftButton.setToolTipText(wrap2HTML(width,
           "By clicking this button, the on-screen acceleration parameters will be changed to suggested values for " +
	   "rapid draft-quality builds.  The values will not be committed to your Replicator until you click the " +
	   "Commit button.  You may adjust the settings before committing them to your Replicator."));
		 }

		 private JButton qualityButton = new JButton("Fine Quality");
		 {
			 qualityButton.setToolTipText(wrap2HTML(width,
           "By clicking this button, the on-screen acceleration parameters will be changed to suggested values for " +
	   "fine-quality builds.  The values will not be committed to your Replicator until you click the " +
	   "Commit button.  You may adjust the settings before committing them to your Replicator."));
		 }

		 private JFormattedTextField xAxisMaxAcceleration = PositiveTextFieldInt(repNF, 10000,
	   "The maximum acceleration and deceleration along the X axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the X axis.");

		 private JFormattedTextField yAxisMaxAcceleration = PositiveTextFieldInt(repNF, 10000,
	   "The maximum acceleration and deceleration along the Y axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the Y axis.");

		 private JFormattedTextField zAxisMaxAcceleration = PositiveTextFieldInt(repNF, 2600,
	   "The maximum acceleration and deceleration along the Z axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the Z axis.");

		 private JFormattedTextField aAxisMaxAcceleration = PositiveTextFieldInt(repNF, 10000,
	   "The maximum acceleration and deceleration experienced by the right extruder in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the right extruder's filament axis.");

		 private JFormattedTextField bAxisMaxAcceleration = PositiveTextFieldInt(repNF, 10000,
	   "The maximum acceleration and deceleration experienced by the left extruder in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the left extruder's filament axis.");

		 private JFormattedTextField xAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the X axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the X axis.");

		 private JFormattedTextField yAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the Y axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the Y axis.");

		 private JFormattedTextField zAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the Z axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the Z axis.");

		 private JFormattedTextField aAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate for the right extruder when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change for the right extruder.");

		 private JFormattedTextField bAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate for the left extruder when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change for the left extruder.");

		 private JFormattedTextField normalMoveAcceleration = PositiveTextFieldInt(repNF, 10000,
           "The maximum rate of acceleration for normal printing moves in which filament is extruded and " +
           "there is motion along any or all of the X, Y, or Z axes.  I.e., the maximum magnitude of the " +
	   "acceleration vector in units of millimeters per second squared, mm/s\u00B2.");

		 private JFormattedTextField extruderMoveAcceleration = PositiveTextFieldInt(repNF, 10000,
            "The maximum acceleration or deceleration in mm/s\u00B2 to use in an extruder-only move.  An extruder-only " +
            "move is a move in which there is no motion along the X, Y, or Z axes: the only motion is the extruder " +
            "extruding or retracting filament.  Typically this value should be at least as large as the A and B axis max " +
	    "accelerations.");

		 // Advance K1 & K2: nn.nnnnn
		 private NumberFormat kNF = NumberFormat.getNumberInstance();
		 {
			 kNF.setMaximumFractionDigits(5);
			 kNF.setMaximumIntegerDigits(2);   // Even 1 may be excessive
		 }

		 private JFormattedTextField JKNAdvance1 = PositiveTextFieldDouble(kNF,
           "The value of the empirically fit Jetty-Kubicek-Newman Advance parameter K which helps control " +
           "the amount of additional plastic that should be extruded during the acceleration phase and not " +
           "extruded during the deceleration phase of each move.  It can be used to remove blobbing and " +
           "splaying on the corners of cubes or at the junctions between line segments.  Typical values " +
           "for this parameter range from around 0.0001 to 0.01.  Set to a value of 0 to disable use of " +
           "this compensation.");

		 private JFormattedTextField JKNAdvance2 = PositiveTextFieldDouble(kNF,
           "The value of the empirically fit Jetty-Kubicek-Newman Advance parameter K2, which helps during the " +
           "deceleration phase of moves to reduce built up pressure in the extruder nozzle.  Typical values for " +
           "this parameter range from around 0.001 to 0.1.  Set to a value of 0 to disable use of this " +
           "compensation.");

		 private JFormattedTextField extruderDeprimeA = PositiveTextFieldInt(repNF, 10000,
           "The number of steps to retract the right extruder's filament when the pipeline of buffered moves empties " +
           "or a travel-only move is encountered.  Set to a value of 0 to disable this feature for this extruder.  " +
           "Do not use with Skeinforge's Reversal plugin nor Skeinforge's Dimension plugin's \"Retraction Distance\".");

		 private JFormattedTextField extruderDeprimeB = PositiveTextFieldInt(repNF, 10000,
           "The number of steps to retract the left extruder's filament when the pipeline of buffered moves empties " +
           "or a travel-only move is encountered.  Set to a value of 0 to disable this feature for this extruder.  " +
           "Do not use with Skeinforge's Reversal plugin nor Skeinforge's Dimension plugin's \"Retraction Distance\".");

           
		 // Slowdown is a flag for the Replicator
		 private JCheckBox slowdownFlagBox = new JCheckBox();
		 {
			 slowdownFlagBox.setToolTipText(wrap2HTML(width,
            "If you are printing an object with fine details or at very fast speeds, it is possible " +
            "that the planner will be unable to keep up with printing.  This may be evidenced by frequent " +
            "pauses accompanied by unwanted plastic blobs or zits.  You may be able to mitigate this by " +
            "enabling \"slowdown\".  When slowdown is enabled and the planner is having difficulty keeping " +
            "up, the printing feed rate is reduced so as to cause each segment to take more time to print.  " +
	    "The reduction in printing speed then gives the planner a chance to catch up."));
		 }

		 private JettyMightyBoardMachineOnboardAccelerationParameters(OnboardParameters target,
									      Driver driver,
									      JTabbedPane subtabs) {
			 super(target, driver, subtabs);

			 int dismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
			 if (dismissDelay < 10*1000)
				 ToolTipManager.sharedInstance().setDismissDelay(10*1000);

			 draftButton.addActionListener(new ActionListener() {
					 public void actionPerformed(ActionEvent arg0) {
						 JettyMightyBoardMachineOnboardAccelerationParameters.this.setUIFields(draftParams);
					 }
			 });

			 qualityButton.addActionListener(new ActionListener() {
					 public void actionPerformed(ActionEvent arg0) {
						 JettyMightyBoardMachineOnboardAccelerationParameters.this.setUIFields(qualityParams);
					 }
			 });
		 }

		 AccelParams getAccelParamsFromUI() {
			 return new AccelParams(new AccelParamsTab1(accelerationBox.isSelected(),
								    new int[] {((Number)normalMoveAcceleration.getValue()).intValue(),
									       ((Number)extruderMoveAcceleration.getValue()).intValue()},
								    new int[] {((Number)xAxisMaxAcceleration.getValue()).intValue(),
									       ((Number)yAxisMaxAcceleration.getValue()).intValue(),
									       ((Number)zAxisMaxAcceleration.getValue()).intValue(),
									       ((Number)aAxisMaxAcceleration.getValue()).intValue(),
									       ((Number)bAxisMaxAcceleration.getValue()).intValue()},
								    new int[] {((Number)xAxisMaxSpeedChange.getValue()).intValue(),
									       ((Number)yAxisMaxSpeedChange.getValue()).intValue(),
									       ((Number)zAxisMaxSpeedChange.getValue()).intValue(),
									       ((Number)aAxisMaxSpeedChange.getValue()).intValue(),
									       ((Number)bAxisMaxSpeedChange.getValue()).intValue()}),
						new AccelParamsTab2(slowdownFlagBox.isSelected(),
								    new int[] {((Number)extruderDeprimeA.getValue()).intValue(),
									       ((Number)extruderDeprimeB.getValue()).intValue()},
								    new double[] {((Number)JKNAdvance1.getValue()).doubleValue(),
										  ((Number)JKNAdvance2.getValue()).doubleValue()}));
		 }

		 @Override
		 public boolean isAccelerationEnabled() {
			 return accelerationBox.isSelected();
		 }

		 private void setEEPROMFromUI(AccelParams params) {
			 target.setAccelerationStatus(params.tab1.accelerationEnabled ? (byte)1 : (byte)0);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_SLOWDOWN_FLAG, params.tab2.slowdownEnabled ? 1 : 0);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_NORM,    params.tab1.accelerations[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_RETRACT, params.tab1.accelerations[1]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_X, params.tab1.maxAccelerations[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Y, params.tab1.maxAccelerations[1]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Z, params.tab1.maxAccelerations[2]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_A, params.tab1.maxAccelerations[3]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_B, params.tab1.maxAccelerations[4]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_X, params.tab1.maxSpeedChanges[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Y, params.tab1.maxSpeedChanges[1]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Z, params.tab1.maxSpeedChanges[2]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_A, params.tab1.maxSpeedChanges[3]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_B, params.tab1.maxSpeedChanges[4]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K,  params.tab2.JKNadvance[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K2, params.tab2.JKNadvance[1]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_A, params.tab2.deprime[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_B, params.tab2.deprime[1]);
		 }

		 @Override
		 public void setEEPROMFromUI() {
			 setEEPROMFromUI(getAccelParamsFromUI());
		 }

		 private void setUIFields(AccelParamsTab1 params) {
			 setUIFields(UI_TAB_1,
				     params.accelerationEnabled,
				     false,
				     params.accelerations,
				     params.maxAccelerations,
				     params.maxSpeedChanges,
				     null,
				     null);
		 }

		 private void setUIFields(int tabs,
					  boolean accelerationEnabled,
					  boolean slowdownEnabled,
					  int[] accelerations,
					  int[] maxAccelerations,
					  int[] maxSpeedChanges,
					  double[] JKNadvance,
					  int[] deprime) {

			 if ((tabs & UI_TAB_1) != 0) {
				 accelerationBox.setSelected(accelerationEnabled);

				 if (accelerations != null) {
					 normalMoveAcceleration.setValue(accelerations[0]);
					 extruderMoveAcceleration.setValue(accelerations[1]);
				 }

				 if (maxAccelerations != null) {
					 xAxisMaxAcceleration.setValue(maxAccelerations[0]);
					 yAxisMaxAcceleration.setValue(maxAccelerations[1]);
					 zAxisMaxAcceleration.setValue(maxAccelerations[2]);
					 aAxisMaxAcceleration.setValue(maxAccelerations[3]);
					 bAxisMaxAcceleration.setValue(maxAccelerations[4]);
				 }

				 if (maxSpeedChanges != null) {
					 xAxisMaxSpeedChange.setValue(maxSpeedChanges[0]);
					 yAxisMaxSpeedChange.setValue(maxSpeedChanges[1]);
					 zAxisMaxSpeedChange.setValue(maxSpeedChanges[2]);
					 aAxisMaxSpeedChange.setValue(maxSpeedChanges[3]);
					 bAxisMaxSpeedChange.setValue(maxSpeedChanges[4]);
				 }
			 }

			 if ((tabs & UI_TAB_2) != 0) {
				 slowdownFlagBox.setSelected(slowdownEnabled);

				 if (JKNadvance != null) {
					 JKNAdvance1.setValue(JKNadvance[0]);
					 JKNAdvance2.setValue(JKNadvance[1]);
				 }

				 if (deprime != null) {
					 extruderDeprimeA.setValue(deprime[0]);
					 extruderDeprimeB.setValue(deprime[1]);
				 }
			 }

			 // Enable/disable the draft & quality buttons based upon the UI field values
			 // propertyChange(null);
		 }

		 @Override
		 public void setUIFromEEPROM() {
			 boolean accelerationEnabled = target.getAccelerationStatus() != 0;
			 boolean slowdownEnabled = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_SLOWDOWN_FLAG) != 0;
			 int[] maxAccelerations = new int[] {
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_X),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Y),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Z),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_A),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_B) };

			 int[] maxSpeedChanges = new int[] {
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_X),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Y),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Z),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_A),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_B) };

			 int[] accelerations = new int[] {
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_NORM),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_RETRACT) };

			 double[] JKNadvance = new double[] {
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K),
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K2) };

			 int[] deprime = new int[] {
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_A),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_B) };
		 
			 setUIFields(UI_TAB_1 | UI_TAB_2, accelerationEnabled, slowdownEnabled, 
				     accelerations, maxAccelerations, maxSpeedChanges,
				     JKNadvance, deprime);
		 }

		 @Override
		 public void buildUI() {
			 JPanel accelerationTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			 subTabs.addTab("Acceleration", accelerationTab);

			 JPanel accelerationMiscTab = new JPanel(new MigLayout("fill", "[r][l]"));
			 subTabs.addTab("Acceleration (Misc)", accelerationMiscTab);

			 normalMoveAcceleration.setColumns(8);
			 extruderMoveAcceleration.setColumns(8);

			 xAxisMaxAcceleration.setColumns(8);
			 xAxisMaxSpeedChange.setColumns(4);

			 yAxisMaxAcceleration.setColumns(8);
			 yAxisMaxSpeedChange.setColumns(4);

			 zAxisMaxAcceleration.setColumns(8);
			 zAxisMaxSpeedChange.setColumns(4);

			 aAxisMaxAcceleration.setColumns(8);
			 aAxisMaxSpeedChange.setColumns(4);

			 bAxisMaxAcceleration.setColumns(8);
			 bAxisMaxSpeedChange.setColumns(4);

			 addWithSharedToolTips(accelerationTab, "Acceleration enabled", accelerationBox, "wrap");

			 addWithSharedToolTips(accelerationTab,
					       "Max acceleration (magnitude of acceleration vector; mm/s\u00B2)", "span 2, gapleft push",
					       normalMoveAcceleration, "wrap, gapright push ");

			 addWithSharedToolTips(accelerationTab,
					       "Max acceleration for extruder-only moves (mm/s\u00B2)", "span 2, gapleft push", //"split 2, span 3",
					       extruderMoveAcceleration, "wrap, gapright push");

			 addWithSharedToolTips(accelerationTab, "X max acceleration (mm/s\u00B2)",
					       xAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "X max speed change (mm/s)",
					       xAxisMaxSpeedChange, "wrap");
 
			 addWithSharedToolTips(accelerationTab, "Y max acceleration (mm/s\u00B2)",
					       yAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Y max speed change (mm/s)",
					       yAxisMaxSpeedChange, "wrap");

			 addWithSharedToolTips(accelerationTab, "Z max acceleration (mm/s\u00B2)",
					       zAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Z max speed change (mm/s)",
					       zAxisMaxSpeedChange, "wrap");

			 addWithSharedToolTips(accelerationTab, "Right extruder max acceleration (mm/s\u00B2)",
					       aAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Right extruder max speed change (mm/s)",
					       aAxisMaxSpeedChange, "wrap");
 
			 addWithSharedToolTips(accelerationTab, "Left extruder acceleration (mm/s\u00B2)",
					       bAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Left extruder max speed change (mm/s)",
					       bAxisMaxSpeedChange, "wrap");

			 accelerationTab.add(qualityButton, "span 2, gapleft push");
			 accelerationTab.add(draftButton, "span 2, gapright push");

			 // Acceleration - Misc

			 JKNAdvance1.setColumns(8);
			 JKNAdvance2.setColumns(8);

			 extruderDeprimeA.setColumns(8);
			 extruderDeprimeB.setColumns(8);

			 addWithSharedToolTips(accelerationMiscTab, "Slow printing when acceleration planing falls behind", slowdownFlagBox, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "JKN Advance K", JKNAdvance1, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "JKN Advance K2", JKNAdvance2, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "Right extruder deprime (steps)", extruderDeprimeA, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "Left extruder deprime (steps)", extruderDeprimeB, "wrap");
		 }
	 }

	 // Thing-o-Matics and Cupcake's with the Jetty Firmware (3.2 and later)

	 private class G3FirmwareMachineOnboardAccelerationParameters extends MachineOnboardAccelerationParameters {

		 // Cupcake & Thing-o-Matic Jetty Firmware specific acceleration parameters

		 // Column width to wrap tool text tips to
		 final int width = defaultToolTipWidth;

		 // The following NumberFormats are used to restrict the various fields to
		 //   integer or floating point values only and to restrict the number of
		 //   whole or fractional digits permitted.
		 //
		 // Further we use a private routine for creating JFormattedTextFields.  This
		 //   private routine tweaks the formatter associated with the field to disallow
		 //   negative values and, in some cases, to place additional range restrictions
		 //   on the fields.  These restrictions cannot easily be placed on the NumberFormats
		 //   and are more easily applied by going after the Formatter used by the text field.

		 // Feedrate: nnnn (mm/s) [allows up to 10 meters/s which is pretty fast]
		 private NumberFormat frNF = NumberFormat.getIntegerInstance();
		 {
			 frNF.setMaximumIntegerDigits(4);
		 }

		 // Acceleration: nnnnnnnn (mm/s)
		 private NumberFormat accNF = NumberFormat.getIntegerInstance();

		 // Advance K1 & K2: nn.nnnnn
		 private NumberFormat kNF = NumberFormat.getNumberInstance();
		 {
			 kNF.setMaximumFractionDigits(5);
			 kNF.setMaximumIntegerDigits(2);   // Even 1 may be excessive
		 }

		 // Minimum Segment Time: 0.nnnn (seconds)
		 private NumberFormat minSegTimeNF = NumberFormat.getNumberInstance();
		 {
			 minSegTimeNF.setMaximumFractionDigits(4);
			 minSegTimeNF.setMaximumIntegerDigits(1);
		 }

		 // max speed change: nnnn.n (mm/s)
		 private NumberFormat jerkNF = NumberFormat.getNumberInstance();
		 {
			 jerkNF.setMaximumFractionDigits(1);
			 jerkNF.setMaximumIntegerDigits(4);
		 }

		 // Temp and other integer values >= 0
		 private NumberFormat tempNF = NumberFormat.getIntegerInstance();
		 {
			 tempNF.setMaximumIntegerDigits(3);
		 }

		 // Slowdown limit: single digit
		 private NumberFormat sdNF = NumberFormat.getIntegerInstance();
		 {
			 sdNF.setMaximumIntegerDigits(1);
		 }

		 private JCheckBox accelerationBox = new JCheckBox();
		 {
			 accelerationBox.setToolTipText(wrap2HTML(width, "Enable or disable printing with acceleration"));
		 }

		 private JCheckBox accelerationPlannerBox = new JCheckBox();
		 {
			 accelerationPlannerBox.setToolTipText(wrap2HTML(width,
                    "The accelerated stepper driver attempts to plan moves ahead of the actual printing.  " +
                    "This is done because planning can be slow relative to the time it takes to print small, " +
                    "fine details.  Planning ahead of printing avoids situations where printing pauses while " +
                    "waiting for the next accelerated move to be computed.  Printing pauses produce blobs " +
                    "and zits when unwanted plastic oozes at one spot on the build from the idle extruder. " +
                    "Consequently, it is strongly recommended that you leave the planner enabled."));
		 }

		 private JCheckBox accelerationStrangledBox = new JCheckBox();
		 {
			 accelerationStrangledBox.setToolTipText(wrap2HTML(width,
                    "By checking this box, the accelerated stepper driver will be used but acceleration " +
                    "will not be used.  Each segment will print at the target feedrate without gently " +
                    "accelerating up to (or down to) that speed.  Check this box when you wish to print " +
                    "an object without acceleration but using the accelerated stepper driver.  You may " +
		    "also want to decrease the per-axis max feedrates."));
		 }

		 private JCheckBox inverted5DExtruderBox = new JCheckBox();
		 {
			 inverted5DExtruderBox.setToolTipText(wrap2HTML(width,
				"Enabled when building models sliced using Volumetric 5D and prepared for " +
				"your printer using the makerbot4g driver."));
		 }

		 // Basic acceleration parameters

		 private JFormattedTextField xAxisMaxFeedrate = PositiveTextFieldInt(frNF,
           "The maximum feedrate along the X axis measured in units of mm/s.  " +
           "I.e., the maximum magnitude of the component of the feedrate vector along the X axis.");

		 private JFormattedTextField yAxisMaxFeedrate = PositiveTextFieldInt(frNF,
           "The maximum feedrate along the Y axis measured in units of mm/s.  " +
           "I.e., the maximum magnitude of the component of the feedrate vector along the Y axis.");

		 private JFormattedTextField zAxisMaxFeedrate = PositiveTextFieldInt(frNF,
           "The maximum feedrate along the Z axis measured in units of mm/s.  " +
           "I.e., the maximum magnitude of the component of the feedrate vector along the Z axis.");

		 private JFormattedTextField aAxisMaxFeedrate = PositiveTextFieldInt(frNF,
           "The maximum feedrate to be experienced by extruder in units of mm/s.  " +
           "I.e., the maximum magnitude of the component of the feedrate vector along the extruder's axis.");

		 // NOTE: could use masterAcceleration except it doesn't limit range to >= 0
		 private JFormattedTextField normalMoveAcceleration = PositiveTextFieldInt(accNF,
           "The maximum rate of acceleration for normal printing moves in which filament is extruded and " +
           "there is motion along any or all of the X, Y, or Z axes.  I.e., the maximum magnitude of the " +
	   "acceleration vector in units of millimeters per second squared, mm/s\u00B2.");

		 // NOTE: Could use _AxisAcceleration, except those do no limit range to >= 0

		 private JFormattedTextField xAxisMaxAcceleration = PositiveTextFieldInt(accNF,
	   "The maximum acceleration and deceleration along the X axis in units of mm/\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the X axis.");

		 private JFormattedTextField yAxisMaxAcceleration = PositiveTextFieldInt(accNF,
	   "The maximum acceleration and deceleration along the Y axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the Y axis.");

		 private JFormattedTextField zAxisMaxAcceleration = PositiveTextFieldInt(accNF,
	   "The maximum acceleration and deceleration along the Z axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the Z axis.");

		 private JFormattedTextField aAxisMaxAcceleration = PositiveTextFieldInt(accNF,
	   "The maximum acceleration and deceleration experienced by the extruder in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the extruder's axis.");

		 private JFormattedTextField xAxisMaxSpeedChange = PositiveTextFieldDouble(jerkNF,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the X axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the X axis.");

		 private JFormattedTextField yAxisMaxSpeedChange = PositiveTextFieldDouble(jerkNF,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the Y axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the Y axis.");

		 private JFormattedTextField zAxisMaxSpeedChange = PositiveTextFieldDouble(jerkNF,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the Z axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the Z axis.");

		 private JFormattedTextField aAxisMaxSpeedChange = PositiveTextFieldDouble(jerkNF,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the A axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the A axis.");

		 // Advanced accel, misc

		 private JFormattedTextField JKNAdvance1 = PositiveTextFieldDouble(kNF,
           "The value of the empirically fit Jetty-Kubicek-Newman Advance parameter K which helps control " +
           "the amount of additional plastic that should be extruded during the acceleration phase and not " +
           "extruded during the deceleration phase of each move.  It can be used to remove blobbing and " +
           "splaying on the corners of cubes or at the junctions between line segments.  Typical values " +
           "for this parameter range from around 0.0001 to 0.01.  Set to a value of 0 to disable use of " +
           "this compensation.");

		 private JFormattedTextField JKNAdvance2 = PositiveTextFieldDouble(kNF,
           "The value of the empirically fit Jetty-Kubicek-Newman Advance parameter K2, which helps during the " +
           "deceleration phase of moves to reduce built up pressure in the extruder nozzle.  Typical values for " +
           "this parameter range from around 0.001 to 0.02.  Set to a value of 0 to disable use of this " +
           "compensation.");

	
		 private JCheckBox clockwiseExtruderChoice = new JCheckBox();
		 {
			 clockwiseExtruderChoice.setToolTipText(wrap2HTML(width,
                   "Select the direction you need the extruder to turn in order to extrude filament.  For most " +
                   "Thing-o-Matic Stepstruders, this box is left unchecked when using 5D and checked when not " + 
		   "using 5D.  You may need to do the opposite if your extruder's stepper motor is wired " +
                   "differently.  The same effect can be had by inverting the A-axis; this option primarily " +
		   "exists for users with a Gen 4 LCD interface."));
		 }

		 private JFormattedTextField extruderStepsPerMM = PositiveTextFieldDouble(jerkNF,
           "The number of extruder steps required to extrude a single millimeter of filament.  Not to be confused with " +
           "the number of extruder steps per millimeter of raw, input filament -- the \"stepspermm\" value used by " +
           "ReplicatorG.  Typical values for a Stepstruder Mk7 with 1.75mm filament range from about 3.8 to 4.8 " +
           "steps/mm.  For a Mk6 with 3.00 mm filament, typical values range from about 1.2 to 1.7.");

		 private JFormattedTextField extruderDeprime = PositiveTextFieldDouble(jerkNF,
           "The number of millimeters to retract the extruded filament when the pipeline of buffered moves empties or " +
           "a travel-only move is encountered.  The default value is 4.0 mm.  Set to a value of 0 to disable this " +
           "feature.  Do not use with Skeinforge's Reversal plugin nor Skeinforge's Dimension plugin's " +
	   "\"Retraction Distance\".");

		 private JFormattedTextField revMaxFeedrate = PositiveTextFieldInt(frNF,
            "The maximum feedrate in mm/s to use in an extruder-only move.  An extruder-only move is a move in which " +
            "there is no motion along the X, Y, or Z axes: the only motion is the extruder extruding or retracting " +
            "filament.  Typically this value should be at least as large as the max A axis feedrate.");

		 private JFormattedTextField extruderMoveAcceleration = PositiveTextFieldInt(accNF,
            "The maximum acceleration or deceleration in mm/s\u00B2 to use in an extruder-only move.  An extruder-only " +
            "move is a move in which there is no motion along the X, Y, or Z axes: the only motion is the extruder " +
            "extruding or retracting filament.  Typically this value should be at least as large as the A axis max " +
	    "acceleration.");

		 private JFormattedTextField minFeedrate = PositiveTextFieldDouble(jerkNF,
            "The slowest feedrate to print at, in mm/s.  Use of this may help prevent some forms of blobbing.  " +
            "This feature is useful when using older versions of Skeinforge which did not offer this functionality.");

		 private JFormattedTextField minTravelFeedrate = PositiveTextFieldDouble(jerkNF,
            "The slowest feedrate to travel at, in mm/s.  This feature is useful when using older versions of " +
            "Skeinforge which did not offer this functionality.");

		 // NOTE: Could use minimumSpeed except that it doesn't limit range to >= 0
		 private JFormattedTextField minPlannerSpeed = PositiveTextFieldInt(frNF,
            "The slowest feedrate in mm/s to use at the junctions between adjoining line segments.  It is " +
            "unlikely that this parameter needs to be adjusted.  Increasing its value will not speed up " +
            "your printing.  Nearly always the acceleration planner ends up with junction speeds far in excess " +
            "of this value (but within the maximum feedrate and acceleration limits).");

		 private JFormattedTextField minSegmentTime = PositiveTextFieldDouble(minSegTimeNF,
            "The minimum time in seconds that printing a segment should take when the planning buffer " +
            "is running dry (i.e., printing is proceeding faster than the microprocessor can plan ahead).  " +
            "Increase this value if you see blobs or pauses while printing models with very fine " +
            "details or when printing at high speed.");

		 private JFormattedTextField slowDownLimit = PositiveTextFieldInt(sdNF, 8,
            "WARNING: you probably do not want to adjust this parameter.  The slowdown limit sets " +
            "a threshhold for the plan ahead logic instructing it when to start enforcing the " +
            "minimum segment print time.  The larger the value, the sooner it starts enforcing the " +
            "slowdown limit.  Acceptable values are 3, 4, 5, 6, 7, and 8.  A value of 0 disables " +
            "this feature.  Any attempt to use a value of 1 or 2 will result in a value of 3 being " +
            "used instead.");
 
		 // Misc. Jetty params not specific to the Gen 4 LCD interface

		 private final String[] lcdDimensionChoices = { "16 x 4", "20 x 4", "24 x 4" };
		 private JComboBox lcdDimensionsChoice = new JComboBox(lcdDimensionChoices);
		 {
			 lcdDimensionsChoice.setToolTipText(wrap2HTML(width,
		   "Select the dimensions of the LCD screen display in your Gen 4 LCD " +
		   "interface.  Measurements are the number of character columns by the " +
                   "number of character rows.  The standard Gen 4 LCD display is 16 x 4."));
		 }

		 public final int moodLightChoiceIndex_DEFAULT = 0; // Default choice in moodLightChoices
		 public final int moodLightScriptId_DEFAULT = 2; // Default Script Id
		 public final String[] moodLightChoices = {
			 "Off (2)",
			 "Bot status (0)",
			 "Custom color (1)",
			 "Almond (12)",
			 "Blue (6)",
			 "Blue, Alice (15)",
			 "Blue, Deep Sky (22)",
			 "Blue, Midnight (21)",
			 "Cyan (7)",
			 "Gold (25)",
			 "Gray (19)",
			 "Gray, Light (20)",
			 "Gray, Slate (18)",
			 "Green (5)",
			 "Green, Forest (24)",
			 "Green, Olive (23)",
			 "Hot Pink (26)",
			 "Lavender (16)",
			 "Linen (27)",
			 "Magenta (8)",
			 "Mint Cream (14)",
			 "Orange (11)",
			 "Peach Puff (13)",
			 "Purple (10)",
			 "Red (4)",
			 "Rose, Misty (17)",
			 "White (3)",
			 "Yellow (9)",
			 "Cycle Rainbow (110)",
			 "Cycle Random (111)",
			 "Cycle Red/Green/Blue (101)",
			 "Cycle S.O.S. (118)",
			 "Cycle Seasons (115)",
			 "Cycle Traffic Lights (117)",
			 "Cycle White/Red/Green/Blue/Off (100)",
			 "Random Candle (112)",
			 "Random Neon Reds (114)",
			 "Random Thunderstorms (116)",
			 "Random Water (113)",
			 "Flashing Blue (105)",
			 "Flashing Cyan (106)",
			 "Flashing Green (104)",
			 "Flashing Magenta (107)",
			 "Flashing Red (103)",
			 "Flashing White (102)",
			 "Flashing Yellow (108)"
		 };

		 private JCheckBox overrideGCodeTempBox = new JCheckBox();
		 {
			 overrideGCodeTempBox.setToolTipText(wrap2HTML(width,
                    "When enabled, override the gcode temperature settings using the preheat " +
		    "temperature settings for the extruders and build platform."));
		 }
           
		 private JFormattedTextField tool0Temp = PositiveTextFieldInt(tempNF, 255,
           "Temperature in degrees Celsius to preheat extruder 0 to.  This temperature is " +
           "also used as the override temperature when the \"override gcode temperature\" " +
           "feature is enabled.");

		 private JFormattedTextField tool1Temp = PositiveTextFieldInt(tempNF, 255,
           "Temperature in degrees Celsius to preheat extruder 1 to.  This temperature is " +
           "also used as the override temperature when the \"override gcode temperature\" " +
           "feature is enabled.");

		 private JFormattedTextField platformTemp = PositiveTextFieldInt(tempNF, 255,
           "Temperature in degrees Celsius to preheat the build platform to.  This temperature is " +
           "also used as the override temperature when the \"override gcode temperature\" " +
           "feature is enabled.");

		 private JFormattedTextField buzzerRepeats = PositiveTextFieldInt(tempNF, 255,
           "The number of times the buzzer should buzz when activated.  Use of this feature " +
           "requires installation of a buzzer as per Thingiverse Thing 16170, \"Buzzer Support\".");

		 private JColorChooser moodLightCustomColor = new JColorChooser();
		 {
			 moodLightCustomColor.setToolTipText(wrap2HTML(width,
		   "Select the custom color used when the \"Custom Color\" mood light script " +
		   "is selected.  Use of this feature requires installation of Thingiverse " +
		   "Thing 15347, \"Mood Lighting For ToM\"."));
		 }

		 private JComboBox moodLightScript = new JComboBox(moodLightChoices);
		 {
			 moodLightScript.setToolTipText(wrap2HTML(width,
                   "Select the mood light script to be played by the mood lighting.  Use of this " +
		   "feature requires installation of Thingiverse Thing 15347, \"Mood Lighting " +
                   "For ToM\"."));
		 }

		 private G3FirmwareMachineOnboardAccelerationParameters(OnboardParameters target,
									 Driver driver,
									 JTabbedPane subtabs) {
			 super(target, driver, subtabs);

			 int dismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
			 if (dismissDelay < 10*1000)
				 ToolTipManager.sharedInstance().setDismissDelay(10*1000);
		 }

		 // Given a Mood Light Script Id, find the corresponding choice in the
		 // Mood Light choice list
		 public int findMoodLightChoice(int scriptId) {
			 String str = "(" + scriptId + ")";
			 for (int i = 0; i < moodLightChoices.length; i++) {
				 if (moodLightChoices[i].contains(str))
					 return i;
			 }
			 return -1;
		 }

		 // Given an index in the list of Mood Light choices, find the corresponding Script Id
		 public int findMoodLightScriptId(int choiceIndex) {
			 if (choiceIndex < 0 || choiceIndex > moodLightChoices.length)
				 return -1;
			 int start = moodLightChoices[choiceIndex].indexOf('(');
			 if (start < 0)
				 return -1;
			 ++start;
			 int end = moodLightChoices[choiceIndex].indexOf(')', start);
			 if (end < 0)
				 return -1;
			 int scriptId = -1;
			 try {
				 scriptId = Integer.parseInt(moodLightChoices[choiceIndex].substring(start, end));
			 }
			 catch (Exception e) {
			 }
			 return scriptId;
		 }

		 @Override
		 public boolean isAccelerationEnabled() {
			 return accelerationBox.isSelected();
		 }

		 @Override
		 public void setEEPROMFromUI() {
			 // Jetty firmware
			 byte status = (byte)0;
			 if (accelerationBox.isSelected())
				 status |= (byte)0x01;
			 if (accelerationPlannerBox.isSelected())
				 status |= (byte)0x02;
			 if (accelerationStrangledBox.isSelected())
				 status |= (byte)0x04;
			 target.setAccelerationStatus(status);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_NORM,
					       ((Number)normalMoveAcceleration.getValue()).longValue());

			 // TODO: Warn when the max feedrates in the machine defs are less than these values

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_X,
					       ((Number)xAxisMaxFeedrate.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_Y,
					       ((Number)yAxisMaxFeedrate.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_Z,
					       ((Number)zAxisMaxFeedrate.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_A,
					       ((Number)aAxisMaxFeedrate.getValue()).longValue());

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_X,
					       ((Number)xAxisMaxAcceleration.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Y,
					       ((Number)yAxisMaxAcceleration.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Z,
					       ((Number)zAxisMaxAcceleration.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_A,
					       ((Number)aAxisMaxAcceleration.getValue()).longValue());

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_X,
					       ((Number)xAxisMaxSpeedChange.getValue()).doubleValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Y,
					       ((Number)yAxisMaxSpeedChange.getValue()).doubleValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Z,
					       ((Number)zAxisMaxSpeedChange.getValue()).doubleValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_A,
					       ((Number)aAxisMaxSpeedChange.getValue()).doubleValue());

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MIN_PLANNER_SPEED,
					       ((Number)minPlannerSpeed.getValue()).longValue());

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K,
					       ((Number)JKNAdvance1.getValue()).doubleValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K2,
					       ((Number)JKNAdvance2.getValue()).doubleValue());

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_E_STEPS_PER_MM,
					       ((Number)extruderStepsPerMM.getValue()).doubleValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_REV_MAX_FEED_RATE,
					       ((Number)revMaxFeedrate.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_RETRACT,
					       ((Number)extruderMoveAcceleration.getValue()).longValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_A,
					       ((Number)extruderDeprime.getValue()).doubleValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_CLOCKWISE_EXTRUDER,
					       clockwiseExtruderChoice.isSelected() ? 1L : 0L);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MIN_FEED_RATE,
					       ((Number)minFeedrate.getValue()).doubleValue());
		 	 target.setEEPROMParam(OnboardParameters.EEPROMParams.INVERTED_EXTRUDER_5D,
					       inverted5DExtruderBox.isSelected() ? 1 : 0);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MIN_TRAVEL_FEED_RATE,
					       ((Number)minTravelFeedrate.getValue()).doubleValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MIN_SEGMENT_TIME,
					       ((Number)minSegmentTime.getValue()).doubleValue());
			 long lv = ((Number)slowDownLimit.getValue()).longValue();
			 if (lv < 0)
				 lv = 0;
			 else if (lv == 1 || lv == 2)
				 lv = 3;
			 else if (lv > 8)
				 lv = 8;
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_SLOWDOWN_LIMIT, lv);

			 int lcd = lcdDimensionsChoice.getSelectedIndex();
			 int lcdType = 0; // lcd == 0 --> lcdType = 0
			 if (lcd == 1) lcdType = 50;
			 else if (lcd == 2) lcdType = 51;
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.LCD_TYPE, lcdType);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.OVERRIDE_GCODE_TEMP,
					       overrideGCodeTempBox.isSelected() ? 1 : 0);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.TOOL0_TEMP,
					       ((Number)tool0Temp.getValue()).intValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.TOOL1_TEMP,
					       ((Number)tool0Temp.getValue()).intValue());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.PLATFORM_TEMP,
					       ((Number)platformTemp.getValue()).intValue());

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.BUZZER_REPEATS,
					       ((Number)buzzerRepeats.getValue()).intValue());

			 int scriptId = findMoodLightScriptId(moodLightScript.getSelectedIndex());
			 if (scriptId < 0)
				 scriptId = moodLightScriptId_DEFAULT;
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_SCRIPT, scriptId);
			 Color c = moodLightCustomColor.getColor();
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_RED,   c.getRed());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_GREEN, c.getGreen());
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_BLUE,  c.getBlue());
		 }

		 @Override
		 public void setUIFromEEPROM() {
			 byte aStatus = target.getAccelerationStatus();
			 accelerationBox.setSelected((aStatus & 0x01) != 0);
			 accelerationPlannerBox.setSelected((aStatus & 0x02) != 0);
			 accelerationStrangledBox.setSelected((aStatus & 0x04) != 0);

			 xAxisMaxFeedrate.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_X));
			 yAxisMaxFeedrate.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_Y));
			 zAxisMaxFeedrate.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_Z));
			 aAxisMaxFeedrate.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_FEEDRATE_A));

			 xAxisMaxAcceleration.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_X));
			 yAxisMaxAcceleration.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Y));
			 zAxisMaxAcceleration.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Z));
			 aAxisMaxAcceleration.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_A));

			 xAxisMaxSpeedChange.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_X));
			 yAxisMaxSpeedChange.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Y));
			 zAxisMaxSpeedChange.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Z));
			 aAxisMaxSpeedChange.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_A));

			 JKNAdvance1.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K));
			 JKNAdvance2.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K2));
 
			 minPlannerSpeed.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MIN_PLANNER_SPEED));
			 normalMoveAcceleration.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_NORM));

			 extruderStepsPerMM.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_E_STEPS_PER_MM));
			 revMaxFeedrate.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_REV_MAX_FEED_RATE));
			 extruderMoveAcceleration.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_RETRACT));
			 extruderDeprime.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_A));
			 clockwiseExtruderChoice.setSelected(0L != target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_CLOCKWISE_EXTRUDER));
			 inverted5DExtruderBox.setSelected(0 != target.getEEPROMParamInt(OnboardParameters.EEPROMParams.INVERTED_EXTRUDER_5D));
			 minFeedrate.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MIN_FEED_RATE));
			 minTravelFeedrate.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MIN_TRAVEL_FEED_RATE));
			 minSegmentTime.setValue(target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MIN_SEGMENT_TIME));
			 slowDownLimit.setValue(target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_SLOWDOWN_LIMIT));

			 int lcdType = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.LCD_TYPE);
			 if (lcdType == 50) lcdDimensionsChoice.setSelectedIndex(1);
			 else if (lcdType == 51) lcdDimensionsChoice.setSelectedIndex(2);
			 else lcdDimensionsChoice.setSelectedIndex(0);

			 overrideGCodeTempBox.setSelected(target.getEEPROMParamInt(OnboardParameters.EEPROMParams.OVERRIDE_GCODE_TEMP) != 0);
			 tool0Temp.setValue(target.getEEPROMParamInt(OnboardParameters.EEPROMParams.TOOL0_TEMP));
			 tool1Temp.setValue(target.getEEPROMParamInt(OnboardParameters.EEPROMParams.TOOL1_TEMP));
			 platformTemp.setValue(target.getEEPROMParamInt(OnboardParameters.EEPROMParams.PLATFORM_TEMP));

			 buzzerRepeats.setValue(target.getEEPROMParamInt(OnboardParameters.EEPROMParams.BUZZER_REPEATS));
			 int moodLightChoice = findMoodLightChoice(target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_SCRIPT));
			 if (moodLightChoice < 0)
				 moodLightChoice = moodLightChoiceIndex_DEFAULT;
			 moodLightScript.setSelectedIndex(moodLightChoice);
			 moodLightCustomColor.setColor(target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_RED),
						       target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_GREEN),
						       target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_BLUE));
		 }

		 @Override
		 public void buildUI() {
			 JPanel accelerationTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			 subTabs.addTab("Acceleration", accelerationTab);

			 JPanel accelerationMiscTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			 subTabs.addTab("Acceleration (Advanced)", accelerationMiscTab);

			 JPanel miscTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			 subTabs.addTab("Misc", miscTab);

			 normalMoveAcceleration.setColumns(8);

			 xAxisMaxFeedrate.setColumns(4);
			 xAxisMaxAcceleration.setColumns(8);

			 yAxisMaxFeedrate.setColumns(4);
			 yAxisMaxAcceleration.setColumns(8);

			 zAxisMaxFeedrate.setColumns(4);
			 zAxisMaxAcceleration.setColumns(8);

			 aAxisMaxFeedrate.setColumns(4);
			 aAxisMaxAcceleration.setColumns(8);

			 xAxisMaxSpeedChange.setColumns(4);
			 yAxisMaxSpeedChange.setColumns(4);
			 zAxisMaxSpeedChange.setColumns(4);
			 aAxisMaxSpeedChange.setColumns(4);

			 addWithSharedToolTips(accelerationTab, "Acceleration enabled", "split 2", accelerationBox);
			 addWithSharedToolTips(accelerationTab,
					       "Max acceleration (magnitude of acceleration vector; mm/s\u00B2)", "span 2, gapleft push",
					       normalMoveAcceleration, "wrap");

			 addWithSharedToolTips(accelerationTab, "X max feedrate (mm/s)", xAxisMaxFeedrate);
			 addWithSharedToolTips(accelerationTab, "X max acceleration (mm/s\u00B2)",
					       xAxisMaxAcceleration, "wrap");

			 addWithSharedToolTips(accelerationTab, "Y max feedrate (mm/s)", yAxisMaxFeedrate);
			 addWithSharedToolTips(accelerationTab, "Y max acceleration (mm/s\u00B2)", yAxisMaxAcceleration, "wrap");

			 addWithSharedToolTips(accelerationTab, "Z max feedrate (mm/s)", zAxisMaxFeedrate);
			 addWithSharedToolTips(accelerationTab, "Z max acceleration (mm/s\u00B2)",
					       zAxisMaxAcceleration, "wrap");

			 addWithSharedToolTips(accelerationTab, "A max feedrate (mm/s)", aAxisMaxFeedrate);
			 addWithSharedToolTips(accelerationTab, "A max acceleration (mm/s\u00B2)", aAxisMaxAcceleration, "wrap");

			 addWithSharedToolTips(accelerationTab, "X max speed change (mm/s)",
					       xAxisMaxSpeedChange, "span 2, wrap");

			 addWithSharedToolTips(accelerationTab, "Y max speed change (mm/s)",
					       yAxisMaxSpeedChange, "span 2, wrap");

			 addWithSharedToolTips(accelerationTab, "Z max speed change (mm/s)",
					       zAxisMaxSpeedChange, "span 2, wrap");

			 addWithSharedToolTips(accelerationTab, "A max speed change (mm/s)",
					       aAxisMaxSpeedChange, "span 2, wrap");

			 // Acceleration - Misc

			 JKNAdvance1.setColumns(8);
			 JKNAdvance2.setColumns(8);
			 extruderMoveAcceleration.setColumns(8);
			 revMaxFeedrate.setColumns(4);
			 minPlannerSpeed.setColumns(4);
			 minSegmentTime.setColumns(4);
			 slowDownLimit.setColumns(4);

			 extruderDeprime.setColumns(4);
			 extruderStepsPerMM.setColumns(4);
			 minFeedrate.setColumns(4);
			 minTravelFeedrate.setColumns(4);

			 addWithSharedToolTips(accelerationMiscTab, "JKN Advance K", JKNAdvance1);
			 addWithSharedToolTips(accelerationMiscTab, "Extruder deprime (mm)",
					       extruderDeprime, "wrap");

			 addWithSharedToolTips(accelerationMiscTab, "JKN Advance K2", JKNAdvance2);
			 addWithSharedToolTips(accelerationMiscTab, "Extruder steps/mm",
					       extruderStepsPerMM, "wrap");

			 addWithSharedToolTips(accelerationMiscTab,
					       "Max acceleration for extruder-only moves (mm/s\u00B2)",
					       extruderMoveAcceleration);
			 addWithSharedToolTips(accelerationMiscTab, "Min printing feedrate (mm/s)",
					       minFeedrate, "wrap");

			 addWithSharedToolTips(accelerationMiscTab,
					       "Max feedrate for extruder-only moves (mm/s)",
					       revMaxFeedrate);
			 addWithSharedToolTips(accelerationMiscTab, "Min travel feedrate (mm/s)",
					       minTravelFeedrate, "wrap");
			
			 addWithSharedToolTips(accelerationMiscTab, "Min feedrate at junctions (mm/s)",
					       minPlannerSpeed);
			 addWithSharedToolTips(accelerationMiscTab, "Clockwise extruder",
					       clockwiseExtruderChoice, "wrap");

			 addWithSharedToolTips(accelerationMiscTab, "Min segment printing time (s)",
					       minSegmentTime);
			 addWithSharedToolTips(accelerationMiscTab, "Acceleration planner enabled",
					       accelerationPlannerBox, "wrap");

			 addWithSharedToolTips(accelerationMiscTab,
					       "Planner slowdown limit", "gapbottom push",
					       slowDownLimit, "gapbottom push");
			 addWithSharedToolTips(accelerationMiscTab, "Acceleration strangled", "gapbottom push",
					       accelerationStrangledBox, "wrap");

			 // Misc tab
			 tool0Temp.setColumns(8);
			 tool1Temp.setColumns(8);
			 platformTemp.setColumns(8);
			 buzzerRepeats.setColumns(8);

			 addWithSharedToolTips(miscTab, "Override gcode temperatures", overrideGCodeTempBox);
			 addWithSharedToolTips(miscTab, "Gen 4 LCD dimensions", lcdDimensionsChoice, "wrap");

			 addWithSharedToolTips(miscTab, "Extruder 0 preheat & override temperature (C)",
					       tool0Temp);
			 addWithSharedToolTips(miscTab, "Buzzer repeats", buzzerRepeats, "wrap");

			 addWithSharedToolTips(miscTab, "Extruder 1 preheat & override temperature (C)",
					       tool1Temp);

			 addWithSharedToolTips(miscTab, "Clockwise extruder", clockwiseExtruderChoice, "wrap");

			 addWithSharedToolTips(miscTab, "Platform preheat & override temperature (C)",
					       platformTemp);
			 addWithSharedToolTips(miscTab, "Volumetric 5D extruder", inverted5DExtruderBox, "wrap");

			 addWithSharedToolTips(miscTab, "Mood light script", moodLightScript, "span 4, wrap");
			 addWithSharedToolTips(miscTab, "Mood light color",
					       moodLightCustomColor, "span 4, wrap, gapbottom push, gapright push");
		 }
	 }

	 // Sailfish
	 // We could implement this class as an extension of the JettyMightyBoardMachineOnboardAccelerationParameters
	 // class.  But so doing then makes the Sailfish firmware dependent upon the tool tip wording and range
	 // changes selected by MBI.

	 private class SailfishG3MachineOnboardAccelerationParameters extends MachineOnboardAccelerationParameters {

		 // Sailfish Firmware specific acceleration parameters

		 // Accel Parameters of Tab 1
		 class AccelParamsTab1 {

			 boolean accelerationEnabled;
			 long[] accelerations;
			 long[] maxAccelerations;
			 double[] maxSpeedChanges;

			 AccelParamsTab1(boolean accelerationEnabled,
					 long[] accelerations,
					 long[] maxAccelerations,
					 double[] maxSpeedChanges)
			 {
				 this.accelerationEnabled = accelerationEnabled;
				 this.accelerations       = accelerations;
				 this.maxAccelerations    = maxAccelerations;
				 this.maxSpeedChanges     = maxSpeedChanges;
			 }
		 }

		 // Accel Parameters of Tab 2
		 class AccelParamsTab2 {

			 boolean slowdownEnabled;
			 long[] deprime;
			 double[] JKNadvance;

			 AccelParamsTab2(boolean slowdownEnabled,
					 long[] deprime,
					 double[] JKNadvance)
			 {
				 this.slowdownEnabled           = slowdownEnabled;
				 this.deprime                   = deprime;
				 this.JKNadvance                = JKNadvance;
			 }
		 }

		 // Accel Parameters of Tab 3
		 class AccelParamsTab3 {

			 boolean overrideGCodeTempEnabled;
			 boolean dittoEnabled;
       boolean extruderHold;
			 int buzzerRepeats;
			 int lcdType;
			 int scriptId;
			 int [] rgb;
			 int [] overrideTemps;

			 AccelParamsTab3(boolean overrideGCodeTempEnabled,
					 boolean dittoEnabled,
           boolean extruderHold,
					 int buzzerRepeats,
					 int lcdType,
					 int scriptId,
					 int [] rgb,
					 int [] overrideTemps)
			 {
				 this.overrideGCodeTempEnabled = overrideGCodeTempEnabled;
				 this.dittoEnabled             = dittoEnabled;
         this.extruderHold             = extruderHold;
				 this.buzzerRepeats            = buzzerRepeats;
				 this.lcdType                  = lcdType;
				 this.scriptId                 = scriptId;
				 this.rgb                      = rgb;
				 this.overrideTemps            = overrideTemps;
			 }
		 }

		 private class AccelParams {
			 public AccelParamsTab1 tab1;
			 public AccelParamsTab2 tab2;
			 public AccelParamsTab3 tab3;

			 AccelParams(AccelParamsTab1 params1, AccelParamsTab2 params2, AccelParamsTab3 params3) {
				 this.tab1 = params1;
				 this.tab2 = params2;
				 this.tab3 = params3;
			 }
		 }

		 AccelParamsTab1 draftParams = new AccelParamsTab1(true,                                            // acceleration enabled
								   new long[]   {2000L, 2000L},                     // p_accel, p_retract_accel
								   new long[]   {1000L, 1000L, 150L, 2000L, 2000L}, // max accelerations x,y,z,a,b
								   new double[] {40.0, 40.0, 10.0, 40.0, 40.0});    // max speed changes x,y,z,a,b

		 AccelParamsTab1 qualityParams = new AccelParamsTab1(true,                                            // acceleration enabled
								     new long[]   {2000L, 2000L},                     // p_accel, p_retract_accel
								     new long[]   {1000L, 1000L, 150L, 2000L, 2000L}, // max accelerations x,y,z,a,b
								     new double[] {15.0, 15.0, 10.0, 20.0, 20.0});    // max speed changes x,y,z,a,b

		 // Column width for formatting tool tip text
		 final int width = defaultToolTipWidth;

		 // Many of the values stored in EEPROM for the replicator are uint16_t
		 //   So, we want 0 < val < 0xffff

		 public final int moodLightChoiceIndex_DEFAULT = 0; // Default choice in moodLightChoices
		 public final int moodLightScriptId_DEFAULT = 2; // Default Script Id
		 public final String[] moodLightChoices = {
			 "Off (2)",
			 "Bot status (0)",
			 "Custom color (1)",
			 "Almond (12)",
			 "Blue (6)",
			 "Blue, Alice (15)",
			 "Blue, Deep Sky (22)",
			 "Blue, Midnight (21)",
			 "Cyan (7)",
			 "Gold (25)",
			 "Gray (19)",
			 "Gray, Light (20)",
			 "Gray, Slate (18)",
			 "Green (5)",
			 "Green, Forest (24)",
			 "Green, Olive (23)",
			 "Hot Pink (26)",
			 "Lavender (16)",
			 "Linen (27)",
			 "Magenta (8)",
			 "Mint Cream (14)",
			 "Orange (11)",
			 "Peach Puff (13)",
			 "Purple (10)",
			 "Red (4)",
			 "Rose, Misty (17)",
			 "White (3)",
			 "Yellow (9)",
			 "Cycle Rainbow (110)",
			 "Cycle Random (111)",
			 "Cycle Red/Green/Blue (101)",
			 "Cycle S.O.S. (118)",
			 "Cycle Seasons (115)",
			 "Cycle Traffic Lights (117)",
			 "Cycle White/Red/Green/Blue/Off (100)",
			 "Random Candle (112)",
			 "Random Neon Reds (114)",
			 "Random Thunderstorms (116)",
			 "Random Water (113)",
			 "Flashing Blue (105)",
			 "Flashing Cyan (106)",
			 "Flashing Green (104)",
			 "Flashing Magenta (107)",
			 "Flashing Red (103)",
			 "Flashing White (102)",
			 "Flashing Yellow (108)"
		 };

		 private NumberFormat repNF = NumberFormat.getIntegerInstance();

		 // Temp and other integer values >= 0
		 private NumberFormat tempNF = NumberFormat.getIntegerInstance();
		 {
			 tempNF.setMaximumIntegerDigits(3);
		 }

		 private JCheckBox accelerationBox = new JCheckBox();
		 {
			 accelerationBox.setToolTipText(wrap2HTML(width, "Enable or disable printing with acceleration"));
		 }

		 private JCheckBox dittoBox = new JCheckBox();
		 {
			 dittoBox.setToolTipText(wrap2HTML(width,
           "Enable ditto printing in which two copies of the build will be simultaneously printed, one copy with the " +
           "left extruder and the other with the right extruder.  The object must be such that the print heads will not " +
	   "interfere with one another; the firmware will not automatically guard against that."));
		 }

      private JCheckBox extruderHoldBox = new JCheckBox();
       {
           extruderHoldBox.setToolTipText(wrap2HTML(width,
         "Check this box if using a 3mm filament extruder.  Using extruder hold causes the extruder stepper motors " +
         "to remain engaged throughout the entire build regardless of whether or not the gcode requests that they " +
         "be disabled via M103 commands.  When 3mm filament extruder stepper motors are disabled, the filament has " +
     "a tendency to back out a tiny amount owing to the high pressure within the melt chamber of a 3mm extruder."));
       } 

		 private JButton draftButton = new JButton("Quick Draft");
		 {
			 draftButton.setToolTipText(wrap2HTML(width,
           "By clicking this button, the on-screen acceleration parameters will be changed to suggested values for " +
	   "rapid draft-quality builds.  The values will not be committed to your Replicator until you click the " +
	   "Commit button.  You may adjust the settings before committing them to your Replicator."));
		 }

		 private JButton qualityButton = new JButton("Fine Quality");
		 {
			 qualityButton.setToolTipText(wrap2HTML(width,
           "By clicking this button, the on-screen acceleration parameters will be changed to suggested values for " +
	   "fine-quality builds.  The values will not be committed to your Replicator until you click the " +
	   "Commit button.  You may adjust the settings before committing them to your Replicator."));
		 }

		 private JFormattedTextField xAxisMaxAcceleration = PositiveTextFieldInt(repNF, 22000,
	   "The maximum acceleration and deceleration along the X axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the X axis.");

		 private JFormattedTextField yAxisMaxAcceleration = PositiveTextFieldInt(repNF, 22000,
	   "The maximum acceleration and deceleration along the Y axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the Y axis.");

		 private JFormattedTextField zAxisMaxAcceleration = PositiveTextFieldInt(repNF, 5200,
	   "The maximum acceleration and deceleration along the Z axis in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the Z axis.");

		 private JFormattedTextField aAxisMaxAcceleration = PositiveTextFieldInt(repNF, 20000,
	   "The maximum acceleration and deceleration experienced by the right extruder in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the right extruder's filament axis.");

		 private JFormattedTextField bAxisMaxAcceleration = PositiveTextFieldInt(repNF, 20000,
	   "The maximum acceleration and deceleration experienced by the left extruder in units of mm/s\u00B2.  " +
	   "I.e., the maximum magnitude of the component of the acceleration vector along the left extruder's filament axis.");

		 private JFormattedTextField xAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the X axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the X axis.");

		 private JFormattedTextField yAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the Y axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the Y axis.");

		 private JFormattedTextField zAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate along the Z axis when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change along the Z axis.");

		 private JFormattedTextField aAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate for the right extruder when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change for the right extruder.");

		 private JFormattedTextField bAxisMaxSpeedChange = PositiveTextFieldInt(repNF, 300,
           "Yet Another Jerk (YAJ) algorithm's maximum change in feedrate for the left extruder when " +
           "transitioning from one printed segment to another, measured in units of mm/s.  I.e., the " +
           "maximum magnitude of the component of the velocity change for the left extruder.");

		 private JFormattedTextField normalMoveAcceleration = PositiveTextFieldInt(repNF, 20000,
           "The maximum rate of acceleration for normal printing moves in which filament is extruded and " +
           "there is motion along any or all of the X, Y, or Z axes.  I.e., the maximum magnitude of the " +
	   "acceleration vector in units of millimeters per second squared, mm/s\u00B2.");

		 private JFormattedTextField extruderMoveAcceleration = PositiveTextFieldInt(repNF, 20000,
            "The maximum acceleration or deceleration in mm/s\u00B2 to use in an extruder-only move.  An extruder-only " +
            "move is a move in which there is no motion along the X, Y, or Z axes: the only motion is the extruder " +
            "extruding or retracting filament.  Typically this value should be at least as large as the A and B axis max " +
	    "accelerations.");

		 // Advance K1 & K2: nn.nnnnn
		 private NumberFormat kNF = NumberFormat.getNumberInstance();
		 {
			 kNF.setMaximumFractionDigits(5);
			 kNF.setMaximumIntegerDigits(2);   // Even 1 may be excessive
		 }

		 private JFormattedTextField JKNAdvance1 = PositiveTextFieldDouble(kNF,
           "The value of the empirically fit Jetty-Kubicek-Newman Advance parameter K which helps control " +
           "the amount of additional plastic that should be extruded during the acceleration phase and not " +
           "extruded during the deceleration phase of each move.  It can be used to remove blobbing and " +
           "splaying on the corners of cubes or at the junctions between line segments.  Typical values " +
           "for this parameter range from around 0.0001 to 0.01.  Set to a value of 0 to disable use of " +
           "this compensation.");

		 private JFormattedTextField JKNAdvance2 = PositiveTextFieldDouble(kNF,
           "The value of the empirically fit Jetty-Kubicek-Newman Advance parameter K2, which helps during the " +
           "deceleration phase of moves to reduce built up pressure in the extruder nozzle.  Typical values for " +
           "this parameter range from around 0.001 to 0.1.  Set to a value of 0 to disable use of this " +
           "compensation.");

		 private JFormattedTextField extruderDeprimeA = PositiveTextFieldInt(repNF, 10000,
           "The number of steps to retract the right extruder's filament when the pipeline of buffered moves empties " +
           "or a travel-only move is encountered.  Set to a value of 0 to disable this feature for this extruder.  " +
           "Do not use with Skeinforge's Reversal plugin nor Skeinforge's Dimension plugin's \"Retraction Distance\".");

		 private JFormattedTextField extruderDeprimeB = PositiveTextFieldInt(repNF, 10000,
           "The number of steps to retract the left extruder's filament when the pipeline of buffered moves empties " +
           "or a travel-only move is encountered.  Set to a value of 0 to disable this feature for this extruder.  " +
           "Do not use with Skeinforge's Reversal plugin nor Skeinforge's Dimension plugin's \"Retraction Distance\".");

		 private JCheckBox overrideGCodeTempBox = new JCheckBox();
		 {
			 overrideGCodeTempBox.setToolTipText(wrap2HTML(width,
                    "When enabled, override the gcode temperature settings using the preheat " +
		    "temperature settings for the extruders and build platform."));
		 }
           
		 private JFormattedTextField tool0Temp = PositiveTextFieldInt(tempNF, 255,
           "Temperature in degrees Celsius to preheat extruder 0 to.  This temperature is " +
           "also used as the override temperature when the \"override gcode temperature\" " +
           "feature is enabled.");

		 private JFormattedTextField tool1Temp = PositiveTextFieldInt(tempNF, 255,
           "Temperature in degrees Celsius to preheat extruder 1 to.  This temperature is " +
           "also used as the override temperature when the \"override gcode temperature\" " +
           "feature is enabled.");

		 private JFormattedTextField platformTemp = PositiveTextFieldInt(tempNF, 255,
           "Temperature in degrees Celsius to preheat the build platform to.  This temperature is " +
           "also used as the override temperature when the \"override gcode temperature\" " +
           "feature is enabled.");

		 // Slowdown is a flag for the Replicator
		 private JCheckBox slowdownFlagBox = new JCheckBox();
		 {
			 slowdownFlagBox.setToolTipText(wrap2HTML(width,
            "If you are printing an object with fine details or at very fast speeds, it is possible " +
            "that the planner will be unable to keep up with printing.  This may be evidenced by frequent " +
            "pauses accompanied by unwanted plastic blobs or zits.  You may be able to mitigate this by " +
            "enabling \"slowdown\".  When slowdown is enabled and the planner is having difficulty keeping " +
            "up, the printing feed rate is reduced so as to cause each segment to take more time to print.  " +
	    "The reduction in printing speed then gives the planner a chance to catch up."));
		 }

		 private final String[] lcdDimensionChoices = { "16 x 4", "20 x 4", "24 x 4" };
		 private JComboBox lcdDimensionsChoice = new JComboBox(lcdDimensionChoices);
		 {
			 lcdDimensionsChoice.setToolTipText(wrap2HTML(width,
		   "Select the dimensions of the LCD screen display in your Gen 4 LCD " +
		   "interface.  Measurements are the number of character columns by the " +
                   "number of character rows.  The standard Gen 4 LCD display is 16 x 4."));
		 }

		 private JFormattedTextField buzzerRepeats = PositiveTextFieldInt(tempNF, 255,
           "The number of times the buzzer should buzz when activated.  Use of this feature " +
           "requires installation of a buzzer as per Thingiverse Thing 16170, \"Buzzer Support\".");

		 private JColorChooser moodLightCustomColor = new JColorChooser();
		 {
			 moodLightCustomColor.setToolTipText(wrap2HTML(width,
		   "Select the custom color used when the \"Custom Color\" mood light script " +
		   "is selected.  Use of this feature requires installation of Thingiverse " +
		   "Thing 15347, \"Mood Lighting For ToM\"."));
		 }

		 private JComboBox moodLightScript = new JComboBox(moodLightChoices);
		 {
			 moodLightScript.setToolTipText(wrap2HTML(width,
                   "Select the mood light script to be played by the mood lighting.  Use of this " +
		   "feature requires installation of Thingiverse Thing 15347, \"Mood Lighting " +
                   "For ToM\"."));
		 }

		 private SailfishG3MachineOnboardAccelerationParameters(OnboardParameters target,
									      Driver driver,
									      JTabbedPane subtabs) {
			 super(target, driver, subtabs);

			 int dismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
			 if (dismissDelay < 10*1000)
				 ToolTipManager.sharedInstance().setDismissDelay(10*1000);

			 draftButton.addActionListener(new ActionListener() {
					 public void actionPerformed(ActionEvent arg0) {
						 SailfishG3MachineOnboardAccelerationParameters.this.setUIFields(draftParams);
					 }
			 });

			 qualityButton.addActionListener(new ActionListener() {
					 public void actionPerformed(ActionEvent arg0) {
						 SailfishG3MachineOnboardAccelerationParameters.this.setUIFields(qualityParams);
					 }
			 });
		 }

		 AccelParams getAccelParamsFromUI() {

			 int scriptId = findMoodLightScriptId(moodLightScript.getSelectedIndex());
			 if (scriptId < 0)
				 scriptId = moodLightScriptId_DEFAULT;

			 int lcdIndex = ((Number)lcdDimensionsChoice.getSelectedIndex()).intValue();
			 int lcdType = 0; // lcd == 0 --> lcdType = 0
			 if (lcdIndex == 1) lcdType = 50;
			 else if (lcdIndex == 2) lcdType = 51;

			 Color c = moodLightCustomColor.getColor();

			 return new AccelParams(new AccelParamsTab1(accelerationBox.isSelected(),
								    new long[] {((Number)normalMoveAcceleration.getValue()).longValue(),
									        ((Number)extruderMoveAcceleration.getValue()).longValue()},
								    new long[] {((Number)xAxisMaxAcceleration.getValue()).longValue(),
									        ((Number)yAxisMaxAcceleration.getValue()).longValue(),
									        ((Number)zAxisMaxAcceleration.getValue()).longValue(),
									        ((Number)aAxisMaxAcceleration.getValue()).longValue(),
									        ((Number)bAxisMaxAcceleration.getValue()).longValue()},
								    new double[] {((Number)xAxisMaxSpeedChange.getValue()).doubleValue(),
									          ((Number)yAxisMaxSpeedChange.getValue()).doubleValue(),
									          ((Number)zAxisMaxSpeedChange.getValue()).doubleValue(),
									          ((Number)aAxisMaxSpeedChange.getValue()).doubleValue(),
									          ((Number)bAxisMaxSpeedChange.getValue()).doubleValue()}),
						new AccelParamsTab2(slowdownFlagBox.isSelected(),
								    new long[] {((Number)extruderDeprimeA.getValue()).longValue(),
									        ((Number)extruderDeprimeB.getValue()).longValue()},
								    new double[] {((Number)JKNAdvance1.getValue()).doubleValue(),
										  ((Number)JKNAdvance2.getValue()).doubleValue()}),
						new AccelParamsTab3(overrideGCodeTempBox.isSelected(),
								    dittoBox.isSelected(),
                    extruderHoldBox.isSelected(),
								    ((Number)buzzerRepeats.getValue()).intValue(),
								    lcdType,
								    scriptId,
								    new int[] {c.getRed(), c.getGreen(), c.getBlue()},
								    new int[] {((Number)tool0Temp.getValue()).intValue(),
									       ((Number)tool1Temp.getValue()).intValue(),
									       ((Number)platformTemp.getValue()).intValue()}));
		 }

		 @Override
		 public boolean isAccelerationEnabled() {
			 return accelerationBox.isSelected();
		 }

		 private void setEEPROMFromUI(AccelParams params) {

			 target.setAccelerationStatus(params.tab1.accelerationEnabled ? (byte)1 : (byte)0);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.DITTO_PRINT_ENABLED, params.tab3.dittoEnabled ? 1 : 0);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.EXTRUDER_HOLD, params.tab3.extruderHold ? 1 : 0);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.OVERRIDE_GCODE_TEMP, params.tab3.overrideGCodeTempEnabled ? 1 : 0);

			 int lv = params.tab2.slowdownEnabled ? 1 : 0;
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_SLOWDOWN_FLAG, lv);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_NORM,    params.tab1.accelerations[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_RETRACT, params.tab1.accelerations[1]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_X, params.tab1.maxAccelerations[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Y, params.tab1.maxAccelerations[1]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Z, params.tab1.maxAccelerations[2]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_A, params.tab1.maxAccelerations[3]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_B, params.tab1.maxAccelerations[4]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_X, params.tab1.maxSpeedChanges[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Y, params.tab1.maxSpeedChanges[1]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Z, params.tab1.maxSpeedChanges[2]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_A, params.tab1.maxSpeedChanges[3]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_B, params.tab1.maxSpeedChanges[4]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K,  params.tab2.JKNadvance[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K2, params.tab2.JKNadvance[1]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_A, params.tab2.deprime[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_B, params.tab2.deprime[1]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.LCD_TYPE, params.tab3.lcdType);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.OVERRIDE_GCODE_TEMP, params.tab3.overrideGCodeTempEnabled ? 1 : 0);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.TOOL0_TEMP, params.tab3.overrideTemps[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.TOOL1_TEMP, params.tab3.overrideTemps[1]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.PLATFORM_TEMP, params.tab3.overrideTemps[2]);

			 target.setEEPROMParam(OnboardParameters.EEPROMParams.BUZZER_REPEATS, params.tab3.buzzerRepeats);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_SCRIPT, params.tab3.scriptId);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_RED,   params.tab3.rgb[0]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_GREEN, params.tab3.rgb[1]);
			 target.setEEPROMParam(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_BLUE,  params.tab3.rgb[2]);
		 }

		 @Override
		 public void setEEPROMFromUI() {
			 setEEPROMFromUI(getAccelParamsFromUI());
		 }

		 private void setUIFields(AccelParamsTab1 params) {
			 setUIFields(params, null, null);
		 }

		 private void setUIFields(AccelParamsTab1 tab1,
					  AccelParamsTab2 tab2,
					  AccelParamsTab3 tab3) {

			 if (tab1 != null) {
				 accelerationBox.setSelected(tab1.accelerationEnabled);

				 if (tab1.accelerations != null) {
					 normalMoveAcceleration.setValue(tab1.accelerations[0]);
					 extruderMoveAcceleration.setValue(tab1.accelerations[1]);
				 }

				 if (tab1.maxAccelerations != null) {
					 xAxisMaxAcceleration.setValue(tab1.maxAccelerations[0]);
					 yAxisMaxAcceleration.setValue(tab1.maxAccelerations[1]);
					 zAxisMaxAcceleration.setValue(tab1.maxAccelerations[2]);
					 aAxisMaxAcceleration.setValue(tab1.maxAccelerations[3]);
					 bAxisMaxAcceleration.setValue(tab1.maxAccelerations[4]);
				 }

				 if (tab1.maxSpeedChanges != null) {
					 xAxisMaxSpeedChange.setValue(tab1.maxSpeedChanges[0]);
					 yAxisMaxSpeedChange.setValue(tab1.maxSpeedChanges[1]);
					 zAxisMaxSpeedChange.setValue(tab1.maxSpeedChanges[2]);
					 aAxisMaxSpeedChange.setValue(tab1.maxSpeedChanges[3]);
					 bAxisMaxSpeedChange.setValue(tab1.maxSpeedChanges[4]);
				 }
			 }

			 if (tab2 != null) {
				 slowdownFlagBox.setSelected(tab2.slowdownEnabled);

				 if (tab2.JKNadvance != null) {
					 JKNAdvance1.setValue(tab2.JKNadvance[0]);
					 JKNAdvance2.setValue(tab2.JKNadvance[1]);
				 }

				 if (tab2.deprime != null) {
					 extruderDeprimeA.setValue(tab2.deprime[0]);
					 extruderDeprimeB.setValue(tab2.deprime[1]);
				 }
			 }

			 if (tab3 != null) {
				 overrideGCodeTempBox.setSelected(tab3.overrideGCodeTempEnabled);
				 dittoBox.setSelected(tab3.dittoEnabled);
         extruderHoldBox.setSelected(tab3.extruderHold);
				 buzzerRepeats.setValue(tab3.buzzerRepeats);
				 int lcdIndex;
				 if (tab3.lcdType == 50)
					 lcdIndex = 1;
				 else if (tab3.lcdType == 51)
					 lcdIndex = 2;
				 else
					 lcdIndex = 0;
				 lcdDimensionsChoice.setSelectedIndex(lcdIndex);
				 int moodLightChoice = findMoodLightChoice(tab3.scriptId);
				 if (moodLightChoice < 0)
					 moodLightChoice = moodLightChoiceIndex_DEFAULT;
				 moodLightScript.setSelectedIndex(moodLightChoice);
				 if (tab3.rgb != null)
					 moodLightCustomColor.setColor(tab3.rgb[0], tab3.rgb[1], tab3.rgb[2]);
				 if (tab3.overrideTemps != null) {
					 tool0Temp.setValue(tab3.overrideTemps[0]);
					 tool1Temp.setValue(tab3.overrideTemps[1]);
					 platformTemp.setValue(tab3.overrideTemps[2]);
				 }
			 }

			 // Enable/disable the draft & quality buttons based upon the UI field values
			 // propertyChange(null);
		 }

		 @Override
		 public void setUIFromEEPROM() {

			 boolean accelerationEnabled = target.getAccelerationStatus() != 0;
			 boolean slowdownEnabled = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.ACCEL_SLOWDOWN_FLAG) != 0;
			 boolean overrideGCodeTempEnabled = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.OVERRIDE_GCODE_TEMP) != 0;
			 boolean dittoEnabled = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.DITTO_PRINT_ENABLED) != 0;
       boolean extruderHold = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.EXTRUDER_HOLD) != 0;
			 int buzzerRepeats = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.BUZZER_REPEATS);
			 int scriptId = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_SCRIPT);
			 int lcdType = target.getEEPROMParamInt(OnboardParameters.EEPROMParams.LCD_TYPE);

			 long[] maxAccelerations = new long[] {
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_X),
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Y),
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_Z),
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_A),
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_ACCELERATION_B) };

			 double[] maxSpeedChanges = new double[] {
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_X),
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Y),
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_Z),
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_A),
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_MAX_SPEED_CHANGE_B) };

			 long[] accelerations = new long[] {
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_NORM),
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_MAX_EXTRUDER_RETRACT) };

			 double[] JKNadvance = new double[] {
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K),
				 target.getEEPROMParamFloat(OnboardParameters.EEPROMParams.ACCEL_ADVANCE_K2) };

			 long[] deprime = new long[] {
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_A),
				 target.getEEPROMParamUInt(OnboardParameters.EEPROMParams.ACCEL_EXTRUDER_DEPRIME_B) };
		 
			 int[] rgb = new int[] {
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_RED),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_GREEN),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.MOOD_LIGHT_CUSTOM_BLUE) };

			 int[] overrideTemps = new int[] {
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.TOOL0_TEMP),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.TOOL1_TEMP),
				 target.getEEPROMParamInt(OnboardParameters.EEPROMParams.PLATFORM_TEMP) };

			 setUIFields(new AccelParamsTab1(accelerationEnabled,
							 accelerations,
							 maxAccelerations,
							 maxSpeedChanges),
				     new AccelParamsTab2(slowdownEnabled,
							 deprime,
							 JKNadvance),
				     new AccelParamsTab3(overrideGCodeTempEnabled,
							 dittoEnabled,
               extruderHold,
							 buzzerRepeats,
							 lcdType,
							 scriptId,
							 rgb,
							 overrideTemps));
		 }

		 @Override
		 public void buildUI() {
			 JPanel accelerationTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			 subTabs.addTab("Acceleration", accelerationTab);

			 JPanel accelerationMiscTab = new JPanel(new MigLayout("fill", "[r][l]"));
			 subTabs.addTab("Acceleration (Misc)", accelerationMiscTab);

			 JPanel miscTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			 subTabs.addTab("Misc", miscTab);

			 normalMoveAcceleration.setColumns(8);
			 extruderMoveAcceleration.setColumns(8);

			 xAxisMaxAcceleration.setColumns(8);
			 xAxisMaxSpeedChange.setColumns(4);

			 yAxisMaxAcceleration.setColumns(8);
			 yAxisMaxSpeedChange.setColumns(4);

			 zAxisMaxAcceleration.setColumns(8);
			 zAxisMaxSpeedChange.setColumns(4);

			 aAxisMaxAcceleration.setColumns(8);
			 aAxisMaxSpeedChange.setColumns(4);

			 bAxisMaxAcceleration.setColumns(8);
			 bAxisMaxSpeedChange.setColumns(4);

			 addWithSharedToolTips(accelerationTab, "Acceleration enabled", accelerationBox, "wrap");

			 addWithSharedToolTips(accelerationTab,
					       "Max acceleration (magnitude of acceleration vector; mm/s\u00B2)", "span 2, gapleft push",
					       normalMoveAcceleration, "wrap, gapright push ");

			 addWithSharedToolTips(accelerationTab,
					       "Max acceleration for extruder-only moves (mm/s\u00B2)", "span 2, gapleft push", //"split 2, span 3",
					       extruderMoveAcceleration, "wrap, gapright push");

			 addWithSharedToolTips(accelerationTab, "X max acceleration (mm/s\u00B2)",
					       xAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "X max speed change (mm/s)",
					       xAxisMaxSpeedChange, "wrap");
 
			 addWithSharedToolTips(accelerationTab, "Y max acceleration (mm/s\u00B2)",
					       yAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Y max speed change (mm/s)",
					       yAxisMaxSpeedChange, "wrap");

			 addWithSharedToolTips(accelerationTab, "Z max acceleration (mm/s\u00B2)",
					       zAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Z max speed change (mm/s)",
					       zAxisMaxSpeedChange, "wrap");

			 addWithSharedToolTips(accelerationTab, "Right extruder max acceleration (mm/s\u00B2)",
					       aAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Right extruder max speed change (mm/s)",
					       aAxisMaxSpeedChange, "wrap");
 
			 addWithSharedToolTips(accelerationTab, "Left extruder acceleration (mm/s\u00B2)",
					       bAxisMaxAcceleration);
			 addWithSharedToolTips(accelerationTab, "Left extruder max speed change (mm/s)",
					       bAxisMaxSpeedChange, "wrap");

			 accelerationTab.add(qualityButton, "span 2, gapleft push");
			 accelerationTab.add(draftButton, "span 2, gapright push");

			 // Acceleration - Misc

			 JKNAdvance1.setColumns(8);
			 JKNAdvance2.setColumns(8);

			 extruderDeprimeA.setColumns(8);
			 extruderDeprimeB.setColumns(8);

			 addWithSharedToolTips(accelerationMiscTab, "Slow printing when acceleration planing falls behind", slowdownFlagBox, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "JKN Advance K", JKNAdvance1, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "JKN Advance K2", JKNAdvance2, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "Right extruder deprime (steps)", extruderDeprimeA, "wrap");
			 addWithSharedToolTips(accelerationMiscTab, "Left extruder deprime (steps)", extruderDeprimeB, "wrap");

			 // Misc tab
			 tool0Temp.setColumns(8);
			 tool1Temp.setColumns(8);
			 platformTemp.setColumns(8);
			 buzzerRepeats.setColumns(8);

			 addWithSharedToolTips(miscTab, "Override gcode temperatures", overrideGCodeTempBox);
			 addWithSharedToolTips(miscTab, "Gen 4 LCD dimensions", lcdDimensionsChoice, "wrap");

			 addWithSharedToolTips(miscTab, "Right/sole extruder (tool 0) preheat & override temperature (C)", tool0Temp);
			 addWithSharedToolTips(miscTab, "Buzzer repeats", buzzerRepeats, "wrap");

			 addWithSharedToolTips(miscTab, "Left extruder (tool 1) preheat & override temperature (C)", tool1Temp);
			 addWithSharedToolTips(miscTab, "Mood light script", moodLightScript, "wrap");

			 addWithSharedToolTips(miscTab, "Platform preheat & override temperature (C)",
					       platformTemp);
			 addWithSharedToolTips(miscTab, "Ditto (duplicate) printing enabled", dittoBox, "wrap");
       addWithSharedToolTips(miscTab, "Extruder hold enabled", extruderHoldBox, "wrap");

			 addWithSharedToolTips(miscTab, "Mood light color",
					       moodLightCustomColor, "span 4, wrap, gapbottom push, gapright push");
		 }
		 // Given a Mood Light Script Id, find the corresponding choice in the
		 // Mood Light choice list
		 public int findMoodLightChoice(int scriptId) {
			 String str = "(" + scriptId + ")";
			 for (int i = 0; i < moodLightChoices.length; i++) {
				 if (moodLightChoices[i].contains(str))
					 return i;
			 }
			 return -1;
		 }

		 // Given an index in the list of Mood Light choices, find the corresponding Script Id
		 public int findMoodLightScriptId(int choiceIndex) {
			 if (choiceIndex < 0 || choiceIndex > moodLightChoices.length)
				 return -1;
			 int start = moodLightChoices[choiceIndex].indexOf('(');
			 if (start < 0)
				 return -1;
			 ++start;
			 int end = moodLightChoices[choiceIndex].indexOf(')', start);
			 if (end < 0)
				 return -1;
			 int scriptId = -1;
			 try {
				 scriptId = Integer.parseInt(moodLightChoices[choiceIndex].substring(start, end));
			 }
			 catch (Exception e) {
			 }
			 return scriptId;
		 }
	 }
 }
