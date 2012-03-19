package replicatorg.app.ui.modeling;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

public class ToolPanel extends JPanel implements KeyListener {

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
	final JPanel subPanel = new JPanel(new MigLayout("fillx,filly,ins 0,gap 0"));
	
	Tool[] tools = { 
			new ViewTool(this),
			new MoveTool(this),
			new RotationTool(this),
			new MirrorTool(this),
			new ScalingTool(this)
	};
	
	JLabel titleLabel;
	JPanel toolControls = null;
	int ctr=0;
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
		if (toolControls != null) {
			subPanel.remove(toolControls);
		}
		toolControls = tool.getControls();
		if (toolControls != null) {
			subPanel.add(toolControls,"spanx,spany,growx,growy,width 100%");
		} else {
		}
		validate();
		repaint();
	}
	
	EditingModel getModel() { return preview.getModel(); }
	
	final JLabel infoLabel = new JLabel();
	
	ToolPanel(final PreviewPanel preview) {
		this.preview = preview;
		setLayout(new MigLayout("gap 0,filly,wrap 1"));

		JPanel toolButtons = new JPanel(new MigLayout("gap 0 0,ins 0,novisualpadding,wrap 1","0[100%]0"));
		
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
		

		JButton sliceButton = createToolButton("Generate GCode","images/model-to-gcode.png");
		sliceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				preview.mainWindow.runToolpathGenerator(false);
				
			}
		});
		toolButtons.add(sliceButton,"growx,spanx,wrap");

		titleLabel = new JLabel("Selected Tool");
		add(titleLabel,"growx,gap 5,spanx,north");
		{
			//Font f = titleLabel.getFont();
			titleLabel.setFont(new Font("FreeSans",Font.BOLD, 14));
		}
//		for (String s : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
//			System.err.println("AVAILABLE: " + s);
//		}
		
		Font f = infoLabel.getFont();
		infoLabel.setFont(f.deriveFont((float)f.getSize()*0.8f));
		add(infoLabel,"growx,gap 2,spanx,south");

		add(subPanel,"spanx,growx,growy,width 100%");
		setTool(tools[0]);
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}
}
