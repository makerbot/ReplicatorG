package replicatorg.app.ui.modeling;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point3d;

public abstract class Tool {
	public class AxisControl {
		SpinnerNumberModel model;
		JSpinner spinner;
		JCheckBox box;
		public AxisControl(String title, JPanel parent, double initial) {
			model = new SpinnerNumberModel(initial,-100000,100000,0.25);
			spinner = new JSpinner(model);
			box = new JCheckBox("lock");
			parent.add(new JLabel(title));
			parent.add(spinner,"growx");
			parent.add(box,"wrap");
			if (Tool.this instanceof ChangeListener) {
				spinner.addChangeListener((ChangeListener)Tool.this);
			}
		}	
	}
	
	public class CoordinateControl {
		AxisControl[] axes = new AxisControl[3];
		Point3d coordinate;

		public CoordinateControl(JPanel parent, Point3d coordinate) {
			if (coordinate == null) { coordinate = new Point3d(); }
			this.coordinate = coordinate;
			axes[0] = new AxisControl("X",parent, coordinate.x);
			axes[1] = new AxisControl("Y",parent, coordinate.y);
			axes[2] = new AxisControl("Z",parent, coordinate.z);
		}
		
		public void update() {
			axes[0].model.setValue(new Double(coordinate.x));
			axes[1].model.setValue(new Double(coordinate.y));
			axes[2].model.setValue(new Double(coordinate.z));
		}
	}
	
	abstract String getTitle();
	abstract String getButtonName();
	abstract Icon getButtonIcon();
	abstract String getInstructions();
	abstract JPanel getControls();
}
