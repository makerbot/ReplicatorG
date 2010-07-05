package replicatorg.app.ui.modeling;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Icon;
import javax.swing.JPanel;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public class ViewTool implements Tool, MouseMotionListener, MouseListener, MouseWheelListener {

	final ToolPanel parent;
	public ViewTool(ToolPanel parent) {
		this.parent = parent;
	}

	public Icon getButtonIcon() {
		return null;
	}

	public String getButtonName() {
		return "View";
	}

	public JPanel getControls() {
		return null;
	}

	public String getInstructions() {
		return Base.isMacOS()?
				"<html><body>Drag to rotate<br>Shift-drag to pan<br>Mouse wheel to zoom</body></html>":
				"<html><body>Left button drag to rotate<br>Right button drag to pan<br>Mouse wheel to zoom</body></html>";
	}

	public String getTitle() {
		return null;
	}


	Point startPoint = null;
	int button = 0;

	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.ROTATE_VIEW; 
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.ROTATE_VIEW; }
			else if (button == MouseEvent.BUTTON1 && e.isShiftDown()) { mode = DragMode.TRANSLATE_VIEW; }
		} else {
			if (e.isAltDown()) {
				if (button == MouseEvent.BUTTON1) { mode = DragMode.ROTATE_OBJECT; }
				else if (button == MouseEvent.BUTTON3) { mode = DragMode.TRANSLATE_OBJECT; }
			} else {
				if (button == MouseEvent.BUTTON1) { mode = DragMode.ROTATE_VIEW; }
				else if (button == MouseEvent.BUTTON3) { mode = DragMode.TRANSLATE_VIEW; }
			}
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = (double)(p.y - startPoint.y);
		switch (mode) {
		case ROTATE_VIEW:
			// Rotate view
			parent.preview.adjustViewAngle(0.05 * xd, -0.05 * yd);
			break;
		case TRANSLATE_VIEW:
			// Pan view
			parent.preview.adjustViewTranslation(-0.05 * xd, 0.05 * yd);
			break;
		case TRANSLATE_OBJECT:
			parent.getModel().translateObject(0.05*xd,0d,-0.05*yd);
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
		startPoint = e.getPoint();
		button = e.getButton();
	}
	public void mouseReleased(MouseEvent e) {
		startPoint = null;
	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		parent.preview.adjustZoom(0.10 * notches);
	}

}
