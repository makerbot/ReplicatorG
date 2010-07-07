package replicatorg.app.ui.modeling;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.media.j3d.Transform3D;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import net.miginfocom.swing.MigLayout;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public class MoveTool extends Tool implements MouseMotionListener, MouseListener, MouseWheelListener,
	ChangeListener {
	final ToolPanel parent;
	public MoveTool(ToolPanel parent) {
		this.parent = parent;
	}
	
	Transform3D vt;

	public Icon getButtonIcon() {
		return null;
	}

	public String getButtonName() {
		return "Move";
	}

	Point3d delta = new Point3d();
	CoordinateControl control;
	
	public JPanel getControls() {
		JPanel p = new JPanel(new MigLayout());
		relativeZero = new Point3d();
		delta = new Point3d();
		control = new CoordinateControl(p,delta);
		control.update();
		return p;
	}

	public String getInstructions() {
		return Base.isMacOS()?
				"<html><body>Drag to move<br>Shift-drag to rotate<br>Mouse wheel to zoom</body></html>":
				"<html><body>Left button drag to move object<br>Right button drag to rotate<br>Mouse wheel to zoom</body></html>";
	}

	public String getTitle() {
		return "Move Object";
	}

	Point startPoint = null;
	int button = 0;
	
	Point3d relativeZero;

	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.ROTATE_VIEW; 
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.TRANSLATE_OBJECT; }
			else if (button == MouseEvent.BUTTON1 && e.isShiftDown()) { mode = DragMode.ROTATE_VIEW; }
		} else {
			if (button == MouseEvent.BUTTON1) { mode = DragMode.TRANSLATE_OBJECT; }
			else if (button == MouseEvent.BUTTON3) { mode = DragMode.ROTATE_VIEW; }
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = -(double)(p.y - startPoint.y);
		switch (mode) {
		case ROTATE_VIEW:
			parent.preview.adjustViewAngle(0.05 * xd, 0.05 * yd);
			break;
		case TRANSLATE_OBJECT:
			doTranslate(xd,yd);
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
	
	double objectDistance;
	
	public void mousePressed(MouseEvent e) {
		startPoint = e.getPoint();
		button = e.getButton();
		// Set up view transform
		vt = parent.preview.getViewTransform();
		// Scale view transform to account for object distance
		Point3d centroid = parent.getModel().getCentroid();
		vt.transform(centroid);
		objectDistance = centroid.distance(new Point3d());
	}
	public void mouseReleased(MouseEvent e) {
		startPoint = null;
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		parent.preview.adjustZoom(10 * notches);
	}
	
	void doTranslate(double deltaX, double deltaY) {
		Vector3d v = new Vector3d(deltaX,deltaY,0d);
		vt.transform(v);
		parent.getModel().translateObject(v.x,v.y,v.z);
	}

	public void stateChanged(ChangeEvent arg0) {
	}
}
