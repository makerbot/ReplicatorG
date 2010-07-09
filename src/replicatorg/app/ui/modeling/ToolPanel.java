package replicatorg.app.ui.modeling;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

public class ToolPanel extends JPanel {

	public JButton createToolButton(String text, String iconPath) {
		//ImageIcon icon = new ImageIcon(Base.getImage(iconPath, this));
		JButton button = new JButton(text);//,icon);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		return button;
	}

	public JButton createToolButton(final Tool tool) {
		JButton button = new JButton(tool.getButtonName(), tool.getButtonIcon());
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				setTool(tool);
			}
		});
		return button;
	}

	final PreviewPanel preview;
	final JPanel subPanel = new JPanel(new MigLayout("fillx,filly"));
	
	Tool[] tools = { 
			new ViewTool(this),
			new MoveTool(this),
			new RotationTool(this),
			new MirrorTool(this),
			new ScalingTool(this)
	};
	
	JLabel titleLabel;
	
	void setTool(Tool tool) {
		// Default to the view tool
		if (tool == null) { tool = tools[0]; }
		// Connect this tool to the preview panel's mouse and keyboard handlers
		preview.setTool(tool);
		// Set the tool title
		titleLabel.setText(tool.getTitle());
		// Set tool instructions
		infoLabel.setText(tool.getInstructions());
		// Add subpanel
		subPanel.removeAll();
		JPanel toolControls = tool.getControls();
		if (toolControls != null) {
			subPanel.add(toolControls,"growx,growy");
		} else {
			subPanel.repaint();
		}
		
	}
	
	EditingModel getModel() { return preview.getModel(); }
	
	final JLabel infoLabel = new JLabel();
	
	ToolPanel(final PreviewPanel preview) {
		this.preview = preview;
		setLayout(new MigLayout("gap 0,filly"));

		JPanel toolButtons = new JPanel(new MigLayout("gap 0 0,ins 0,novisualpadding","0[50%]0[50%]0"));
		
		add(toolButtons,"south");
	
		int column = 0;
		final int COL_COUNT = 2;
		for (Tool t : tools) {
			column++;
			JButton b = createToolButton(t);
			if (column == COL_COUNT) {
				toolButtons.add(b,"growx,growy,wrap");
				column = 0;
			} else { 
				toolButtons.add(b,"growx,growy");
			}
			
		}
		
		JButton resetViewButton = createToolButton("Reset view","images/look-at-object.png");
		resetViewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				preview.resetView();
			}
		});
		toolButtons.add(resetViewButton,"growx,spanx,wrap");

		JButton sliceButton = createToolButton("Generate GCode","images/model-to-gcode.png");
		sliceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				preview.mainWindow.runToolpathGenerator();
			}
		});
		toolButtons.add(sliceButton,"growx,spanx,wrap");

		JButton flipButton = createToolButton("Flip","images/flip-object.png");
		flipButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				getModel().flipZ();
			}
		});
		toolButtons.add(flipButton,"growx,growy,wrap");

		titleLabel = new JLabel("Selected Tool");
		add(titleLabel,"growx,gap 5,spanx,north");
		{
			//Font f = titleLabel.getFont();
			titleLabel.setFont(new Font("FreeSans",Font.BOLD, 14));
		}
		for (String s : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
			System.err.println("AVAILABLE: " + s);
		}
		
		Font f = infoLabel.getFont();
		infoLabel.setFont(f.deriveFont((float)f.getSize()*0.8f));
		add(infoLabel,"growx,gap 2,spanx,south");

		add(subPanel,"growx,growy,spanx");
		setTool(tools[0]);
	}
}
