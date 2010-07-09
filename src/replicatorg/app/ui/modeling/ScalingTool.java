package replicatorg.app.ui.modeling;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public class ScalingTool extends Tool implements MouseMotionListener, MouseListener, MouseWheelListener {

	public ScalingTool(ToolPanel parent) {
		super(parent);
	}

	@Override
	Icon getButtonIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	String getButtonName() {
		return "Scale";
	}

	@Override
	JPanel getControls() {
		JPanel p = new JPanel(new MigLayout("fillx,filly"));

		JButton b;
		b = createToolButton("inches->mm","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().scale(25.4d);
			}
		});
		p.add(b,"growx,wrap");

		b = createToolButton("mm->inches","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().scale(1d/25.4d);
			}
		});
		p.add(b,"growx,wrap");

		return p;
	}

	@Override
	String getInstructions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	String getTitle() {
		return "Scale object";
	}

	Point startPoint = null;
	int button = 0;
	
	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.ROTATE_VIEW; 
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.SCALE_OBJECT; }
			else if (button == MouseEvent.BUTTON1 && e.isShiftDown()) { mode = DragMode.ROTATE_VIEW; }
		} else {
			if (button == MouseEvent.BUTTON1) { mode = DragMode.SCALE_OBJECT; }
			else if (button == MouseEvent.BUTTON3) { mode = DragMode.ROTATE_VIEW; }
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = -(double)(p.y - startPoint.y);
		switch (mode) {
		case ROTATE_VIEW:
			parent.preview.adjustViewAngle(0.05 * xd, 0.05 * yd);
			break;
		case SCALE_OBJECT:
			parent.getModel().scale(1d + (0.01*(xd+yd)));
			break;
		}
		startPoint = p;
	}

	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		startPoint = e.getPoint();
		button = e.getButton();
	}

	public void mouseReleased(MouseEvent e) {
		startPoint = null;
	}


	public void mouseWheelMoved(MouseWheelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
