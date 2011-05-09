/**
 * 
 */
package replicatorg.app.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.drivers.Driver;
import replicatorg.drivers.OnboardParameters;
import replicatorg.machine.model.AxisId;

/**
 * A panel for editing the options stored onboard a machine.
 * @author phooky
 *
 */
public class MachineOnboardParameters extends JFrame {
	private static final long serialVersionUID = 7876192459063774731L;
	private final OnboardParameters target;
	private final Driver driver;
	private JTextField machineNameField = new JTextField();
	private JCheckBox xAxisInvertBox = new JCheckBox();
	private JCheckBox yAxisInvertBox = new JCheckBox();
	private JCheckBox zAxisInvertBox = new JCheckBox();
	private JCheckBox aAxisInvertBox = new JCheckBox();
	private JCheckBox bAxisInvertBox = new JCheckBox();
	private JButton resetToFactoryButton = new JButton("Reset motherboard to factory settings");
	private static final String[]  endstopInversionChoices = {
		"No endstops installed",
		"Inverted (Default; H21LOB-based enstops)",
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
	private JTextField xAxisHomeOffsetField = new JTextField();
	private JTextField yAxisHomeOffsetField = new JTextField();
	private JTextField zAxisHomeOffsetField = new JTextField();
	private JTextField aAxisHomeOffsetField = new JTextField();
	private JTextField bAxisHomeOffsetField = new JTextField();
	
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
		
		target.setAxisHomeOffset(0, Double.parseDouble(xAxisHomeOffsetField.getText()));
		target.setAxisHomeOffset(1, Double.parseDouble(yAxisHomeOffsetField.getText()));
		target.setAxisHomeOffset(2, Double.parseDouble(zAxisHomeOffsetField.getText()));
		target.setAxisHomeOffset(3, Double.parseDouble(aAxisHomeOffsetField.getText()));
		target.setAxisHomeOffset(4, Double.parseDouble(bAxisHomeOffsetField.getText()));
		
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
		// 0 == inverted, 1 == not inverted
		OnboardParameters.EndstopType endstops = this.target.getInvertedEndstops();
		endstopInversionSelection.setSelectedIndex(endstops.ordinal());

		OnboardParameters.EstopType estop = this.target.getEstopConfig();
		estopSelection.setSelectedIndex(estop.ordinal());
		
		xAxisHomeOffsetField.setText(Double.toString(this.target.getAxisHomeOffset(0)));
		yAxisHomeOffsetField.setText(Double.toString(this.target.getAxisHomeOffset(1)));
		zAxisHomeOffsetField.setText(Double.toString(this.target.getAxisHomeOffset(2)));
		aAxisHomeOffsetField.setText(Double.toString(this.target.getAxisHomeOffset(3)));
		bAxisHomeOffsetField.setText(Double.toString(this.target.getAxisHomeOffset(4)));
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
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MachineOnboardParameters.this.dispose();
			}
		});
		panel.add(cancelButton);
		return panel;
	}
	
	public MachineOnboardParameters(OnboardParameters target, Driver driver) {
		super("Update onboard machine options");
		this.target = target;
		this.driver = driver;
		JPanel panel = new JPanel(new MigLayout());
		panel.add(new JLabel("Machine Name (max. "+Integer.toString(MAX_NAME_LENGTH)+" chars)"));
		machineNameField.setColumns(MAX_NAME_LENGTH);
		panel.add(machineNameField,"wrap");
		panel.add(new JLabel("Invert X axis"));
		panel.add(xAxisInvertBox,"wrap");
		panel.add(new JLabel("Invert Y axis"));
		panel.add(yAxisInvertBox,"wrap");
		panel.add(new JLabel("Invert Z axis"));
		panel.add(zAxisInvertBox,"wrap");
		panel.add(new JLabel("Invert A axis"));
		panel.add(aAxisInvertBox,"wrap");
		panel.add(new JLabel("Invert B axis"));
		panel.add(bAxisInvertBox,"wrap");
		panel.add(new JLabel("Invert endstops"));
		panel.add(endstopInversionSelection,"wrap");
		panel.add(new JLabel("Emergency stop"));
		panel.add(estopSelection,"wrap");
		
		xAxisHomeOffsetField.setColumns(10);
		yAxisHomeOffsetField.setColumns(10);
		zAxisHomeOffsetField.setColumns(10);
		aAxisHomeOffsetField.setColumns(10);
		bAxisHomeOffsetField.setColumns(10);
		
		panel.add(new JLabel("X home offset"));
		panel.add(xAxisHomeOffsetField,"wrap");
		panel.add(new JLabel("Y home offset"));
		panel.add(yAxisHomeOffsetField,"wrap");
		panel.add(new JLabel("Z home offset"));
		panel.add(zAxisHomeOffsetField,"wrap");
		panel.add(new JLabel("A home offset"));
		panel.add(aAxisHomeOffsetField,"wrap");
		panel.add(new JLabel("B home offset"));
		panel.add(bAxisHomeOffsetField,"wrap");
		
		resetToFactoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MachineOnboardParameters.this.resetToFactory();
				// Reload
				loadParameters();
			}
		});
		panel.add(resetToFactoryButton,"wrap");
		
		panel.add(makeButtonPanel());
		add(panel);
		pack();
		loadParameters();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2,
				(screen.height - getHeight()) / 2);
	}
}
