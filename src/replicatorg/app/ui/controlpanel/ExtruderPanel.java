package replicatorg.app.ui.controlpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeTableXYDataset;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.commands.DriverCommand.AxialDirection;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.model.ToolModel;
import replicatorg.app.ui.CallbackTextField;

public class ExtruderPanel extends JPanel implements FocusListener, ActionListener, ItemListener {
	private ToolModel toolModel;
	private MachineInterface machine;

	public ToolModel getTool() { return toolModel; }
	
	protected JTextField currentTempField;
	
	protected JTextField platformCurrentTempField;

	protected double targetTemperature;
	protected double targetPlatformTemperature;
	
	final private static Color targetColor = Color.BLUE;
	final private static Color measuredColor = Color.RED;
	final private static Color targetPlatformColor = Color.YELLOW;
	final private static Color measuredPlatformColor = Color.WHITE;
	
	long startMillis = System.currentTimeMillis();

	private TimeTableXYDataset measuredDataset = new TimeTableXYDataset();
	private TimeTableXYDataset targetDataset = new TimeTableXYDataset();
	private TimeTableXYDataset measuredPlatformDataset = new TimeTableXYDataset();
	private TimeTableXYDataset targetPlatformDataset = new TimeTableXYDataset();

	protected Pattern extrudeTimePattern;
	
	protected String[] extrudeTimeStrings = { /* "Continuous Move", */ "1s", "2s", "5s", "10s", "30s", "60s", "300s" };
	
	protected boolean continuousJogMode = false;
	protected long extrudeTime;
	private final String EXTRUDE_TIME_PREF_NAME = "extruderpanel.extrudetime";
	
//	protected Driver driver;
	
	
	/**
	 * Make a label with an icon indicating its color on the graph.
	 * @param text The text of the label
	 * @param c The color of the matching line on the graph
	 * @return the generated label
	 */
	private JLabel makeKeyLabel(String text, Color c) {
		BufferedImage image = new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		g.setColor(c);
		g.fillRect(0,0,10,10);
		//image.getGraphics().fillRect(0,0,10,10);
		Icon icon = new ImageIcon(image);
		return new JLabel(text,icon,SwingConstants.LEFT);
	}

	public ChartPanel makeChart(ToolModel t) {
		JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, 
				measuredDataset, PlotOrientation.VERTICAL, 
				false, false, false);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(null);
		XYPlot plot = chart.getXYPlot();
		ValueAxis axis = plot.getDomainAxis();
		axis.setLowerMargin(0);
		axis.setFixedAutoRange(3L*60L*1000L); // auto range to three minutes
		TickUnits unitSource = new TickUnits();
		unitSource.add(new NumberTickUnit(60L*1000L)); // minutes
		unitSource.add(new NumberTickUnit(1L*1000L)); // seconds
		axis.setStandardTickUnits(unitSource);
		axis.setTickLabelsVisible(false); // We don't need to see the millisecond count
		axis = plot.getRangeAxis();
		axis.setRange(0,300); // set termperature range from 0 to 300 degrees C so you can see overshoots 
		// Tweak L&F of chart
		//((XYAreaRenderer)plot.getRenderer()).setOutline(true);
		XYStepRenderer renderer = new XYStepRenderer();
		plot.setDataset(1, targetDataset);
		plot.setRenderer(1, renderer);
		plot.getRenderer(1).setSeriesPaint(0, targetColor);
		plot.getRenderer(0).setSeriesPaint(0, measuredColor);
		if (t.hasHeatedPlatform()) {
			plot.setDataset(2,measuredPlatformDataset);
			plot.setRenderer(2, new XYLineAndShapeRenderer(true,false)); 
			plot.getRenderer(2).setSeriesPaint(0, measuredPlatformColor);
			plot.setDataset(3,targetPlatformDataset);
			plot.setRenderer(3, new XYStepRenderer()); 
			plot.getRenderer(3).setSeriesPaint(0, targetPlatformColor);
		}
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(400,160));
		chartPanel.setOpaque(false);
		return chartPanel;
	}

	private final Dimension labelMinimumSize = new Dimension(175, 25);

	private JLabel makeLabel(String text) {
		JLabel label = new JLabel();
		label.setText(text);
		label.setMinimumSize(labelMinimumSize);
		label.setMaximumSize(labelMinimumSize);
		label.setPreferredSize(labelMinimumSize);
		label.setHorizontalAlignment(JLabel.LEFT);
		return label;
	}

	
	private void setExtrudeTime(String mode) {
		if ("Continuous Jog".equals(mode)) {
			continuousJogMode = true;
			extrudeTime = 0;
		} else {
			// If we were in continuous jog mode, send a stop to be safe
			if (continuousJogMode) {
				this.machine.stopMotion();			
			}
			continuousJogMode = false;
			Matcher jogMatcher = extrudeTimePattern.matcher(mode);
			if (jogMatcher.find())
				extrudeTime = Long.parseLong(jogMatcher.group(1));
		}
		if (mode != null && mode.length() > 0) {
			Base.preferences.put(EXTRUDE_TIME_PREF_NAME,mode);
		}		
	}
	
	public ExtruderPanel(MachineInterface machine, ToolModel t) {
		this.machine = machine;
		this.toolModel = t;
		
		int textBoxWidth = 75;
		Dimension panelSize = new Dimension(420, 30);
//		driver = machine2.getDriver();

		extrudeTimePattern = Pattern.compile("([.0-9]+)");
		
		// create our initial panel
		setLayout(new MigLayout());
		// create our motor options
		if (t.hasMotor()) {
			// Due to current implementation issues, we need to send the PWM
			// before the RPM for a stepper motor. Thus we display both controls in these
			// cases. This shouldn't be necessary for a Gen4 stepper extruder. (it's not!)
			if ((t.getMotorStepperAxis() == null) && !(t.motorHasEncoder() || t.motorIsStepper())) {
				// our motor speed vars
				JLabel label = makeLabel("Motor Speed (PWM)");
				JTextField field = new CallbackTextField(this, "handleTextField", "motor-speed-pwm", 9);
				field.setText(Integer.toString(machine.getDriverQueryInterface().getMotorSpeedPWM()));
				add(label);
				add(field,"wrap");
			}

			if (t.motorHasEncoder() || t.motorIsStepper()) {
				// our motor speed vars
				JLabel label = makeLabel("Motor Speed (RPM)");
				JTextField field = new CallbackTextField(this, "handleTextField", "motor-speed", 9);
				field.setText(Double.toString(machine.getDriverQueryInterface().getMotorRPM()));
				add(label);
				add(field,"wrap");

				if (this.toolModel.getMotorStepperAxis() != null) {
					label = makeLabel("Extrude duration");
				
					JComboBox timeList = new JComboBox(extrudeTimeStrings);
					timeList.setSelectedItem(Base.preferences.get(EXTRUDE_TIME_PREF_NAME,"5s"));
					timeList.setActionCommand("Extrude-duration");
					timeList.addActionListener(this);
					setExtrudeTime((String)timeList.getSelectedItem());
					add(label);
					add(timeList,"wrap");
				}
			}
			// create our motor options
			JLabel motorEnabledLabel = makeLabel("Motor Control");
			
			if (t.motorHasEncoder() || (t.motorIsStepper() && this.toolModel.getMotorStepperAxis() != null)) {
				JButton motorReverseButton = new JButton("reverse");
				motorReverseButton.setActionCommand("reverse");
				motorReverseButton.addActionListener(this);

				JButton motorStoppedButton = new JButton("stop");
				motorStoppedButton.setActionCommand("stop");
				motorStoppedButton.addActionListener(this);

				JButton motorForwardButton = new JButton("forward");
				motorForwardButton.setActionCommand("forward");
				motorForwardButton.addActionListener(this);

				ButtonGroup motorControl = new ButtonGroup();
				motorControl.add(motorReverseButton);
				motorControl.add(motorStoppedButton);
				motorControl.add(motorForwardButton);

				// add components in.
				add(motorEnabledLabel,"split,spanx");
				add(motorReverseButton);
				add(motorStoppedButton);
				add(motorForwardButton,"wrap");
			}
			else {
				JRadioButton motorReverseButton = new JRadioButton("reverse");
				motorReverseButton.setName("motor-reverse");
				motorReverseButton.setActionCommand("motor-reverse");
				motorReverseButton.addItemListener(this);

				JRadioButton motorStoppedButton = new JRadioButton("stop");
				motorStoppedButton.setName("motor-stop");
				motorStoppedButton.setActionCommand("motor-stop");
				motorStoppedButton.addItemListener(this);

				JRadioButton motorForwardButton = new JRadioButton("forward");
				motorForwardButton.setName("motor-forward");
				motorForwardButton.setActionCommand("motor-forward");
				motorForwardButton.addItemListener(this);

				ButtonGroup motorControl = new ButtonGroup();
				motorControl.add(motorReverseButton);
				motorControl.add(motorStoppedButton);
				motorControl.add(motorForwardButton);

				// add components in.
				add(motorEnabledLabel,"split,spanx");
				add(motorReverseButton);
				add(motorStoppedButton);
				add(motorForwardButton,"wrap");		
			}

		}

		// our temperature fields
		if (t.hasHeater()) {
			JLabel targetTempLabel = makeKeyLabel("Target Temperature (C)",targetColor);
			JTextField targetTempField = new CallbackTextField(this, "handleTextField", "target-temp", 9);
			targetTemperature = machine.getDriverQueryInterface().getTemperatureSetting();
			targetTempField.setText(Double.toString(targetTemperature));

			JLabel currentTempLabel = makeKeyLabel("Current Temperature (C)",measuredColor);
			currentTempField = new JTextField("",9);
			currentTempField.setEnabled(false);

			add(targetTempLabel);
			add(targetTempField,"wrap");
			add(currentTempLabel);
			add(currentTempField,"wrap");
		}

		// our heated platform fields
		if (t.hasHeatedPlatform()) {
			JLabel targetTempLabel = makeKeyLabel("Platform Target Temp (C)",targetPlatformColor);
			JTextField targetTempField = new CallbackTextField(this, "handleTextField", "platform-target-temp", 9);
			targetPlatformTemperature = machine.getDriverQueryInterface().getPlatformTemperatureSetting();
			targetTempField.setText(Double.toString(targetPlatformTemperature));

			JLabel currentTempLabel = makeKeyLabel("Platform Current Temp (C)",measuredPlatformColor);
			platformCurrentTempField = new JTextField("",9);
			platformCurrentTempField.setEnabled(false);

			add(targetTempLabel);
			add(targetTempField,"wrap");
			add(currentTempLabel);
			add(platformCurrentTempField,"wrap");
			
		}

		if (t.hasHeater() || t.hasHeatedPlatform()) {
			add(new JLabel("Temperature Chart"),"growx,spanx,wrap");
			add(makeChart(t),"growx,spanx,wrap");
		}

		// flood coolant controls
		if (t.hasFloodCoolant()) {
			JLabel floodCoolantLabel = makeLabel("Flood Coolant");

			JCheckBox floodCoolantCheck = new JCheckBox("enable");
			floodCoolantCheck.setName("flood-coolant");
			floodCoolantCheck.addItemListener(this);

			add(floodCoolantLabel);
			add(floodCoolantCheck,"wrap");
		}

		// mist coolant controls
		if (t.hasMistCoolant()) {
			JLabel mistCoolantLabel = makeLabel("Mist Coolant");

			JCheckBox mistCoolantCheck = new JCheckBox("enable");
			mistCoolantCheck.setName("mist-coolant");
			mistCoolantCheck.addItemListener(this);

			add(mistCoolantLabel);
			add(mistCoolantCheck,"wrap");
		}

		// cooling fan controls
		if (t.hasFan()) {
			String fanString = "Cooling Fan";
			String enableString = "enable";
			Element xml = findMappingNode(t.getXml(),"fan");
			if (xml != null) {
				fanString = xml.getAttribute("name");
				enableString = xml.getAttribute("actuated");
			}
			JLabel fanLabel = makeLabel(fanString);

			JCheckBox fanCheck = new JCheckBox(enableString);
			fanCheck.setName("fan-check");
			fanCheck.addItemListener(this);

			add(fanLabel);
			add(fanCheck,"wrap");
		}

		// cooling fan controls
		if (t.hasAutomatedPlatform()) {
			String abpString = "Build platform belt";
			String enableString = "enable";
			JLabel abpLabel = makeLabel(abpString);

			JCheckBox abpCheck = new JCheckBox(enableString);
			abpCheck.setName("abp-check");
			abpCheck.addItemListener(this);

			add(abpLabel);
			add(abpCheck,"wrap");
		}

		// valve controls
		if (t.hasValve()) {
			String valveString = "Valve";
			String enableString = "open";

			Element xml = findMappingNode(t.getXml(),"valve");
			if (xml != null) {
				valveString = xml.getAttribute("name");
				enableString = xml.getAttribute("actuated");
			}
			
			JLabel valveLabel = makeLabel(valveString);

			JCheckBox valveCheck = new JCheckBox(enableString);
			valveCheck.setName("valve-check");
			valveCheck.addItemListener(this);

			add(valveLabel);
			add(valveCheck,"wrap");
		}

		// valve controls
		if (t.hasCollet()) {
			JLabel colletLabel = makeLabel("Collet");

			JCheckBox colletCheck = new JCheckBox("open");
			colletCheck.setName("collet-check");
			colletCheck.addItemListener(this);

			JPanel colletPanel = new JPanel();
			colletPanel.setLayout(new BoxLayout(colletPanel,
					BoxLayout.LINE_AXIS));
			colletPanel.setMaximumSize(panelSize);
			colletPanel.setMinimumSize(panelSize);
			colletPanel.setPreferredSize(panelSize);

			add(colletLabel);
			add(colletCheck,"wrap");
		}
	}

	private Element findMappingNode(Node xml,String portName) {
		// scan the remapping nodes.
		NodeList children = xml.getChildNodes();
		for (int j=0; j<children.getLength(); j++) {
			Node child = children.item(j);
			if (child.getNodeName().equals("remap")) {
				Element e = (Element)child;
				if (e.getAttribute("port").equals(portName)) {
					return e;
				}
			}
		}
		return null;
	}

	public void updateStatus() {
		
		Second second = new Second(new Date(System.currentTimeMillis() - startMillis));
		
		if (machine.getModel().currentTool() == toolModel && toolModel.hasHeater()) {
			double temperature = machine.getDriverQueryInterface().getTemperature();
			updateTemperature(second, temperature);
		}
		if (machine.getModel().currentTool() == toolModel && toolModel.hasHeatedPlatform()) {
			double temperature = machine.getDriverQueryInterface().getPlatformTemperature();
			updatePlatformTemperature(second, temperature);
		}
	}
	
	synchronized public void updateTemperature(Second second, double temperature)
	{
		currentTempField.setText(Double.toString(temperature));
		measuredDataset.add(second, temperature,"a");
		targetDataset.add(second, targetTemperature,"a");
	}

	synchronized public void updatePlatformTemperature(Second second, double temperature)
	{
		platformCurrentTempField.setText(Double.toString(temperature));
		measuredPlatformDataset.add(second, temperature,"a");
		targetPlatformDataset.add(second, targetPlatformTemperature,"a");
	}

	public void focusGained(FocusEvent e) {
	}

	public void focusLost(FocusEvent e) {
		JTextField source = (JTextField) e.getSource();
		try {
			handleChangedTextField(source);
		} catch (RetryException e1) {
			Base.logger.severe("Could not execute command; machine busy.");
		}
	}

	//
	// Check the temperature range and insure that the target is within bounds.  If not,
	// query the user to see if they want to make an exception.
	// agm: I've removed the "set this as the new limit" option.  This should be set in the
	// preferences or some other hard-to-reach place.  Accidentally clicking a button
	// to set a potentially dangerous limit would suck.
	// @return Double.MIN_VALUE if cancelled; the target temperature otherwise.
	//
	// NOTE: copy of this in preferences window
	private double confirmTemperature(double target, String limitPrefName, double defaultLimit) {
		double limit = Base.preferences.getDouble("temperature.acceptedLimit", defaultLimit);
		if (target > limit){
			// Temperature warning dialog!
			int n = JOptionPane.showConfirmDialog(this,
					"<html>Setting the temperature to <b>" + Double.toString(target) + "\u00b0C</b> may<br>"+
					"involve health and/or safety risks or cause damage to your machine!<br>"+
					"The maximum recommended temperature is <b>"+Double.toString(limit)+"</b>.<br>"+
					"Do you accept the risk and still want to set this temperature?",
					"Danger",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (n == JOptionPane.YES_OPTION) {
				return target;
			} else if (n == JOptionPane.NO_OPTION) {
				return Double.MIN_VALUE;
			} else { // Cancel or whatnot
				return Double.MIN_VALUE;
			}
		}  else {
			return target;
		}
	}
	
	public void handleChangedTextField(JTextField source) throws RetryException
	{
		String name = source.getName();
		if (source.getText().length() > 0) {
			double target;
			try {
				NumberFormat nf = Base.getLocalFormat();
				target = nf.parse(source.getText()).doubleValue();
			} catch (ParseException pe) {
				Base.logger.log(Level.WARNING,"Could not parse value!",pe);
				JOptionPane.showMessageDialog(this, "Error parsing value: "+pe.getMessage()+"\nPlease try again.", "Could not parse value", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (name.equals("target-temp") || name.equals("platform-target-temp")) {
				
				if(name.equals("target-temp")) {
					target = confirmTemperature(target,"temperature.acceptedLimit",260.0);
					if (target == Double.MIN_VALUE) {
						return;
					}
					machine.runCommand(new replicatorg.drivers.commands.SetTemperature(target));
					targetTemperature = target;
				} else {
					target = confirmTemperature(target,"temperature.acceptedLimit.bed",130.0);
					if (target == Double.MIN_VALUE) {
						return;
					}
					machine.runCommand(new replicatorg.drivers.commands.SetPlatformTemperature(target));
					targetPlatformTemperature = target;
				}
				// This gives some feedback by adding .0 if it wasn't typed.
				source.setText(Double.toString(target));
			} else if (name.equals("motor-speed")) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedRPM(target));
			} else if (name.equals("motor-speed-pwm")) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedPWM((int)target));
			} else {
				Base.logger.warning("Unhandled text field: "+name);
			}
		}
	}


	public void itemStateChanged(ItemEvent e) {
		Component source = (Component) e.getItemSelectable();
		String name = source.getName();
		if (e.getStateChange() == ItemEvent.SELECTED) {
			/* Handle DC extruder commands */
			if (name.equals("motor-forward")) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.CLOCKWISE));
				// TODO: Hack to support RepRap/Ultimaker- always re-send RPM
				if (toolModel.motorHasEncoder() || toolModel.motorIsStepper()) {
					machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedRPM(machine.getDriver().getMotorRPM()));
				}
				machine.runCommand(new replicatorg.drivers.commands.EnableMotor());
			} else if (name.equals("motor-reverse")) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.COUNTERCLOCKWISE));
				// TODO: Hack to support RepRap/Ultimaker- always re-send RPM
				if (toolModel.motorHasEncoder() || toolModel.motorIsStepper()) {
					machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedRPM(machine.getDriver().getMotorRPM()));
				}
				machine.runCommand(new replicatorg.drivers.commands.EnableMotor());
			} else if (name.equals("motor-stop")) {
				machine.runCommand(new replicatorg.drivers.commands.DisableMotor());
			}
			else if (name.equals("spindle-enabled"))
				machine.runCommand(new replicatorg.drivers.commands.EnableSpindle());
			else if (name.equals("flood-coolant"))
				machine.runCommand(new replicatorg.drivers.commands.EnableFloodCoolant());
			else if (name.equals("mist-coolant"))
				machine.runCommand(new replicatorg.drivers.commands.EnableMistCoolant());
			else if (name.equals("fan-check"))
				machine.runCommand(new replicatorg.drivers.commands.EnableFan());
			else if (name.equals("abp-check")) {
				// TODO: Debugging. Run both!
				machine.runCommand(new replicatorg.drivers.commands.ToggleAutomatedBuildPlatform(true));
				machine.runCommand(new replicatorg.drivers.commands.EnableFan());
			}
			else if (name.equals("valve-check"))
				machine.runCommand(new replicatorg.drivers.commands.OpenValve());
			else if (name.equals("collet-check"))
				machine.runCommand(new replicatorg.drivers.commands.OpenCollet());
			else
				Base.logger.warning("checkbox selected: " + source.getName());
		} else {
			if (name.equals("motor-enabled"))
				machine.runCommand(new replicatorg.drivers.commands.DisableMotor());
			else if (name.equals("spindle-enabled"))
				machine.runCommand(new replicatorg.drivers.commands.DisableSpindle());
			else if (name.equals("flood-coolant"))
				machine.runCommand(new replicatorg.drivers.commands.DisableFloodCoolant());
			else if (name.equals("mist-coolant"))
				machine.runCommand(new replicatorg.drivers.commands.DisableMistCoolant());
			else if (name.equals("fan-check"))
				machine.runCommand(new replicatorg.drivers.commands.DisableFan());
			else if (name.equals("abp-check")) {
				// TODO: Debugging. Run both!
				machine.runCommand(new replicatorg.drivers.commands.ToggleAutomatedBuildPlatform(false));
				machine.runCommand(new replicatorg.drivers.commands.DisableFan());
			}
			else if (name.equals("valve-check"))
				machine.runCommand(new replicatorg.drivers.commands.CloseValve());
			else if (name.equals("collet-check"))
				machine.runCommand(new replicatorg.drivers.commands.CloseCollet());
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String s = e.getActionCommand();
		
		if(s.equals("handleTextField"))
		{
			JTextField source = (JTextField) e.getSource();
			try {
				handleChangedTextField(source);
			} catch (RetryException e1) {
				Base.logger.severe("Could not execute command; machine busy.");
			}
			source.selectAll();
		}
		else if (s.equals("Extrude-duration")) {
			JComboBox cb = (JComboBox) e.getSource();
			String timeText = (String) cb.getSelectedItem();
			setExtrudeTime(timeText);
		}
		/* Handle stepper extruder commands */
		if (s.equals("forward")) {
			if (this.toolModel.getMotorStepperAxis() != null) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.CLOCKWISE));
				// Reverted to one single command for RepRap5D driver
				if (machine.getDriver().getDriverName().equals("RepRap5D")) {
					machine.runCommand(new replicatorg.drivers.commands.EnableMotor(extrudeTime*1000));
				} else {
					machine.runCommand(new replicatorg.drivers.commands.EnableMotor());
					machine.runCommand(new replicatorg.drivers.commands.Delay(extrudeTime*1000));
					machine.runCommand(new replicatorg.drivers.commands.DisableMotor());
				}
			}
		} else if (s.equals("reverse")) {
			if (this.toolModel.getMotorStepperAxis() != null) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.COUNTERCLOCKWISE));
				// Reverted to one single command for RepRap5D driver
				if (machine.getDriver().getDriverName().equals("RepRap5D")) {
					machine.runCommand(new replicatorg.drivers.commands.EnableMotor(extrudeTime*1000));
				} else {
					machine.runCommand(new replicatorg.drivers.commands.EnableMotor());
					machine.runCommand(new replicatorg.drivers.commands.Delay(extrudeTime*1000));
					machine.runCommand(new replicatorg.drivers.commands.DisableMotor());
				}
			}
		} else if (s.equals("stop")) {
			machine.runCommand(new replicatorg.drivers.commands.DisableMotor());
			
			if (this.toolModel.getMotorStepperAxis() != null) {
				machine.stopMotion();
			}
		}
	}


}
