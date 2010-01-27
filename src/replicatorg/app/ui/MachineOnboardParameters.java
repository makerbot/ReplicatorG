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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.drivers.OnboardParameters;
import replicatorg.machine.model.Axis;

/**
 * A panel for editing the options stored onboard a machine.
 * @author phooky
 *
 */
public class MachineOnboardParameters extends JFrame {
	private final OnboardParameters target;
	private JTextField machineNameField = new JTextField();
	private JCheckBox xAxisInvertBox = new JCheckBox();
	private JCheckBox yAxisInvertBox = new JCheckBox();
	private JCheckBox zAxisInvertBox = new JCheckBox();
	private JButton extruderButton = new JButton("Set extruder parameters");
	private static final int MAX_NAME_LENGTH = 16;
	
	private void commit() {
		target.setMachineName(machineNameField.getText());
		EnumSet<Axis> axesInverted = EnumSet.noneOf(Axis.class);
		if (xAxisInvertBox.isSelected()) axesInverted.add(Axis.X);
		if (yAxisInvertBox.isSelected()) axesInverted.add(Axis.Y);
		if (zAxisInvertBox.isSelected()) axesInverted.add(Axis.Z);
		target.setInvertedParameters(axesInverted);
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
	
	public MachineOnboardParameters(OnboardParameters target) {
		super("Update onboard machine options");
		this.target = target;
		JPanel panel = new JPanel(new MigLayout());
		machineNameField.setText(this.target.getMachineName());
		machineNameField.setColumns(MAX_NAME_LENGTH);
		panel.add(new JLabel("Machine Name (max. "+Integer.toString(MAX_NAME_LENGTH)+" chars)"));
		panel.add(machineNameField,"wrap");
		EnumSet<Axis> invertedAxes = this.target.getInvertedParameters();
		xAxisInvertBox.setSelected(invertedAxes.contains(Axis.X));
		panel.add(new JLabel("Invert X axis"));
		panel.add(xAxisInvertBox,"wrap");
		yAxisInvertBox.setSelected(invertedAxes.contains(Axis.Y));
		panel.add(new JLabel("Invert Y axis"));
		panel.add(yAxisInvertBox,"wrap");
		zAxisInvertBox.setSelected(invertedAxes.contains(Axis.Z));
		panel.add(new JLabel("Invert Z axis"));
		panel.add(zAxisInvertBox,"wrap");

		extruderButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ExtruderOnboardParameters eop = new ExtruderOnboardParameters(MachineOnboardParameters.this.target);
				eop.setVisible(true);
			}
		});
		panel.add(extruderButton,"wrap");
		
		panel.add(makeButtonPanel());
		add(panel);
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2,
				(screen.height - getHeight()) / 2);
	}
}
