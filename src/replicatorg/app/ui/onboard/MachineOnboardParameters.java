/**
 * 
 */
package replicatorg.app.ui.onboard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.EnumSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.OnboardParameters;
import replicatorg.machine.model.AxisId;
import replicatorg.util.Point5d;

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
	
	private JTextField machineNameField = new JTextField();
	private static final String[] toolCountChoices = {"unavailable","1", "2"};
	private JComboBox toolCountField = new JComboBox(toolCountChoices);
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
		"Inverted (Default)",
		"Non-inverted"
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
	
	private JFormattedTextField masterAcceleration = new JFormattedTextField(threePlaces);
	
	private JFormattedTextField minimumPlannerSpeed = new JFormattedTextField(threePlaces);

	private JFormattedTextField xAxisAcceleration = new JFormattedTextField(threePlaces);
	private JFormattedTextField yAxisAcceleration = new JFormattedTextField(threePlaces);
	private JFormattedTextField zAxisAcceleration = new JFormattedTextField(threePlaces);
	private JFormattedTextField aAxisAcceleration = new JFormattedTextField(threePlaces);
	private JFormattedTextField bAxisAcceleration = new JFormattedTextField(threePlaces);

	private JFormattedTextField xyJunctionJerk = new JFormattedTextField(threePlaces);
	private JFormattedTextField  zJunctionJerk = new JFormattedTextField(threePlaces);
	private JFormattedTextField  aJunctionJerk = new JFormattedTextField(threePlaces);
	private JFormattedTextField  bJunctionJerk = new JFormattedTextField(threePlaces);

	
	/** Prompts the user to fire a bot  reset after the changes have been sent to the board.
	 */
	private void requestResetFromUser() {
		int confirm = JOptionPane.showConfirmDialog(this, 
				"<html>For these changes to take effect your motherboard needs to reset. <br/>"+
				"This may take up to <b>10 seconds</b>.</html>",
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
                
        target.eepromStoreToolDelta(0, ((Number)xToolheadOffsetField.getValue()).doubleValue());
        target.eepromStoreToolDelta(1, ((Number)yToolheadOffsetField.getValue()).doubleValue());
        target.eepromStoreToolDelta(2, ((Number)zToolheadOffsetField.getValue()).doubleValue());

        requestResetFromUser();
		
		if(target.hasAccelerationSupport())
		{
			target.setMasterAccelerationRate(((Number)masterAcceleration.getValue()).intValue());
			target.setMinimumPlannerSpeed(((Number)minimumPlannerSpeed.getValue()).doubleValue());
			
			Point5d accelerationRates = new Point5d();
			accelerationRates.set(0, ((Number)xAxisAcceleration.getValue()).doubleValue());
			accelerationRates.set(1, ((Number)yAxisAcceleration.getValue()).doubleValue());
			accelerationRates.set(2, ((Number)zAxisAcceleration.getValue()).doubleValue());
			accelerationRates.set(3, ((Number)aAxisAcceleration.getValue()).doubleValue());
			accelerationRates.set(4, ((Number)bAxisAcceleration.getValue()).doubleValue());
			target.setAxisAccelerationRates(accelerationRates);

			Point5d jerkRates = new Point5d();
			jerkRates.set(0, ((Number)xyJunctionJerk.getValue()).doubleValue());
			jerkRates.set(2, ((Number) zJunctionJerk.getValue()).doubleValue());
			jerkRates.set(3, ((Number) aJunctionJerk.getValue()).doubleValue());
			jerkRates.set(4, ((Number) bJunctionJerk.getValue()).doubleValue());
			target.setAxisJunctionJerks(jerkRates);
		}
		requestResetFromUser();
	}

	/// Causes the EEPROM to be reset to a totally blank state, and during dispose
	/// tells caller to reset/reconnect the eeprom.
	private void resetToBlank()
	{
		try { 
			target.resetSettingsToBlank();
			requestResetFromUser();
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
			requestResetFromUser();
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
		
		
		if(target.hasAccelerationSupport())
		{
			masterAcceleration.setValue(target.getMasterAccelerationRate());

			minimumPlannerSpeed.setValue(target.getMinimumPlannerSpeed());
			
			Point5d accelerationRates = target.getAxisAccelerationRates();
			xAxisAcceleration.setValue(accelerationRates.x());
			yAxisAcceleration.setValue(accelerationRates.y());
			zAxisAcceleration.setValue(accelerationRates.z());
			aAxisAcceleration.setValue(accelerationRates.a());
			bAxisAcceleration.setValue(accelerationRates.b());

			Point5d jerkRates = target.getAxisJunctionJerks();
			xyJunctionJerk.setValue(jerkRates.get(0));
			 zJunctionJerk.setValue(jerkRates.get(2));
			 aJunctionJerk.setValue(jerkRates.get(3));
			 bJunctionJerk.setValue(jerkRates.get(4));
		}
	}

	protected void dispose() {
		parent.dispose();
	}

	public MachineOnboardParameters(OnboardParameters target, Driver driver, JFrame parent) {
		this.target = target;
		this.driver = driver;
		this.parent = parent;
		
		setLayout(new MigLayout("fill", "[r][l][r]"));

		add(new JLabel("Machine Name (max. "+Integer.toString(MAX_NAME_LENGTH)+" chars)"));
		machineNameField.setColumns(MAX_NAME_LENGTH);
		add(machineNameField,"spanx, wrap");

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
  			endstopsTab.add(toolCountField, "spanx, wrap");
  		}
		
		
		endstopsTab.add(new JLabel("Invert X axis"));		
		endstopsTab.add(xAxisInvertBox,"spanx, wrap");
		
		endstopsTab.add(new JLabel("Invert Y axis"));
		endstopsTab.add(yAxisInvertBox,"spanx, wrap");
		
		endstopsTab.add(new JLabel("Invert Z axis"));
		endstopsTab.add(zAxisInvertBox,"spanx, wrap");

		String aName = "Invert A axis";
		if( axesAltNamesMap.containsKey(AxisId.A) )
			aName = aName + " (" + axesAltNamesMap.get(AxisId.A) + ") ";
		endstopsTab.add(new JLabel(aName));
		endstopsTab.add(aAxisInvertBox,"spanx, wrap");
		
		String bName = "Invert B axis";
		if( axesAltNamesMap.containsKey(AxisId.B) )
			bName = bName + " (" + axesAltNamesMap.get(AxisId.B) + ") ";
		endstopsTab.add(new JLabel(bName));
		endstopsTab.add(bAxisInvertBox,"spanx, wrap");

		endstopsTab.add(new JLabel("Hold Z axis"));
		endstopsTab.add(zHoldBox,"spanx, wrap");

		endstopsTab.add(new JLabel("Invert endstops"));
		endstopsTab.add(endstopInversionSelection,"spanx, wrap");
		endstopsTab.add(new JLabel("<html><b>Inverted</b> is for mechanical switch or H21LOB-based enstops.</html>"), "skip, wrap");
		endstopsTab.add(new JLabel("<html><b>Non-inverted</b> is for H21LOI-based endstops.</html>"), "skip, wrap");

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
			homeVrefsTab.add(new JLabel("VREF Pot. 0"));
			homeVrefsTab.add(vref0, "wrap");
			homeVrefsTab.add(new JLabel("Y home offset (mm)"));
			homeVrefsTab.add(yAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 1"));
			homeVrefsTab.add(vref1, "wrap");
			homeVrefsTab.add(new JLabel("Z home offset (mm)"));
			homeVrefsTab.add(zAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 2"));
			homeVrefsTab.add(vref2, "wrap");
			homeVrefsTab.add(new JLabel("A home offset (mm)"));
			homeVrefsTab.add(aAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 3"));
			homeVrefsTab.add(vref3, "wrap");
			homeVrefsTab.add(new JLabel("B home offset (mm)"));
			homeVrefsTab.add(bAxisHomeOffsetField);
			homeVrefsTab.add(new JLabel("VREF Pot. 4"));
			homeVrefsTab.add(vref4, "wrap");
		}
		else
		{
			homeVrefsTab.add(new JLabel("X home offset (mm)"));
			homeVrefsTab.add(xAxisHomeOffsetField,"spanx, wrap");
			homeVrefsTab.add(new JLabel("Y home offset (mm)"));
			homeVrefsTab.add(yAxisHomeOffsetField,"spanx, wrap");
			homeVrefsTab.add(new JLabel("Z home offset (mm)"));
			homeVrefsTab.add(zAxisHomeOffsetField,"spanx, wrap");
			homeVrefsTab.add(new JLabel("A home offset (mm)"));
			homeVrefsTab.add(aAxisHomeOffsetField,"spanx, wrap");
			homeVrefsTab.add(new JLabel("B home offset (mm)"));
			homeVrefsTab.add(bAxisHomeOffsetField,"spanx, wrap");
		}

		if(target.hasToolheadsOffset()) {
		    xToolheadOffsetField.setColumns(10);
		    yToolheadOffsetField.setColumns(10);
		    zToolheadOffsetField.setColumns(10);
		    
		    homeVrefsTab.add(new JLabel("X toolhead offset (mm)"));
		    homeVrefsTab.add(xToolheadOffsetField, "spanx, wrap");
		    
		    homeVrefsTab.add(new JLabel("Y toolhead offset (mm)"));
		    homeVrefsTab.add(yToolheadOffsetField, "spanx, wrap");
		    
		    homeVrefsTab.add(new JLabel("Z toolhead offset (mm)"));
		    homeVrefsTab.add(zToolheadOffsetField, "spanx, wrap");
		}

		if(target.hasAccelerationSupport())
		{
			JPanel accelerationTab = new JPanel(new MigLayout("fill", "[r][l][r][l]"));
			subTabs.addTab("Acceleration", accelerationTab);

			masterAcceleration.setColumns(4);
			minimumPlannerSpeed.setColumns(4);
			
			xAxisAcceleration.setColumns(8);
			xyJunctionJerk.setColumns(4);
			
			yAxisAcceleration.setColumns(8);
			
			zAxisAcceleration.setColumns(8);
			zJunctionJerk.setColumns(4);
			
			aAxisAcceleration.setColumns(8);
			aJunctionJerk.setColumns(4);
			
			bAxisAcceleration.setColumns(8);
			bJunctionJerk.setColumns(4);

			accelerationTab.add(new JLabel("Master acceleration rate (mm/s/s)"));
			accelerationTab.add(masterAcceleration, "spanx, wrap");

			accelerationTab.add(new JLabel("Deceleration minimum speed (mm/s)"));
			accelerationTab.add(minimumPlannerSpeed, "spanx, wrap");
			
			accelerationTab.add(new JLabel("X acceleration rate (mm/s/s)"));
			accelerationTab.add(xAxisAcceleration);
			accelerationTab.add(new JLabel("X/Y max junction jerk (mm/s)"));
			accelerationTab.add(xyJunctionJerk, "wrap");

			accelerationTab.add(new JLabel("Y acceleration rate (mm/s/s)"));
			accelerationTab.add(yAxisAcceleration, "spanx, wrap");

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
		}

		
		resetToFactoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int n = JOptionPane.showConfirmDialog(MachineOnboardParameters.this, "Are you sure you wish to reset the motherboard settings \nto the factory defaults?", "Reset EEPROM to Blank?", JOptionPane.YES_NO_OPTION);
				if (n == JOptionPane.YES_OPTION) {
					MachineOnboardParameters.this.resetToFactory();
				}
				// This gets called in resetToFactory()
//				loadParameters();
			}
		});
		resetToFactoryButton.setToolTipText("Reset the onboard settings to the factory defaults");
		add(resetToFactoryButton, "al left");

		
		resetToBlankButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int n = JOptionPane.showConfirmDialog(MachineOnboardParameters.this, "Are you sure you wish to reset the motherboard settings \nto *completely blank*?", "Reset EEPROM to Blank?", JOptionPane.YES_NO_OPTION);
				if (n == JOptionPane.YES_OPTION) {
					MachineOnboardParameters.this.resetToBlank();
				}
				// This gets called in resetToFactory()
//				loadParameters();
			}
		});
		resetToBlankButton.setToolTipText("Reset the onboard settings to *completely blank*");
		add(resetToBlankButton, "al center");

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

}
