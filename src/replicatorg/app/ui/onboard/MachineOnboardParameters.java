/**
 * 
 */
package replicatorg.app.ui.onboard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.logging.Level;

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
import replicatorg.drivers.RetryException;
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
	private JComboBox toolCountComboBox = new JComboBox();
	private JCheckBox xAxisInvertBox = new JCheckBox();
	private JCheckBox yAxisInvertBox = new JCheckBox();
	private JCheckBox zAxisInvertBox = new JCheckBox();
	private JCheckBox aAxisInvertBox = new JCheckBox();
	private JCheckBox bAxisInvertBox = new JCheckBox();
	private JCheckBox zHoldBox = new JCheckBox();
	private JButton resetToFactoryButton = new JButton("Reset motherboard to factory settings");
	private JButton resetToBlankButton = new JButton("Reset motherboard completely");
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

	private void resetDialog() {
		int confirm = JOptionPane.showConfirmDialog(this, 
				"<html>Before these changes can take effect, you'll need to reset your <br/>"+
				"motherboard.  If you choose not to reset the board now, some old settings <br/>"+
				"will remain in effect until you manually reset.<br/><br/>Reset the " +
				"motherboard now?</html>",
				"Reset board?", 
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (confirm == JOptionPane.YES_OPTION) {
			driver.reset();
		}
	}
	
	private void commit() {
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
		if (!zHoldBox.isSelected())      
			axesInverted.add(AxisId.V);
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

			add(vref0, "growx, split");
			add(vref1, "growx, split");
			add(vref2, "growx, split");
			add(vref3, "growx, split");
			add(vref4, "growx, wrap");
		}
		resetDialog();
	}

	private void resetToBlank()
	{
		try { 
			target.resetToBlank();
			resetDialog();
			loadParameters();		
		}
		catch (replicatorg.drivers.RetryException e){
			Base.logger.severe("reset to blank failed due to error" + e.toString());
			Base.logger.severe("Please restart your machine for safety");
		}		
	}
	
	private void resetToFactory() {
		try { 
			target.resetToFactory();
			resetDialog();
			loadParameters();
		}
		catch (replicatorg.drivers.RetryException e){
			Base.logger.severe("reset to blank failed due to error" + e.toString());
			Base.logger.severe("Please restart your machine for safety");
		}
	}
	

	private void loadParameters() {
		machineNameField.setText(this.target.getMachineName());
		
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
	}

	private JPanel makeButtonPanel() {
		JPanel panel = new JPanel(new MigLayout());
		JButton commitButton = new JButton("Commit Changes");
		panel.add(commitButton);
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MachineOnboardParameters.this.commit();
				MachineOnboardParameters.this.dispose();
			}
		});
		return panel;
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
		add(machineNameField,"span 3, wrap");

  		if( target.hasToolCountOnboard() ) {
  			add(new JLabel("Reported Tool Count:"));
  			add(toolCountField, "span 3, wrap");
  		}
		
		
		add(new JLabel("Invert X axis"));		
		add(xAxisInvertBox,"span 3, wrap");
		
		add(new JLabel("Invert Y axis"));
		add(yAxisInvertBox,"span 3, wrap");
		
		add(new JLabel("Invert Z axis"));
		add(zAxisInvertBox,"span 3, wrap");

		String aName = "Invert A axis";
		if( axesAltNamesMap.containsKey(AxisId.A) )
			aName = aName + " (" + axesAltNamesMap.get(AxisId.A) + ") ";
		add(new JLabel(aName));
		add(aAxisInvertBox,"span 3, wrap");
		
		String bName = "Invert B axis";
		if( axesAltNamesMap.containsKey(AxisId.B) )
			bName = bName + " (" + axesAltNamesMap.get(AxisId.B) + ") ";
		add(new JLabel(bName));
		
		add(bAxisInvertBox,"span 3, wrap");
		add(new JLabel("Hold Z axis"));
		
		add(zHoldBox,"span 3, wrap");
		add(new JLabel("Invert endstops"));
		
		add(endstopInversionSelection,"span 3, wrap");
		add(new JLabel("Emergency stop"));
		add(estopSelection,"span 3, wrap");
		
		xAxisHomeOffsetField.setColumns(10);
		yAxisHomeOffsetField.setColumns(10);
		zAxisHomeOffsetField.setColumns(10);
		aAxisHomeOffsetField.setColumns(10);
		bAxisHomeOffsetField.setColumns(10);
		
		if(target.hasVrefSupport())
		{
			add(new JLabel("X home offset (mm)"));
			add(xAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 0"));
			add(vref0, "growx, wrap");
			add(new JLabel("Y home offset (mm)"));
			add(yAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 1"));
			add(vref1, "growx, wrap");
			add(new JLabel("Z home offset (mm)"));
			add(zAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 2"));
			add(vref2, "growx, wrap");
			add(new JLabel("A home offset (mm)"));
			add(aAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 3"));
			add(vref3, "growx, wrap");
			add(new JLabel("B home offset (mm)"));
			add(bAxisHomeOffsetField);
			add(new JLabel("VREF Pot. 4"));
			add(vref4, "growx, wrap");
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

		add(makeButtonPanel());
		
		resetToFactoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MachineOnboardParameters.this.resetToFactory();
				// This gets called in resetToFactory()
//				loadParameters();
			}
		});
		resetToFactoryButton.setToolTipText("Reest the onboard settings to the factory defaults");
		add(resetToFactoryButton);

		
		resetToBlankButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MachineOnboardParameters.this.resetToBlank();
				// This gets called in resetToFactory()
//				loadParameters();
			}
		});
		resetToBlankButton.setToolTipText("Reest the onboard settings to the *completely blank*");
		add(resetToBlankButton);


		
		loadParameters();
	}
}
