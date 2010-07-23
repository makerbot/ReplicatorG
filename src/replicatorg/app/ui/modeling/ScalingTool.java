package replicatorg.app.ui.modeling;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public class ScalingTool extends Tool {

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

	// If isAbove is true, scale from the bottom of the object; if false, scale from the rough centroid
	boolean isOnPlatform = false;
	
	@Override
	JPanel getControls() {
		JPanel p = new JPanel(new MigLayout("fillx,filly,gap 0"));
		JButton b;

		final JTextField scaleFactor = new JFormattedTextField(NumberFormat.getInstance());
		p.add(scaleFactor,"growx");

		b = new JButton("Scale");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String txt = scaleFactor.getText();
				if (txt != null) {
					try {
						double scale = Double.parseDouble(txt);
						parent.getModel().scale(scale,parent.getModel().isOnPlatform());
					} catch (NumberFormatException nfe) {
						Base.logger.fine("Scale factor "+txt+" is not parseable");
					}
				}
			}
		});
		p.add(b,"growx,wrap");
		
		b = createToolButton("inches->mm","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().scale(25.4d,parent.getModel().isOnPlatform());
			}
		});
		p.add(b,"growx,wrap");

		b = createToolButton("mm->inches","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().scale(1d/25.4d,parent.getModel().isOnPlatform());
			}
		});
		p.add(b,"growx,wrap");

		return p;
	}

	@Override
	public String getInstructions() {
		return Base.isMacOS()?
				"<html><body>Drag to scale object<br>Shift-drag to rotate view<br>Mouse wheel to zoom</body></html>":
				"<html><body>Left drag to scale object<br>Right drag to rotate view<br>Mouse wheel to zoom</body></html>";
	}

	@Override
	String getTitle() {
		return "Scale object";
	}
	
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		isOnPlatform = parent.getModel().isOnPlatform();
	}

	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.NONE; 
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.SCALE_OBJECT; }
		} else {
			if (button == MouseEvent.BUTTON1) { mode = DragMode.SCALE_OBJECT; }
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = -(double)(p.y - startPoint.y);
		switch (mode) {
		case NONE:
			super.mouseDragged(e);
			break;
		case SCALE_OBJECT:
			parent.getModel().scale(1d + (0.01*(xd+yd)), isOnPlatform);
			break;
		}
		startPoint = p;
	}

}
