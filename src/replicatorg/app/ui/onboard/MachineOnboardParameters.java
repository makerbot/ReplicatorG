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
        
        private JFormattedTextField xNozzleOffsetField = new JFormattedTextField(threePlaces);
        private JFormattedTextField yNozzleOffsetField = new JFormattedTextField(threePlaces);
        private JFormattedTextField zNozzleOffsetField = new JFormattedTextField(threePlaces);

	
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
                
        target.eepromStoreToolDelta(0, ((Number)xNozzleOffsetField.getValue()).doubleValue());
        target.eepromStoreToolDelta(1, ((Number)yNozzleOffsetField.getValue()).doubleValue());
        target.eepromStoreToolDelta(2, ((Number)zNozzleOffsetField.getValue()).doubleValue());

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
                
                xNozzleOffsetField.setValue(this.target.getNozzleOffset(0));
                yNozzleOffsetField.setValue(this.target.getNozzleOffset(1));
                zNozzleOffsetField.setValue(this.target.getNozzleOffset(2));
                
	}

	protected void dispose() {
		parent.dispose();
	}

	public MachineOnboardParameters(OnboardParameters target, Driver driver, JFrame parent) {
		this.target = target;
		this.driver = driver;
		this.parent = parent;
		
		setLayout(new MigLayout("fill"));
		EnumMap<AxisId, String> axesAltNamesMap = target.getAxisAlises();

		add(new JLabel("Machine Name (max. "+Integer.toString(MAX_NAME_LENGTH)+" chars)"));
		machineNameField.setColumns(MAX_NAME_LENGTH);
		add(machineNameField,"span 2, wrap");

  		if( target.hasToolCountOnboard() ) {
  			add(new JLabel("Reported Tool Count:"));
  			add(toolCountField, "span 2, wrap");
  		}
		
		
		add(new JLabel("Invert X axis"));		
		add(xAxisInvertBox,"span 2, wrap");
		
		add(new JLabel("Invert Y axis"));
		add(yAxisInvertBox,"span 2, wrap");
		
		add(new JLabel("Invert Z axis"));
		add(zAxisInvertBox,"span 2, wrap");

		String aName = "Invert A axis";
		if( axesAltNamesMap.containsKey(AxisId.A) )
			aName = aName + " (" + axesAltNamesMap.get(AxisId.A) + ") ";
		add(new JLabel(aName));
		add(aAxisInvertBox,"span 2, wrap");
		
		String bName = "Invert B axis";
		if( axesAltNamesMap.containsKey(AxisId.B) )
			bName = bName + " (" + axesAltNamesMap.get(AxisId.B) + ") ";
		add(new JLabel(bName));
		
		add(bAxisInvertBox,"span 2, wrap");
		add(new JLabel("Hold Z axis"));
		
		add(zHoldBox,"span 2, wrap");
		add(new JLabel("Invert endstops"));
		
		add(endstopInversionSelection,"span 2, wrap");
		add(new JLabel("Emergency stop"));
		add(estopSelection,"spanx, wrap");
		
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
			add(new JLabel("X home offset (mm)"));
			add(xAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 0"), "split");
			add(vref0, "wrap");
			add(new JLabel("Y home offset (mm)"));
			add(yAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 1"), "split");
			add(vref1, "wrap");
			add(new JLabel("Z home offset (mm)"));
			add(zAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 2"), "split");
			add(vref2, "wrap");
			add(new JLabel("A home offset (mm)"));
			add(aAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 3"), "split");
			add(vref3, "wrap");
			add(new JLabel("B home offset (mm)"));
			add(bAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 4"), "split");
			add(vref4, "wrap");
		}
		else
		{
			add(new JLabel("X home offset (mm)"));
			add(xAxisHomeOffsetField,"wrap");
			add(new JLabel("Y home offset (mm)"));
			add(yAxisHomeOffsetField,"wrap");
			add(new JLabel("Z home offset (mm)"));
			add(zAxisHomeOffsetField,"wrap");
			add(new JLabel("A home offset (mm)"));
			add(aAxisHomeOffsetField,"wrap");
			add(new JLabel("B home offset (mm)"));
			add(bAxisHomeOffsetField,"wrap");
		}
                
                xNozzleOffsetField.setColumns(10);
                yNozzleOffsetField.setColumns(10);
                zNozzleOffsetField.setColumns(10);
                
                add(new JLabel("X nozzle offset (mm)"));
                add(xNozzleOffsetField, "wrap");
                
                add(new JLabel("Y nozzle offset (mm)"));
                add(yNozzleOffsetField, "wrap");
                
                add(new JLabel("Z nozzle offset (mm)"));
                add(zNozzleOffsetField, "wrap");

		
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

}
