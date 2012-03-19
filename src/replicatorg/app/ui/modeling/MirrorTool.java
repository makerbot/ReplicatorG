package replicatorg.app.ui.modeling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class MirrorTool extends Tool {
	public MirrorTool(ToolPanel parent) {
		super(parent);
	}
	
	@Override
	Icon getButtonIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	String getButtonName() {
		return "Mirror";
	}

	@Override
	JPanel getControls() {
		JPanel p = new JPanel(new MigLayout("fillx,filly"));
		JButton b;
		b = createToolButton("Reflect in X","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().mirrorX();
			}
		});
		p.add(b,"growx,wrap");

		b = createToolButton("Reflect in Y","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().mirrorY();
			}
		});
		p.add(b,"growx,wrap");

		b = createToolButton("Reflect in Z","images/center-object.png");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				parent.getModel().mirrorZ();
			}
		});
		p.add(b,"growx,wrap");

		return p;
	}

	@Override
	public String getInstructions() {
		return "<html><body>Drag to rotate view<br>Mouse wheel to zoom</body></html>";
	}

	@Override
	String getTitle() {
		return "Reflect object";
	}

}
