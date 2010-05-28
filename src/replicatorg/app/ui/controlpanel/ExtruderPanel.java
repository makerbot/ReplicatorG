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
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
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
import replicatorg.app.MachineController;
import replicatorg.drivers.Driver;
import replicatorg.machine.model.ToolModel;

public class ExtruderPanel extends JPanel implements FocusListener, ActionListener, ItemListener {
	private ToolModel toolModel;
	private MachineController machine;

	protected JTextField currentTempField;
	
	protected JTextField platformCurrentTempField;

	protected double targetTemperature;
	protected double targetPlatformTemperature;
	
	final private static Color targetColor = Color.blue;
	final private static Color measuredColor = Color.BLUE;
	final private static Color targetPlatformColor = Color.red;
	final private static Color measuredPlatformColor = Color.RED;
	
	long startMillis = System.currentTimeMillis();

	private TimeTableXYDataset measuredDataset = new TimeTableXYDataset();
	private TimeTableXYDataset targetDataset = new TimeTableXYDataset();
	private TimeTableXYDataset measuredPlatformDataset = new TimeTableXYDataset();
	private TimeTableXYDataset targetPlatformDataset = new TimeTableXYDataset();

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
		JFreeChart chart = ChartFactory.createXYLineChart("Temperature vs Time", "Time (ms)", "Temperature (C)", 
				measuredDataset, PlotOrientation.VERTICAL, 
				false, false, false);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(null);
		XYPlot plot = chart.getXYPlot();
		ValueAxis axis = plot.getDomainAxis();
		axis.setLowerMargin(0);
		axis.setFixedAutoRange(3L*60L*1000L); // auto range to three minutes
		axis.setTickLabelsVisible(false); // let's not see the miliseconds count
		axis = plot.getRangeAxis();
		axis.setRange(0,260); // set temperature range from 0 to 260 degrees C 
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
		//chartPanel.setPreferredSize(new Dimension(400,200));
		chartPanel.setOpaque(false);
		return chartPanel;
	}
	
	public ExtruderPanel(MachineController machine, ToolModel t) {
		this.machine = machine;
		this.toolModel = t;
		
		int textBoxWidth = 75;
		Dimension labelMinimumSize = new Dimension(175, 25);
		Dimension panelSize = new Dimension(420, 30);
		Driver driver = machine.getDriver();

		// create our initial panel
		setLayout(new MigLayout());

		// cooling fan controls
		if (t.hasFan()) {
			String fanString = "Cooling Fan";
			String enableString = "enable";
			Element xml = findMappingNode(t.getXml(),"fan");
			if (xml != null) {
				fanString = xml.getAttribute("name");
				enableString = xml.getAttribute("actuated");
			}
			JLabel fanLabel = new JLabel(fanString);
			fanLabel.setMinimumSize(labelMinimumSize);
			fanLabel.setMaximumSize(labelMinimumSize);
			fanLabel.setPreferredSize(labelMinimumSize);
			fanLabel.setHorizontalAlignment(JLabel.LEFT);

			JCheckBox fanCheck = new JCheckBox(enableString);
			fanCheck.setName("fan-check");
			fanCheck.addItemListener(this);

			add(fanLabel);
			add(fanCheck,"span,wrap");
		}

		// create our motor options
		if (t.hasMotor()) {
			// Due to current implementation issues, we need to send the PWM
			// before the RPM
			// for a stepper motor. Thus we display both controls in these
			// cases.
			{
				// our motor speed vars
				JLabel label = new JLabel();
				JTextField field = new JTextField();
				label.setText("Motor Speed (PWM)");
				label.setMinimumSize(labelMinimumSize);
				label.setMaximumSize(labelMinimumSize);
				label.setPreferredSize(labelMinimumSize);
				label.setHorizontalAlignment(JLabel.LEFT);

				field.setMaximumSize(new Dimension(textBoxWidth, 25));
				field.setMinimumSize(new Dimension(textBoxWidth, 25));
				field.setPreferredSize(new Dimension(textBoxWidth, 25));
				field.setName("motor-speed-pwm");
				field.addFocusListener(this);
				field.setActionCommand("handleTextfield");
				field.addActionListener(this);

				add(label);
				add(field);
			}

			if (t.motorHasEncoder() || t.motorIsStepper()) {
				JLabel label = new JLabel();
				JTextField field = new JTextField();
				// our motor speed vars
				label.setText("Motor Speed (RPM)");
				label.setMinimumSize(labelMinimumSize);
				label.setMaximumSize(labelMinimumSize);
				label.setPreferredSize(labelMinimumSize);
				label.setHorizontalAlignment(JLabel.LEFT);

				field.setMaximumSize(new Dimension(textBoxWidth, 25));
				field.setMinimumSize(new Dimension(textBoxWidth, 25));
				field.setPreferredSize(new Dimension(textBoxWidth, 25));
				field.setName("motor-speed");
				field.addFocusListener(this);
				field.setActionCommand("handleTextfield");
				field.addActionListener(this);

				add(label);
				add(field,"span");
			}
			// create our motor options
			JLabel motorEnabledLabel = new JLabel("Motor Control");
			motorEnabledLabel.setMinimumSize(labelMinimumSize);
			motorEnabledLabel.setMaximumSize(labelMinimumSize);
			motorEnabledLabel.setPreferredSize(labelMinimumSize);
			motorEnabledLabel.setHorizontalAlignment(JLabel.LEFT);

			JRadioButton motorReverseButton = new JRadioButton(" < ");
			motorReverseButton.setName("motor-reverse");
			motorReverseButton.addItemListener(this);

			JRadioButton motorStoppedButton = new JRadioButton(" || ");
			motorStoppedButton.setName("motor-stop");
			motorStoppedButton.addItemListener(this);

			JRadioButton motorForwardButton = new JRadioButton(" > ");
			motorForwardButton.setName("motor-forward");
			motorForwardButton.addItemListener(this);

			ButtonGroup motorControl = new ButtonGroup();
			motorControl.add(motorReverseButton);
			motorControl.add(motorStoppedButton);
			motorControl.add(motorForwardButton);


			// add components in.
			//add(motorEnabledLabel,"split,spanx");
			add(motorReverseButton,"split,spanx");
			add(motorStoppedButton);
			add(motorForwardButton,"span,wrap");
		}

		// our temperature fields
		if (t.hasHeater()) {
			JLabel targetTempLabel = makeKeyLabel("Nozzle Temp (C)",targetColor);
			JTextField targetTempField = new JTextField();
			targetTempField.setMaximumSize(new Dimension(textBoxWidth, 25));
			targetTempField.setMinimumSize(new Dimension(textBoxWidth, 25));
			targetTempField.setPreferredSize(new Dimension(textBoxWidth, 25));
			targetTempField.setName("target-temp");
			targetTempField.addFocusListener(this);
			targetTemperature = driver.getTemperatureSetting();
			targetTempField.setText(Double.toString(targetTemperature));
			targetTempField.setActionCommand("handleTextfield");
			targetTempField.addActionListener(this);

			JLabel currentTempLabel = makeKeyLabel("Current",measuredColor);
			currentTempField = new JTextField();
			currentTempField.setMaximumSize(new Dimension(textBoxWidth, 25));
			currentTempField.setMinimumSize(new Dimension(textBoxWidth, 25));
			currentTempField.setPreferredSize(new Dimension(textBoxWidth, 25));
			currentTempField.setEnabled(false);

			add(targetTempLabel);
			add(targetTempField);
			//add(currentTempLabel);
			add(currentTempField,"span,wrap");
		}

		// our heated platform fields
		if (t.hasHeatedPlatform()) {
			JLabel targetTempLabel = makeKeyLabel("Platform Temp (C)",targetPlatformColor);
			JTextField targetTempField = new JTextField();
			targetTempField.setMaximumSize(new Dimension(textBoxWidth, 25));
			targetTempField.setMinimumSize(new Dimension(textBoxWidth, 25));
			targetTempField.setPreferredSize(new Dimension(textBoxWidth, 25));
			targetTempField.setName("platform-target-temp");
			targetTempField.addFocusListener(this);
			double temperature = driver.getPlatformTemperatureSetting();
			targetPlatformTemperature = temperature;
			targetTempField.setText(Double.toString(temperature));
			targetTempField.setActionCommand("handleTextfield");
			targetTempField.addActionListener(this);

			JLabel currentTempLabel = makeKeyLabel("Current",measuredPlatformColor);

			platformCurrentTempField = new JTextField();
			platformCurrentTempField.setMaximumSize(new Dimension(textBoxWidth, 25));
			platformCurrentTempField.setMinimumSize(new Dimension(textBoxWidth, 25));
			platformCurrentTempField.setPreferredSize(new Dimension(textBoxWidth, 25));
			platformCurrentTempField.setEnabled(false);

			add(targetTempLabel);
			add(targetTempField);
			//add(currentTempLabel);
			add(platformCurrentTempField,"span,wrap");
			
		}

		if (t.hasHeater() || t.hasHeatedPlatform()) {
			//add(new JLabel("Temperature Chart"),"growx,spanx,wrap");
			add(makeChart(t),"span,flowx,grow");
		}

		// flood coolant controls
		if (t.hasFloodCoolant()) {
			JLabel floodCoolantLabel = new JLabel("Flood Coolant");
			floodCoolantLabel.setMinimumSize(labelMinimumSize);
			floodCoolantLabel.setMaximumSize(labelMinimumSize);
			floodCoolantLabel.setPreferredSize(labelMinimumSize);
			floodCoolantLabel.setHorizontalAlignment(JLabel.LEFT);

			JCheckBox floodCoolantCheck = new JCheckBox("enable");
			floodCoolantCheck.setName("flood-coolant");
			floodCoolantCheck.addItemListener(this);

			add(floodCoolantLabel);
			add(floodCoolantCheck,"wrap");
		}

		// mist coolant controls
		if (t.hasMistCoolant()) {
			JLabel mistCoolantLabel = new JLabel("Mist Coolant");
			mistCoolantLabel.setMinimumSize(labelMinimumSize);
			mistCoolantLabel.setMaximumSize(labelMinimumSize);
			mistCoolantLabel.setPreferredSize(labelMinimumSize);
			mistCoolantLabel.setHorizontalAlignment(JLabel.LEFT);

			JCheckBox mistCoolantCheck = new JCheckBox("enable");
			mistCoolantCheck.setName("mist-coolant");
			mistCoolantCheck.addItemListener(this);

			add(mistCoolantLabel);
			add(mistCoolantCheck,"wrap");
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
			
			JLabel valveLabel = new JLabel(valveString);
			valveLabel.setMinimumSize(labelMinimumSize);
			valveLabel.setMaximumSize(labelMinimumSize);
			valveLabel.setPreferredSize(labelMinimumSize);
			valveLabel.setHorizontalAlignment(JLabel.LEFT);

			JCheckBox valveCheck = new JCheckBox(enableString);
			valveCheck.setName("valve-check");
			valveCheck.addItemListener(this);

			add(valveLabel);
			add(valveCheck,"wrap");
		}

		// valve controls
		if (t.hasCollet()) {
			JLabel colletLabel = new JLabel("Collet");
			colletLabel.setMinimumSize(labelMinimumSize);
			colletLabel.setMaximumSize(labelMinimumSize);
			colletLabel.setPreferredSize(labelMinimumSize);
			colletLabel.setHorizontalAlignment(JLabel.LEFT);

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

	synchronized public void updateStatus() {
		Second second = new Second(new Date(System.currentTimeMillis() - startMillis));
		if (machine.getModel().currentTool() == toolModel && toolModel.hasHeater()) {
			double temperature = machine.getDriver().getTemperature();
			currentTempField.setText(Double.toString(temperature));
			measuredDataset.add(second, temperature,"a");
			targetDataset.add(second, targetTemperature,"a");
		}
		if (machine.getModel().currentTool() == toolModel && toolModel.hasHeatedPlatform()) {
			double temperature = machine.getDriver().getPlatformTemperature();
			platformCurrentTempField.setText(Double.toString(temperature));
			measuredPlatformDataset.add(second, temperature,"a");
			targetPlatformDataset.add(second, targetPlatformTemperature,"a");
		}
	}

	public void focusGained(FocusEvent e) {
	}

	public void focusLost(FocusEvent e) {
		JTextField source = (JTextField) e.getSource();
		handleChangedTextField(source);
	}

	public void handleChangedTextField(JTextField source)
	{
		String name = source.getName();
		Driver driver = machine.getDriver();
		if (source.getText().length() > 0) {
			if (name.equals("target-temp")) {
				double temperature = Double.parseDouble(source.getText());
				driver.setTemperature(temperature);
				targetTemperature = temperature;
			} else if (name.equals("platform-target-temp")) {
				double temperature = Double.parseDouble(source.getText());
				driver.setPlatformTemperature(temperature);
				targetPlatformTemperature = temperature;
			} else if (name.equals("motor-speed")) {
				driver.setMotorRPM(Double.parseDouble(source.getText()));
			} else if (name.equals("motor-speed-pwm")) {
				driver.setMotorSpeedPWM(Integer.parseInt(source.getText()));
			} else
				Base.logger.warning("Unhandled text field: "+name);
		}
	}


	public void itemStateChanged(ItemEvent e) {
		Component source = (Component) e.getItemSelectable();
		String name = source.getName();
		Driver driver = machine.getDriver();
		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (name.equals("motor-forward")) {
				driver.setMotorDirection(ToolModel.MOTOR_CLOCKWISE);
				driver.enableMotor();
			} else if (name.equals("motor-reverse")) {
				driver.setMotorDirection(ToolModel.MOTOR_COUNTER_CLOCKWISE);
				driver.enableMotor();
			} else if (name.equals("motor-stop"))
				driver.disableMotor();
			else if (name.equals("spindle-enabled"))
				driver.enableSpindle();
			else if (name.equals("flood-coolant"))
				driver.enableFloodCoolant();
			else if (name.equals("mist-coolant"))
				driver.enableMistCoolant();
			else if (name.equals("fan-check"))
				driver.enableFan();
			else if (name.equals("valve-check"))
				driver.openValve();
			else if (name.equals("collet-check"))
				driver.openCollet();
			else
				Base.logger.warning("checkbox selected: " + source.getName());
		} else {
			if (name.equals("motor-enabled"))
				driver.disableMotor();
			else if (name.equals("spindle-enabled"))
				driver.disableSpindle();
			else if (name.equals("flood-coolant"))
				driver.disableFloodCoolant();
			else if (name.equals("mist-coolant"))
				driver.disableMistCoolant();
			else if (name.equals("fan-check"))
				driver.disableFan();
			else if (name.equals("valve-check"))
				driver.closeValve();
			else if (name.equals("collet-check"))
				driver.closeCollet();
			// else
			// System.out.println("checkbox deselected: " + source.getName());
		}
	}

	public void actionPerformed(ActionEvent e) {
		String s = e.getActionCommand();
		
		if(s.equals("handleTextfield"))
		{
			JTextField source = (JTextField) e.getSource();
			handleChangedTextField(source);
			source.selectAll();
		}
	}


}
