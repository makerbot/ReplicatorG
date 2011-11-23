package replicatorg.app.ui.modeling;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.media.j3d.Transform3D;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.vecmath.Vector3d;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.modeling.PreviewPanel.DragMode;

public class MoveTool extends Tool {
	public MoveTool(ToolPanel parent) {
		super(parent);
	}
	
	Transform3D vt;

	public Icon getButtonIcon() {
		return null;
	}

	public String getButtonName() {
		return "Move";
	}

	JCheckBox lockZ;
	JFormattedTextField transX, transY, transZ;
	
	public JPanel getControls() {
		JPanel p = new JPanel(new MigLayout("fillx,filly,gap 0"));
		JButton centerButton = createToolButton("Center","images/center-object.png");
		centerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().center();
			}
		});
		p.add(centerButton,"growx,wrap,spanx");

		JButton lowerButton = createToolButton("Put on platform","images/center-object.png");
		lowerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().putOnPlatform();
			}
		});
		p.add(lowerButton,"growx,wrap,spanx");
		transX = new JFormattedTextField(Base.getLocalFormat());
		Dimension d = transX.getPreferredSize();
		d.width = 800;
		transX.setPreferredSize(d);
		transY = new JFormattedTextField(Base.getLocalFormat());
		transZ = new JFormattedTextField(Base.getLocalFormat());
		transX.setValue(10);
		transY.setValue(10);
		transZ.setValue(10);
		JButton transXplus = new JButton("X+");
		JButton transYplus = new JButton("Y+");
		JButton transZplus = new JButton("Z+");
		JButton transXminus = new JButton("X-");
		JButton transYminus = new JButton("Y-");
		JButton transZminus = new JButton("Z-");
		p.add(transXminus);
		p.add(transX,"growx");
		p.add(transXplus,"wrap");
		p.add(transYminus);
		p.add(transY,"growx");
		p.add(transYplus,"wrap");
		p.add(transZminus);
		p.add(transZ,"growx");
		p.add(transZplus,"wrap");
		
		transXplus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String txt = transX.getText();
				if (txt != null) {
					try {
						double transXval = Base.getLocalFormat().parse(txt).doubleValue();
						parent.getModel().translateObject(transXval, 0, 0);
					} catch (Exception e) {
						Base.logger.fine("Problem parsing number or translating object.");
					}
				}
			}
		});
		transXminus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String txt = transX.getText();
				if (txt != null) {
					try {
						double transXval = Base.getLocalFormat().parse(txt).doubleValue();
						parent.getModel().translateObject(-transXval, 0, 0);
					} catch (Exception e) {
						Base.logger.fine("Problem parsing number or translating object.");
					}
				}
			}
		});
		transYplus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String txt = transY.getText();
				if (txt != null) {
					try {
						double transYval = Base.getLocalFormat().parse(txt).doubleValue();
						parent.getModel().translateObject(0,transYval, 0);
					} catch (Exception e) {
						Base.logger.fine("Problem parsing number or translating object.");
					}
				}
			}
		});
		transYminus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String txt = transY.getText();
				if (txt != null) {
					try {
						double transYval = Base.getLocalFormat().parse(txt).doubleValue();
						parent.getModel().translateObject(0,-transYval,0);
					} catch (Exception e) {
						Base.logger.fine("Problem parsing number or translating object.");
					}
				}
			}
		});
		transZplus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String txt = transZ.getText();
				if (txt != null) {
					try {
						double transZval = Base.getLocalFormat().parse(txt).doubleValue();
						parent.getModel().translateObject(0,0,transZval);
					} catch (Exception e) {
						Base.logger.fine("Problem parsing number or translating object.");
					}
				}
			}
		});
		transZminus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String txt = transZ.getText();
				if (txt != null) {
					try {
						double transZval = Base.getLocalFormat().parse(txt).doubleValue();
						parent.getModel().translateObject(0,0,-transZval);
					} catch (Exception e) {
						Base.logger.fine("Problem parsing number or translating object.");
					}
				}
			}
		});
		lockZ = new JCheckBox("Lock height");
		p.add(lockZ,"growx,wrap,spanx");
		
		return p;
	}

	public String getInstructions() {
		return Base.isMacOS()?
				"<html><body>Drag to move object<br>Shift-drag to rotate view<br>Mouse wheel to zoom</body></html>":
				"<html><body>Left drag to move object<br>Right drag to rotate view<br>Mouse wheel to zoom</body></html>";
	}

	public String getTitle() {
		return "Move Object";
	}

	public void mouseDragged(MouseEvent e) {
		if (startPoint == null) return;
		Point p = e.getPoint();
		DragMode mode = DragMode.NONE; 
		if (Base.isMacOS()) {
			if (button == MouseEvent.BUTTON1 && !e.isShiftDown()) { mode = DragMode.TRANSLATE_OBJECT; }
		} else {
			if (button == MouseEvent.BUTTON1) { mode = DragMode.TRANSLATE_OBJECT; }
		}
		double xd = (double)(p.x - startPoint.x);
		double yd = -(double)(p.y - startPoint.y);
		switch (mode) {
		case NONE:
			super.mouseDragged(e);
			break;
		case TRANSLATE_OBJECT:
			doTranslate(xd,yd);
			break;
		}
		startPoint = p;
	}
		
	public void mousePressed(MouseEvent e) {
		// Set up view transform
		vt = parent.preview.getViewTransform();
		super.mousePressed(e);
	}
	
	void doTranslate(double deltaX, double deltaY) {
		Vector3d v = new Vector3d(deltaX,deltaY,0d);
		vt.transform(v);
		if (lockZ.isSelected()) { v.z = 0d; }
		parent.getModel().translateObject(v.x,v.y,v.z);
	}

}
