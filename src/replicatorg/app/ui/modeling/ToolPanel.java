package replicatorg.app.ui.modeling;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;

public class ToolPanel extends JPanel {

	public JButton createToolButton(String text, String iconPath) {
		ImageIcon icon = new ImageIcon(Base.getImage(iconPath, this));
		JButton button = new JButton(text,icon);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		return button;
	}

	final PreviewPanel preview;
	
	EditingModel getModel() { return preview.getModel(); }
	
	ToolPanel(final PreviewPanel preview) {
		this.preview = preview;
		setLayout(new MigLayout());

		JButton resetViewButton = createToolButton("Reset view","images/look-at-object.png");
		resetViewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				preview.resetView();
			}
		});
		add(resetViewButton,"growx,spanx,wrap");

		JButton sliceButton = createToolButton("Generate GCode","images/model-to-gcode.png");
		sliceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				preview.mainWindow.runToolpathGenerator();
			}
		});
		add(sliceButton,"growx,spanx,wrap");

		JButton centerButton = createToolButton("Center","images/center-object.png");
		centerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getModel().center();
			}
		});
		add(centerButton,"growx,growy");

		JButton flipButton = createToolButton("Flip","images/flip-object.png");
		flipButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getModel().flipZ();
			}
		});
		add(flipButton,"growx,growy,wrap");

		String instrStr = Base.isMacOS()?
				"<html><body>Drag to rotate<br>Shift-drag to pan<br>Mouse wheel to zoom</body></html>":
				"<html><body>Left button drag to rotate<br>Right button drag to pan<br>Mouse wheel to zoom</body></html>";
		JLabel instructions = new JLabel(instrStr);
		Font f = instructions.getFont();
		instructions.setFont(f.deriveFont((float)f.getSize()*0.8f));
		add(instructions,"growx,gaptop 20,spanx,wrap");
	}
}
