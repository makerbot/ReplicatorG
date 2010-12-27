package replicatorg.app.ui.controlpanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.EnumMap;
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
import replicatorg.app.MachineController;
import replicatorg.drivers.Driver;
import replicatorg.drivers.RetryException;
import replicatorg.machine.model.AxisId;
import replicatorg.util.Point5d;

public class JogPanel extends JPanel implements ActionListener
{
	protected double jogRate;

	protected Pattern jogPattern;

	protected String[] jogStrings = { "0.01mm", "0.05mm", "0.1mm", "0.5mm",
			"1mm", "5mm", "10mm", "20mm", "50mm" };

	protected final Point5d feedrate;

	protected EnumMap<AxisId,JTextField> positionFields = new EnumMap<AxisId,JTextField>(AxisId.class);

	protected MachineController machine;
	protected Driver driver;

	/**
	 * Create a jog-style button with the given name and tooltip.  By default, the
	 * action name is the same as the text of the button.  The button will emit an
	 * action event to the jog panel when it is clicked.
	 * @param text the text to display on the button.
	 * @param tooltip the text to display when the mouse hovers over the button.
	 * @return the generated button.
	 */
	protected JButton createJogButton(String text, String tooltip) {
		final int buttonSize = 60;
		JButton b = new JButton(text);
		b.setToolTipText(tooltip);
		b.setMaximumSize(new Dimension(buttonSize, buttonSize));
		b.setPreferredSize(new Dimension(buttonSize, buttonSize));
		b.setMinimumSize(new Dimension(buttonSize, buttonSize));
		b.addActionListener(this);
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
	protected JButton createJogButton(String text, String tooltip, String action) {
		JButton button = createJogButton(text,tooltip);
		button.setActionCommand(action);
		return button;
	}

	/**
	 * Create a text field for dynamic data display 
	 */
	protected JTextField createDisplayField() {
		JTextField tf = new JTextField();
		tf.setEnabled(false);
		return tf;
	}
	
	private JPanel buildPositionPanel() {
		// create our position panel
		JPanel positionPanel = new JPanel(new MigLayout("flowy,fillx"));
		// our label
		positionPanel.add(new JLabel("Jog Size"),"growx");
		// create our jog size dropdown
		JComboBox jogList = new JComboBox(jogStrings);
		jogList.setSelectedIndex(6);
		jogList.setActionCommand("jog size");
		jogList.addActionListener(this);
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
			field.setMinimumSize(new Dimension(75, 25));
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

	public JogPanel(MachineController machine) {
		feedrate = new Point5d();
		this.machine = machine;
		this.driver = machine.getDriver();
		Set<AxisId> axes = machine.getModel().getAvailableAxes();
		
		setLayout(new MigLayout());
		
		// compile our regexes
		jogRate = 10.0;
		jogPattern = Pattern.compile("([.0-9]+)");

		JButton xPlusButton = createJogButton("X+", "Jog X axis in positive direction");
		JButton xMinusButton = createJogButton("X-", "Jog X axis in negative direction");
		JButton yPlusButton = createJogButton("Y+", "Jog Y axis in positive direction");
		JButton yMinusButton = createJogButton("Y-", "Jog Y axis in negative direction");
		JButton zPlusButton = createJogButton("Z+", "Jog Z axis in positive direction");
		JButton zMinusButton = createJogButton("Z-", "Jog Z axis in negative direction");
		JButton zeroButton = createJogButton("<html><center>Set<br/>zero","Mark Current Position as Zero (0,0,0)","Zero");
		JButton panicButton = createJogButton("","Emergency stop","Stop");
		panicButton.setIcon(new ImageIcon(Base.getImage("images/button-panic.png",this)));

		JPanel xyzPanel = new JPanel(new MigLayout("","[]0[]","[]0[]"));
		JPanel xyPanel = new JPanel(new MigLayout("","[]0[]0[]","[]0[]0[]"));
        //xyzPanel.add(zCenterButton, );
		xyPanel.add(yPlusButton, "skip 1,gap 0 0 0 0");
		xyPanel.add(panicButton, "gap 0 0 0 0,wrap");
		xyPanel.add(xMinusButton, "gap 0 0 0 0");
		xyPanel.add(zeroButton,"gap 0 0 0 0");
		xyPanel.add(xPlusButton,"gap 0 0 0 0,wrap");
		xyPanel.add(yMinusButton, "skip 1,wrap,gap 0 0 0 0");
		xyzPanel.add(xyPanel);
		xyzPanel.add(zPlusButton, "split 2,flowy,gap 0 0 0 0");
		xyzPanel.add(zMinusButton);


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
		add(xyzPanel);
		add(buildPositionPanel(),"growx,wrap");
		add(feedratePanel,"growx,spanx");

		// add jog panel border and stuff.
		setBorder(BorderFactory.createTitledBorder("Jog Controls"));
	
	}
	

	DecimalFormat positionFormatter = new DecimalFormat("###.#");

	synchronized public void updateStatus() {
		Point5d current = driver.getCurrentPosition();

		for (AxisId axis : machine.getModel().getAvailableAxes()) {
			double v = current.axis(axis);
			positionFields.get(axis).setText(positionFormatter.format(v));
		}
	}

	Pattern jogActionParser = Pattern.compile("([XYZAB])([\\+\\-])");
	Pattern centerActionParser = Pattern.compile("Center ([XYZAB])");
	
	public void actionPerformed(ActionEvent e) {
		Point5d current = driver.getCurrentPosition();
		String s = e.getActionCommand();

		try {
			Matcher jogMatch = jogActionParser.matcher(s);
			Matcher centerMatch = centerActionParser.matcher(s);
			if (jogMatch.matches()) {
				AxisId axis = AxisId.valueOf(jogMatch.group(1));
				boolean positive = jogMatch.group(2).equals("+");
				current.setAxis(axis, current.axis(axis) + (positive?jogRate:-jogRate));
				double f = feedrate.axis(axis);
				// Exception: XY feedrate is assumed to be X feedrate (symmetrical)
				if (axis.equals(AxisId.Y)) { f = feedrate.axis(AxisId.X); }
				driver.setFeedrate(f);
				driver.queuePoint(current);
			} else if (s.equals("Stop")) {
				this.driver.stop();
				// FIXME: If we reenable the control panel while printing, 
				// we should check this, call this.machine.stop(),
				// plus communicate this action back to the main window
			} else if (centerMatch.matches()) {
				AxisId axis = AxisId.valueOf(centerMatch.group(1));
				current.setAxis(axis, 0);
				double f = feedrate.axis(axis);
				// Exception: XY feedrate is assumed to be X feedrate (symmetrical)
				if (axis.equals(AxisId.Y)) { f = feedrate.axis(AxisId.X); }
				driver.setFeedrate(f);
				driver.queuePoint(current);
			} else if (s.equals("Zero")) {
				// "Zero" tells the machine to calibrate its
				// current position as zero, not to move to its
				// currently-set zero position.
				driver.setCurrentPosition(new Point5d());
			}
			// get our new jog rate
			else if (s.equals("jog size")) {
				JComboBox cb = (JComboBox) e.getSource();
				String jogText = (String) cb.getSelectedItem();

				// look for a decimal number
				Matcher jogMatcher = jogPattern.matcher(jogText);
				if (jogMatcher.find())
					jogRate = Double.parseDouble(jogMatcher.group(1));

				// TODO: save this back to our preferences file.

				// System.out.println("jog rate: " + jogRate);
			} else {
				Base.logger.warning("Unknown Action Event: " + s);
			}
		} catch (RetryException e1) {
			Base.logger.severe("Could not execute command; machine busy.");
		}
	}
}
