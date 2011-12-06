package replicatorg.app.ui.controlpanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.model.AxisId;
import replicatorg.util.Point5d;

public class JogPanel extends JPanel implements ActionListener, MouseListener
{
	private final String JOGMODE_PREF_NAME = "controlpanel.jogmode";
	
	protected boolean continuousJogMode = false;
	protected double jogRate;

	protected Pattern jogPattern;

	protected String[] jogStrings = { "0.01mm", "0.05mm", "0.1mm", "0.5mm",
			"1mm", "5mm", "10mm", "20mm", "50mm", "Continuous Jog" };

	protected final Point5d feedrate;

	protected EnumMap<AxisId,JTextField> positionFields = new EnumMap<AxisId,JTextField>(AxisId.class);

	protected MachineInterface machine;

	public class JogButton extends JButton {
		public JogButton(String root, String tooltip) {
			BufferedImage img = Base.getImage("images/"+root+".png", this);					
			setIcon(new ImageIcon(img));
			BufferedImage overImg = Base.getImage("images/"+root+"Over.png",this);
			if (overImg != null) {
				setRolloverIcon(new ImageIcon(overImg));
				setRolloverEnabled(true);
			}
			BufferedImage downImg = Base.getImage("images/"+root+"Down.png",this);
			if (downImg == null) { downImg = overImg; }
			if (downImg != null) {
				setSelectedIcon(new ImageIcon(downImg));
			}
			Dimension imgSize = new Dimension(img.getWidth(null),img.getHeight(null));
			setSize(imgSize);
			setMinimumSize(imgSize);
			setPreferredSize(imgSize);
			setOpaque(false);
			setFocusPainted(false);
			setBorderPainted(false);
			setContentAreaFilled(false);
			setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
			setToolTipText(tooltip);
		}
	}

	/**
	 * Create a jog-style button with the given name and tooltip.  By default, the
	 * action name is the same as the text of the button.  The button will emit an
	 * action event to the jog panel when it is clicked.
	 * @param text the text to display on the button.
	 * @param tooltip the text to display when the mouse hovers over the button.
	 * @return the generated button.
	 */
	protected JButton createJogButton(String root, String tooltip) {
		JogButton b = new JogButton(root,tooltip);
		b.addActionListener(this);
		b.addMouseListener(this);
		return b;
	}

	/**
	 * Create a jog-style button with the given name and tooltip.  The action
	 * name is specified by the caller.  The button will emit an
	 * action event to the jog panel when it is clicked.
	 * @param text the text to display on the button.
	 * @param tooltip the text to display when the mouse hovers over the button.
	 * @param action the string representing the action.
	 * @return the generated button.
	 */
	protected JButton createJogButton(String root, String tooltip, String action) {
		JButton button = createJogButton(root,tooltip);
		button.setActionCommand(action);
		return button;
	}

	/**
	 * Create a text field for dynamic data display 
	 */
	protected JTextField createDisplayField() {
		JTextField tf = new JTextField();
		tf.setEnabled(false);
		tf.setDisabledTextColor(Color.BLACK);
		return tf;
	}
	
	private void setJogMode(String mode) {
		if ("Continuous Jog".equals(mode)) {
			if(this.machine.getDriver().hasSoftStop())
			{
				continuousJogMode = true;
				jogRate = 0;
			}
		} else {
			// If we were in continuous jog mode, send a stop to be safe
			if (continuousJogMode) {
				this.machine.stopMotion();			
			}
			continuousJogMode = false;
			Matcher jogMatcher = jogPattern.matcher(mode);
			if (jogMatcher.find())
				jogRate = Double.parseDouble(jogMatcher.group(1));
		}
		if (mode != null && mode.length() > 0) {
			Base.preferences.put(JOGMODE_PREF_NAME,mode);
		}		
	}
	
	private JPanel buildPositionPanel() {
		// create our position panel
		JPanel positionPanel = new JPanel(new MigLayout("flowy,fillx"));
		// our label
		positionPanel.add(new JLabel("Jog Mode"),"growx");
		// create our jog size dropdown
		JComboBox jogList = new JComboBox(jogStrings);
		jogList.setSelectedItem(Base.preferences.get(JOGMODE_PREF_NAME,"1mm"));
		jogList.setActionCommand("jog size");
		jogList.addActionListener(this);
		setJogMode((String)jogList.getSelectedItem());
		positionPanel.add(jogList,"growx");
		
		// our position text boxes
		for (final AxisId axis : machine.getModel().getAvailableAxes()) {
			JTextField f = createDisplayField();
			positionFields.put(axis, f);
			positionPanel.add(new JLabel(axis.name()),"split 3,flowx");
			positionPanel.add(f,"growx");
			JButton centerButton = new JButton("Center "+axis.name());
			centerButton.setToolTipText("Jog "+axis.name()+" axis to the origin");
			centerButton.setActionCommand("Center "+axis.name());
			centerButton.addActionListener(this);
			positionPanel.add(centerButton);
		}
		JButton zeroButton = new JButton("Make current position zero");
		zeroButton.setToolTipText("Mark Current Position as zero (0,0,0).  Will not move the toolhead.");
		zeroButton.setActionCommand("Zero");
		zeroButton.addActionListener(this);
		positionPanel.add(zeroButton,"growx");
		return positionPanel;
	}

	private class FeedrateControl implements ActionListener, FocusListener, ChangeListener {
		final JSlider slider;
		final AxisId axis;
		final JTextField field;

		private FeedrateControl(String display, AxisId axis, JPanel parent) {
			this.axis = axis;
			slider = new JSlider(JSlider.HORIZONTAL);
			field = new JTextField();
			parent.add(new JLabel(display));
			int maxFeedrate = (int)machine.getModel().getMaximumFeedrates().axis(axis);
			int currentFeedrate = Math.min(maxFeedrate, Base.preferences.getInt(getPrefName(),480));
			feedrate.setAxis(axis, currentFeedrate);
			slider.setMinimum(1);
			slider.setMaximum(maxFeedrate);
			slider.setValue(currentFeedrate);
			slider.addChangeListener(this);
			parent.add(slider,"growx");
			field.setMinimumSize(new Dimension(75, 22));
			field.setEnabled(true);
			field.setText(Integer.toString(currentFeedrate));
			field.addFocusListener(this);
			field.addActionListener(this);
			parent.add(field,"growx");
			parent.add(new JLabel("mm/min."),"wrap");
		}

		String getPrefName() { 
			return "controlpanel.feedrate."+axis.name().toLowerCase();
		}

		void updateFromField() {
			int val = Integer.parseInt(field.getText());
			feedrate.setAxis(axis, val);
			Base.preferences.putInt(getPrefName(), val);
			slider.setValue(val);
		}
		
		public void actionPerformed(ActionEvent e) {
			updateFromField();
		}

		public void focusGained(FocusEvent e) {
		}

		public void focusLost(FocusEvent e) {
			updateFromField();
		}

		public void stateChanged(ChangeEvent e) {
			int val = slider.getValue();
			feedrate.setAxis(axis, val);
			Base.preferences.putInt(getPrefName(), val);			
			field.setText(Integer.toString(val));			
		}
	}

	// Make a rotation tool (for A and B axes)
	private JPanel makeRotationPanel(AxisId axis) {
		JButton cwButton = createJogButton("jog/CW", "Jog "+axis.name()+" axis in clockwise direction", axis.name()+"+");
		JButton ccwButton = createJogButton("jog/CCW", "Jog "+axis.name()+" axis in counterclockwise direction", axis.name()+"-");
		JPanel panel = new JPanel(new MigLayout());
		panel.add(new JLabel(axis.name()));
		panel.add(cwButton,"split 2,flowy");
		panel.add(ccwButton);
		return panel;
	}
	
	public JogPanel(MachineInterface machine) {
		feedrate = new Point5d();
		this.machine = machine;
		Set<AxisId> axes = machine.getModel().getAvailableAxes();
		
		setLayout(new MigLayout("gap 0, ins 0"));
		
		// compile our regexes
		jogRate = 10.0;
		jogPattern = Pattern.compile("([.0-9]+)");
		
		// If it does have soft stops, happy continuous jogging!!
		if(!this.machine.getDriver().hasSoftStop())
		{
			List<String> list = new ArrayList<String>(Arrays.asList(jogStrings));
			list.removeAll(Arrays.asList("Continuous Jog"));
			jogStrings = list.toArray(jogStrings);
		}
		
		JButton xPlusButton = createJogButton("jog/X+", "Jog X axis in positive direction", "X+");
		JButton xMinusButton = createJogButton("jog/X-", "Jog X axis in negative direction", "X-");
		JButton yPlusButton = createJogButton("jog/Y+", "Jog Y axis in positive direction", "Y+");
		JButton yMinusButton = createJogButton("jog/Y-", "Jog Y axis in negative direction", "Y-");
		JButton panicButton = createJogButton("jog/panic","Emergency stop","Stop");

		JPanel jogButtonPanel = new JPanel(new MigLayout("nogrid, ins 0"));
		JPanel xyPanel = new JPanel(new MigLayout("ins 0","[]2[]2[]","[]2[]2[]"));
        //xyzPanel.add(zCenterButton, );
		xyPanel.add(yPlusButton, "skip 1,wrap,growx,growy");
		xyPanel.add(xMinusButton,"growx,growy");
		if(this.machine.getDriver().hasEmergencyStop()) {
			xyPanel.add(panicButton,"growx,growy");
		} else
		{
			JButton dummyButton = createJogButton("jog/dummy","");
			xyPanel.add(dummyButton,"growx,growy");
		}
		xyPanel.add(xPlusButton,"growx,growy,wrap");
		xyPanel.add(yMinusButton, "skip 1,growx,growy,wrap");
		jogButtonPanel.add(xyPanel);
		if (axes.contains(AxisId.Z)) {
			JButton zPlusButton = createJogButton("jog/Z+", "Jog Z axis in positive direction", "Z+");
			JButton zMinusButton = createJogButton("jog/Z-", "Jog Z axis in negative direction", "Z-");
			JPanel zPanel = new JPanel(new MigLayout("flowy"));
			zPanel.add(zPlusButton);
			zPanel.add(zMinusButton);
			jogButtonPanel.add(zPanel,"wrap");
		}
		if (axes.contains(AxisId.A)) {
			jogButtonPanel.add(makeRotationPanel(AxisId.A));
		}		
		if (axes.contains(AxisId.B)) {
			jogButtonPanel.add(makeRotationPanel(AxisId.B));
		}

		// create the xyfeedrate panel
		JPanel feedratePanel = new JPanel(new MigLayout());

		if (axes.contains(AxisId.X) || axes.contains(AxisId.Y)) {
			new FeedrateControl("XY Feedrate",AxisId.X,feedratePanel);
		}

		if (axes.contains(AxisId.Z)) {
			new FeedrateControl("Z Feedrate",AxisId.Z,feedratePanel);
		}
		if (axes.contains(AxisId.A)) {
			new FeedrateControl("A Feedrate",AxisId.A,feedratePanel);
		}
		if (axes.contains(AxisId.B)) {
			new FeedrateControl("B Feedrate",AxisId.B,feedratePanel);
		}
		// add it all to our jog panel
		add(jogButtonPanel);
		add(buildPositionPanel(),"growx,wrap");
		add(feedratePanel,"growx,spanx");

		// add jog panel border and stuff.
		setBorder(BorderFactory.createTitledBorder("Jog Controls"));
	}
	

	private NumberFormat positionFormatter = Base.getLocalFormat();

	synchronized public void updateStatus() {
		Point5d current = machine.getDriverQueryInterface().getCurrentPosition(false);

		for (AxisId axis : machine.getModel().getAvailableAxes()) {
			double v = current.axis(axis);
			positionFields.get(axis).setText(positionFormatter.format(v));
		}
	}

	Pattern jogActionParser = Pattern.compile("([XYZAB])([\\+\\-])");
	Pattern centerActionParser = Pattern.compile("Center ([XYZAB])");
	
	public void actionPerformed(ActionEvent e) {
		Point5d current = machine.getDriverQueryInterface().getCurrentPosition(false);

		String s = e.getActionCommand();

		Matcher jogMatch = jogActionParser.matcher(s);
		Matcher centerMatch = centerActionParser.matcher(s);
		if (jogMatch.matches()) {
			// If continuous jog mode, let the mouse listener interface handle it.
			if (!continuousJogMode) {
				AxisId axis = AxisId.valueOf(jogMatch.group(1));
				boolean positive = jogMatch.group(2).equals("+");
				current.setAxis(axis, current.axis(axis) + (positive?jogRate:-jogRate));
				double f = feedrate.axis(axis);
				// Exception: XY feedrate is assumed to be X feedrate (symmetrical)
				if (axis.equals(AxisId.Y)) { f = feedrate.axis(AxisId.X); }
				machine.runCommand(new replicatorg.drivers.commands.SetFeedrate(f));
				machine.runCommand(new replicatorg.drivers.commands.QueuePoint(current));
			}
		} else if (s.equals("Stop")) {
			machine.stopMotion();
			// FIXME: If we reenable the control panel while printing, 
			// we should check this, call this.machine.stop(),
			// plus communicate this action back to the main window
		} else if (centerMatch.matches()) {
			AxisId axis = AxisId.valueOf(centerMatch.group(1));
			current.setAxis(axis, 0);
			double f = feedrate.axis(axis);
			// Exception: XY feedrate is assumed to be X feedrate (symmetrical)
			if (axis.equals(AxisId.Y)) { f = feedrate.axis(AxisId.X); }
			machine.runCommand(new replicatorg.drivers.commands.SetFeedrate(f));
			machine.runCommand(new replicatorg.drivers.commands.QueuePoint(current));
		} else if (s.equals("Zero")) {
			// "Zero" tells the machine to calibrate its
			// current position as zero, not to move to its
			// currently-set zero position.
			machine.runCommand(new replicatorg.drivers.commands.SetCurrentPosition(new Point5d()));
		}
		// get our new jog rate
		else if (s.equals("jog size")) {
			JComboBox cb = (JComboBox) e.getSource();
			String jogText = (String) cb.getSelectedItem();
			setJogMode(jogText);
		} else {
			Base.logger.warning("Unknown Action Event: " + s);
		}
	}

	public void mouseClicked(MouseEvent arg0) {
		// Ignore; let default handler take care of it
	}

	public void mouseEntered(MouseEvent arg0) {
		// Ignore; let default handler take care of it
	}

	public void mouseExited(MouseEvent arg0) {
		// Ignore; let default handler take care of it
	}

	public void mousePressed(MouseEvent e) {
		if (continuousJogMode) {
			Point5d current = machine.getDriverQueryInterface().getCurrentPosition(false);

			String s = ((JButton)e.getSource()).getActionCommand();
			Matcher jogMatch = jogActionParser.matcher(s);
			if (jogMatch.matches()) {
				AxisId axis = AxisId.valueOf(jogMatch.group(1));
				boolean positive = jogMatch.group(2).equals("+");
				// Fake it by sending a 1m move
				current.setAxis(axis, current.axis(axis) + (positive?1000:-1000));
				double f = feedrate.axis(axis);
				// Exception: XY feedrate is assumed to be X feedrate (symmetrical)
				if (axis.equals(AxisId.Y)) { f = feedrate.axis(AxisId.X); }
				machine.runCommand(new replicatorg.drivers.commands.SetFeedrate(f));
				machine.runCommand(new replicatorg.drivers.commands.QueuePoint(current));
			}
		}
	}

	public void mouseReleased(MouseEvent arg0) {
		if (continuousJogMode) {
			machine.stopMotion();
		}
	}
}
