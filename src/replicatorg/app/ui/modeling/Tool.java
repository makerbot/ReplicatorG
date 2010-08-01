package replicatorg.app.ui.modeling;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point3d;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public abstract class Tool implements MouseMotionListener, MouseListener, MouseWheelListener {
	public class AxisControl {
		SpinnerNumberModel model;
		JSpinner spinner;
		JCheckBox box;
		public AxisControl(String title, JPanel parent, double initial) {
			model = new SpinnerNumberModel(initial,-100000,100000,1);
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

	public JButton createToolButton(String text, String iconPath) {
		//ImageIcon icon = new ImageIcon(Base.getImage(iconPath, this));
		JButton button = new JButton(text);//,icon);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		return button;
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
	
	final protected ToolPanel parent;
	public Tool(ToolPanel parent) {
		this.parent = parent;
	}

	protected Point startPoint = null;
	protected int button = 0;

	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.ROTATE_VIEW; 
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.ROTATE_VIEW; }
			else if (button == MouseEvent.BUTTON1 && e.isShiftDown()) { mode = DragMode.ROTATE_VIEW; }
		} else {
			if (button == MouseEvent.BUTTON1) { mode = DragMode.ROTATE_VIEW; }
			else if (button == MouseEvent.BUTTON3) { mode = DragMode.ROTATE_VIEW; }
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = (double)(p.y - startPoint.y);
		switch (mode) {
		case ROTATE_VIEW:
			// Rotate view
			parent.preview.adjustViewAngle(-0.05 * xd, -0.05 * yd);
			break;
		case TRANSLATE_VIEW:
			// Pan view
			parent.preview.adjustViewTranslation(-0.5 * xd, 0.5 * yd);
			break;
		}
		startPoint = p;
	}
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
		parent.getModel().startDrag();
		startPoint = e.getPoint();
		button = e.getButton();
	}
	public void mouseReleased(MouseEvent e) {
		parent.getModel().endDrag();
		startPoint = null;
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		parent.preview.adjustZoom(10d * notches);
	}

}
