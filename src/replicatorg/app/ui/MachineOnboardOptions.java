/**
 * 
 */
package replicatorg.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import replicatorg.drivers.OnboardParameters;
import replicatorg.machine.model.Axis;

/**
 * A panel for editing the options stored onboard a machine.
 * @author phooky
 *
 */
public class MachineOnboardOptions extends JFrame {
	private OnboardParameters target;
	private JTextField machineNameField = new JTextField();
	private JCheckBox xAxisInvertBox = new JCheckBox("Invert X axis");
	private JCheckBox yAxisInvertBox = new JCheckBox("Invert Y axis");
	private JCheckBox zAxisInvertBox = new JCheckBox("Invert Z axis");
	
	private JComponent addLabel(String label, JComponent needsLabel) {
		Box hbox = Box.createHorizontalBox();
		hbox.add(new JLabel(label));
		hbox.add(needsLabel);
		return hbox;
	}
	
	private void commit() {
		target.setMachineName(machineNameField.getText());
		EnumSet<Axis> axesInverted = EnumSet.noneOf(Axis.class);
		if (xAxisInvertBox.isSelected()) axesInverted.add(Axis.X);
		if (yAxisInvertBox.isSelected()) axesInverted.add(Axis.Y);
		if (zAxisInvertBox.isSelected()) axesInverted.add(Axis.Z);
		target.setInvertedParameters(axesInverted);
	}
	private JComponent makeButtons() {
		Box hbox = Box.createHorizontalBox();
		JButton commitButton = new JButton("Commit Changes");
		hbox.add(commitButton);
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MachineOnboardOptions.this.commit();
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MachineOnboardOptions.this.dispose();
			}
		});
		hbox.add(cancelButton);
		hbox.add(Box.createHorizontalGlue());
		return hbox;
	}
	public MachineOnboardOptions(OnboardParameters target) {
		this.target = target;
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		machineNameField.setText(this.target.getMachineName());
		add(addLabel("Machine Name:",machineNameField));
		EnumSet<Axis> invertedAxes = this.target.getInvertedParameters();
		xAxisInvertBox.setSelected(invertedAxes.contains(Axis.X));
		add(xAxisInvertBox);
		yAxisInvertBox.setSelected(invertedAxes.contains(Axis.Y));
		add(yAxisInvertBox);
		zAxisInvertBox.setSelected(invertedAxes.contains(Axis.Z));
		add(zAxisInvertBox);
		add(makeButtons());
	}
}
