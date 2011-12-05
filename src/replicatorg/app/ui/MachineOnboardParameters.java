/**
 * 
 */
package replicatorg.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.EnumSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
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
	private JTextField machineNameField = new JTextField();
	private JCheckBox xAxisInvertBox = new JCheckBox();
	private JCheckBox yAxisInvertBox = new JCheckBox();
	private JCheckBox zAxisInvertBox = new JCheckBox();
	private JCheckBox aAxisInvertBox = new JCheckBox();
	private JCheckBox bAxisInvertBox = new JCheckBox();
	private JCheckBox zHoldBox = new JCheckBox();
	private JButton resetToFactoryButton = new JButton("Reset motherboard to factory settings");
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
		EnumSet<AxisId> axesInverted = EnumSet.noneOf(AxisId.class);
		if (xAxisInvertBox.isSelected()) axesInverted.add(AxisId.X);
		if (yAxisInvertBox.isSelected()) axesInverted.add(AxisId.Y);
		if (zAxisInvertBox.isSelected()) axesInverted.add(AxisId.Z);
		if (aAxisInvertBox.isSelected()) axesInverted.add(AxisId.A);
		if (bAxisInvertBox.isSelected()) axesInverted.add(AxisId.B);
		if (!zHoldBox.isSelected())      axesInverted.add(AxisId.V); // V is in the 7th bit position, and it's set to NOT hold Z
		target.setInvertedParameters(axesInverted);
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
		resetDialog();
	}

	private void resetToFactory() {
		target.resetToFactory();
		resetDialog();
		loadParameters();
	}
	

	private void loadParameters() {
		machineNameField.setText(this.target.getMachineName());
		EnumSet<AxisId> invertedAxes = this.target.getInvertedParameters();
		
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
	}

	private JPanel makeButtonPanel() {
		JPanel panel = new JPanel(new MigLayout());
		JButton commitButton = new JButton("Commit Changes");
		panel.add(commitButton);
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MachineOnboardParameters.this.commit();
			}
		});
		return panel;
	}
	
	public MachineOnboardParameters(OnboardParameters target, Driver driver) {
		this.target = target;
		this.driver = driver;

		setLayout(new MigLayout("fill"));

		add(new JLabel("Machine Name (max. "+Integer.toString(MAX_NAME_LENGTH)+" chars)"));
		machineNameField.setColumns(MAX_NAME_LENGTH);
		add(machineNameField,"wrap");
		add(new JLabel("Invert X axis"));
		add(xAxisInvertBox,"wrap");
		add(new JLabel("Invert Y axis"));
		add(yAxisInvertBox,"wrap");
		add(new JLabel("Invert Z axis"));
		add(zAxisInvertBox,"wrap");
		add(new JLabel("Invert A axis"));
		add(aAxisInvertBox,"wrap");
		add(new JLabel("Invert B axis"));
		add(bAxisInvertBox,"wrap");
		add(new JLabel("Hold Z axis"));
		add(zHoldBox,"wrap");
		add(new JLabel("Invert endstops"));
		add(endstopInversionSelection,"wrap");
		add(new JLabel("Emergency stop"));
		add(estopSelection,"wrap");
		
		xAxisHomeOffsetField.setColumns(10);
		yAxisHomeOffsetField.setColumns(10);
		zAxisHomeOffsetField.setColumns(10);
		aAxisHomeOffsetField.setColumns(10);
		bAxisHomeOffsetField.setColumns(10);
		
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
				
		add(makeButtonPanel());
		
		resetToFactoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MachineOnboardParameters.this.resetToFactory();
				// Reload
				// This gets called in resetToFactory()
//				loadParameters();
			}
		});
		add(resetToFactoryButton);
		
		loadParameters();
	}
}
