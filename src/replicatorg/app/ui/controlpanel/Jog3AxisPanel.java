package replicatorg.app.ui.controlpanel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
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
import replicatorg.util.Point5d;

public class Jog3AxisPanel extends JPanel implements ActionListener, ChangeListener, FocusListener
{
	protected double jogRate;

	protected Pattern jogPattern;

	protected String[] jogStrings = { "0.01mm", "0.05mm", "0.1mm", "0.5mm",
			"1mm", "5mm", "10mm", "20mm", "50mm" };

	protected JSlider xyFeedrateSlider;
	protected JTextField xyFeedrateValue;

	protected JSlider zFeedrateSlider;
	protected JTextField zFeedrateValue;

	protected JTextField xPosField;
	protected JTextField yPosField;
	protected JTextField zPosField;

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

	public Jog3AxisPanel(MachineController machine) {
		this.machine = machine;
		this.driver = machine.getDriver();
		setLayout(new MigLayout());
		
		// compile our regexes
		jogRate = 10.0;
		jogPattern = Pattern.compile("([.0-9]+)");

		JButton xPlusButton = createJogButton("X+", "Jog X axis in positive direction");
		JButton xMinusButton = createJogButton("X-", "Jog X axis in negative direction");
		JButton xCenterButton = createJogButton("<html><center>Center<br/>X", "Jog X axis to the origin","Center X");
		JButton yPlusButton = createJogButton("Y+", "Jog Y axis in positive direction");
		JButton yMinusButton = createJogButton("Y-", "Jog Y axis in negative direction");
		JButton yCenterButton = createJogButton("<html><center>Center<br/>Y", "Jog Y axis to the origin","Center Y");
		JButton zPlusButton = createJogButton("Z+", "Jog Z axis in positive direction");
		JButton zMinusButton = createJogButton("Z-", "Jog Z axis in negative direction");
		JButton zCenterButton = createJogButton("<html><center>Center<br/>Z", "Jog Z axis to the origin","Center Z");
		JButton zeroButton = createJogButton("<html><center>Set<br/>zero","Mark Current Position as Zero (0,0,0)","Zero");

		JPanel xyzPanel = new JPanel(new MigLayout("","[]0[]","[]0[]"));
        xyzPanel.add(zCenterButton, "split 3,flowy,gap 0 0 0 0");
		xyzPanel.add(xMinusButton, "gap 0 0 0 0");
        xyzPanel.add(yCenterButton);
		xyzPanel.add(yPlusButton, "split 3,flowy,gap 0 0 0 0");
		xyzPanel.add(zeroButton,"gap 0 0 0 0");
		xyzPanel.add(yMinusButton);
		xyzPanel.add(xPlusButton,"split 2, flowy, aligny bottom, gap 0 0 0 0, gapafter 10");
        xyzPanel.add(xCenterButton);
		xyzPanel.add(zPlusButton, "split 2,flowy,gap 0 0 0 0");
		xyzPanel.add(zMinusButton);

		// create our position panel
		JPanel positionPanel = new JPanel(new MigLayout("flowy"));
		// our label
		positionPanel.add(new JLabel("Jog Size"));
		// create our jog size dropdown
		JComboBox jogList = new JComboBox(jogStrings);
		jogList.setSelectedIndex(6);
		jogList.setActionCommand("jog size");
		jogList.addActionListener(this);
		positionPanel.add(jogList,"growx");
		
		// our position text boxes
		xPosField = createDisplayField();
		yPosField = createDisplayField();
		zPosField = createDisplayField();

		positionPanel.add(new JLabel("X Position"));
		positionPanel.add(xPosField,"growx");
		positionPanel.add(new JLabel("Y Position"));
		positionPanel.add(yPosField,"growx");
		positionPanel.add(new JLabel("Z Position"));
		positionPanel.add(zPosField,"growx");

		// create the xyfeedrate panel
		JPanel feedratePanel = new JPanel(new MigLayout());

		int maxXYFeedrate = (int) Math.min(machine.getModel().getMaximumFeedrates().x(), 
				machine.getModel().getMaximumFeedrates().y());
		int currentXYFeedrate = Math.min(maxXYFeedrate, Base.preferences
				.getInt("controlpanel.feedrate.xy",480));
		xyFeedrateSlider = new JSlider(JSlider.HORIZONTAL, 1, maxXYFeedrate,
				currentXYFeedrate);
		xyFeedrateSlider.setMajorTickSpacing(1000);
		xyFeedrateSlider.setMinorTickSpacing(100);
		xyFeedrateSlider.setName("xy-feedrate-slider");
		xyFeedrateSlider.addChangeListener(this);

		// our display box
		xyFeedrateValue = new JTextField();
		xyFeedrateValue.setMinimumSize(new Dimension(75, 25));
		xyFeedrateValue.setEnabled(true);
		xyFeedrateValue.setName("xy-feedrate-value");
		xyFeedrateValue.setText(Integer.toString(xyFeedrateSlider.getValue()));
		xyFeedrateValue.addFocusListener(this);
		xyFeedrateValue.setActionCommand("handleTextfield");
		xyFeedrateValue.addActionListener(this);


		// create our z slider
		int maxZFeedrate = (int) machine.getModel().getMaximumFeedrates().z();
		int currentZFeedrate = Math.min(maxZFeedrate, 
				Base.preferences.getInt("controlpanel.feedrate.z",480));
		zFeedrateSlider = new JSlider(JSlider.HORIZONTAL, 1, maxZFeedrate,
				currentZFeedrate);
		zFeedrateSlider.setMajorTickSpacing(10);
		zFeedrateSlider.setMinorTickSpacing(1);
		zFeedrateSlider.setName("z-feedrate-slider");
		zFeedrateSlider.addChangeListener(this);

		// our display box
		zFeedrateValue = new JTextField();
		zFeedrateValue.setMinimumSize(new Dimension(75, 25));
		zFeedrateValue.setEnabled(true);
		zFeedrateValue.setName("z-feedrate-value");
		zFeedrateValue.setText(Integer.toString(zFeedrateSlider.getValue()));
		zFeedrateValue.addFocusListener(this);
		zFeedrateValue.setActionCommand("handleTextfield");
		zFeedrateValue.addActionListener(this);

		feedratePanel.add(new JLabel("XY Feedrate (mm/min.)"));
		feedratePanel.add(xyFeedrateSlider,"growx");
		feedratePanel.add(xyFeedrateValue,"growx,wrap");
		feedratePanel.add(new JLabel("Z Feedrate (mm/min.)"));
		feedratePanel.add(zFeedrateSlider,"growx");
		feedratePanel.add(zFeedrateValue,"growx,wrap");

		// add it all to our jog panel
		add(xyzPanel);
		add(positionPanel,"growx,wrap");
		add(feedratePanel,"growx,spanx");

		// add jog panel border and stuff.
		setBorder(BorderFactory.createTitledBorder("Jog Controls"));
	
	}
	

	DecimalFormat positionFormatter = new DecimalFormat("###.#");

	synchronized public void updateStatus() {
		Point5d current = driver.getCurrentPosition();

		xPosField.setText(positionFormatter.format(current.x()));
		yPosField.setText(positionFormatter.format(current.y()));
		zPosField.setText(positionFormatter.format(current.z()));		
	}

	public void handleChangedTextField(JTextField source)
	{
		String name = source.getName();

		if (source.getText().length() > 0) {
			if (name.equals("xy-feedrate-value")) {
				xyFeedrateSlider.setValue(Integer.parseInt(source.getText()));
			} else if (name.equals("z-feedrate-value")) {
				zFeedrateSlider.setValue(Integer.parseInt(source.getText()));
			} else {
				Base.logger.warning("Unhandled text field: "+name);
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		Point5d current = driver.getCurrentPosition();
		double xyFeedrate = xyFeedrateSlider.getValue();
		double zFeedrate = zFeedrateSlider.getValue();
		String s = e.getActionCommand();

		try {

		if(s.equals("handleTextfield"))
		{
			JTextField source = (JTextField) e.getSource();
			handleChangedTextField(source);
			source.selectAll();
		}

		if (s.equals("X+")) {
			current.setX(current.x() + jogRate);

			driver.setFeedrate(xyFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("X-")) {
			current.setX(current.x() - jogRate);

			driver.setFeedrate(xyFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("Y+")) {
			current.setY(current.y() + jogRate);

			driver.setFeedrate(xyFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("Y-")) {
			current.setY(current.y() - jogRate);

			driver.setFeedrate(xyFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("Z+")) {
			current.setZ(current.z() + jogRate);

			driver.setFeedrate(zFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("Z-")) {
			current.setZ(current.z() - jogRate);

			driver.setFeedrate(zFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("Center X")) {
			current.setX(0);

			driver.setFeedrate(xyFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("Center Y")) {
			current.setY(0);

			driver.setFeedrate(xyFeedrate);
			driver.queuePoint(current);
		} else if (s.equals("Center Z")) {
			current.setZ(0);

			driver.setFeedrate(zFeedrate);
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

	public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider) e.getSource();
		int feedrate = source.getValue();

		if (source.getName().equals("xy-feedrate-slider")) {
			xyFeedrateValue.setText(Integer.toString(feedrate));
		} else if (source.getName().equals("z-feedrate-slider")) {
			zFeedrateValue.setText(Integer.toString(feedrate));
		}
	}

	
	public void focusGained(FocusEvent e) {
	}

	public void focusLost(FocusEvent e) {
		JTextField source = (JTextField) e.getSource();
		handleChangedTextField(source);
	}
}
