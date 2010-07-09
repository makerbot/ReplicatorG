package replicatorg.app.ui.modeling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;

public class ViewTool extends Tool {
	public ViewTool(ToolPanel parent) {
		super(parent);
	}

	public Icon getButtonIcon() {
		return null;
	}

	public String getButtonName() {
		return "View";
	}

	public JPanel getControls() {
		JPanel p = new JPanel(new MigLayout("fillx,filly"));
		JButton b;
		b = createToolButton("Center","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.preview.resetView();
			}
		});
		p.add(b,"growx");

		b = createToolButton("XY","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.preview.viewXY();
			}
		});
		p.add(b,"growx,wrap");

		b = createToolButton("XZ","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.preview.viewXZ();
			}
		});
		p.add(b,"growx");

		b = createToolButton("YZ","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.preview.viewYZ();
			}
		});
		p.add(b,"growx,wrap");

		return p;
	}

	public String getInstructions() {
		return Base.isMacOS()?
				"<html><body>Drag to rotate<br>Shift-drag to pan<br>Mouse wheel to zoom</body></html>":
				"<html><body>Left button drag to rotate<br>Right button drag to pan<br>Mouse wheel to zoom</body></html>";
	}

	public String getTitle() {
		return "Preview";
	}



}
