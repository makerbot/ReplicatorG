package replicatorg.app.ui.controlpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

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
	
	final private static Color targetColor = Color.BLUE;
	final private static Color measuredColor = Color.RED;
	final private static Color targetPlatformColor = Color.GREEN;
	final private static Color measuredPlatformColor = Color.YELLOW;
	
	long startMillis = System.currentTimeMillis();

	private TimeTableXYDataset measuredDataset = new TimeTableXYDataset();
	private TimeTableXYDataset targetDataset = new TimeTableXYDataset();
	private TimeTableXYDataset measuredPlatformDataset = new TimeTableXYDataset();
	private TimeTableXYDataset targetPlatformDataset = new TimeTableXYDataset();

	public ChartPanel makeChart(ToolModel t) {
		JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, 
				measuredDataset, PlotOrientation.VERTICAL, 
				false, false, false);
		chart.setBorderVisible(false);
		chart.setBackgroundPaint(null);
		XYPlot plot = chart.getXYPlot();
		ValueAxis axis = plot.getDomainAxis();
		axis.setLowerMargin(0);
		axis.setFixedAutoRange(4L*60L*1000L); // auto range to four minutes
		axis.setTickLabelsVisible(false); // We don't need to see the millisecond count
		axis = plot.getRangeAxis();
		axis.setRange(0,260); // set termperature range from 0 to 260 degrees C 
		// Tweak L&F of chart
		//((XYAreaRenderer)plot.getRenderer()).setOutline(true);
		XYStepRenderer renderer = new XYStepRenderer();
		plot.setDataset(1, targetDataset);
		plot.setRenderer(1, renderer);
		plot.getRenderer(1).setSeriesPaint(0, targetColor);
		plot.getRenderer(0).setSeriesPaint(0, measuredColor);
		if (t.hasHeatedPlatform()) {
			System.err.println("Adding HBP graph");
			plot.setDataset(2,measuredPlatformDataset);
			plot.setRenderer(2, new XYLineAndShapeRenderer()); 
			plot.getRenderer(2).setSeriesPaint(0, measuredPlatformColor);
			plot.setDataset(3,targetPlatformDataset);
			plot.setRenderer(3, new XYStepRenderer()); 
			plot.getRenderer(3).setSeriesPaint(0, targetPlatformColor);
		}
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
		//renderer.setSeriesLinesVisible(1, true);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(400,140));
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
//				field.addActionListener(this);

				add(label);
				add(field,"wrap");
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
				add(field,"wrap");
			}
			// create our motor options
			JLabel motorEnabledLabel = new JLabel("Motor Control");
			motorEnabledLabel.setMinimumSize(labelMinimumSize);
			motorEnabledLabel.setMaximumSize(labelMinimumSize);
			motorEnabledLabel.setPreferredSize(labelMinimumSize);
			motorEnabledLabel.setHorizontalAlignment(JLabel.LEFT);

			JRadioButton motorReverseButton = new JRadioButton("reverse");
			motorReverseButton.setName("motor-reverse");
			motorReverseButton.addItemListener(this);

			JRadioButton motorStoppedButton = new JRadioButton("stop");
			motorStoppedButton.setName("motor-stop");
			motorStoppedButton.addItemListener(this);

			JRadioButton motorForwardButton = new JRadioButton("forward");
			motorForwardButton.setName("motor-forward");
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

		// our temperature fields
		if (t.hasHeater()) {
			JLabel targetTempLabel = new JLabel("Target Temperature (C)");
			targetTempLabel.setForeground(targetColor);
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

			JLabel currentTempLabel = new JLabel("Current Temperature (C)");
			currentTempLabel.setForeground(measuredColor);
			currentTempField = new JTextField();
			currentTempField.setMaximumSize(new Dimension(textBoxWidth, 25));
			currentTempField.setMinimumSize(new Dimension(textBoxWidth, 25));
			currentTempField.setPreferredSize(new Dimension(textBoxWidth, 25));
			currentTempField.setEnabled(false);

			add(targetTempLabel);
			add(targetTempField,"wrap");
			add(currentTempLabel);
			add(currentTempField,"wrap");
		}

		// our heated platform fields
		if (t.hasHeatedPlatform()) {
			JLabel targetTempLabel = new JLabel("Platform Target Temp (C)");
			targetTempLabel.setForeground(targetPlatformColor);
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

			JLabel currentTempLabel = new JLabel("Platform Current Temp (C)");
			currentTempLabel.setForeground(measuredPlatformColor);
//			currentTempLabel.setMinimumSize(labelMinimumSize);
//			currentTempLabel.setMaximumSize(labelMinimumSize);
//			currentTempLabel.setPreferredSize(labelMinimumSize);
//			currentTempLabel.setHorizontalAlignment(JLabel.LEFT);

			platformCurrentTempField = new JTextField();
			platformCurrentTempField.setMaximumSize(new Dimension(textBoxWidth, 25));
			platformCurrentTempField.setMinimumSize(new Dimension(textBoxWidth, 25));
			platformCurrentTempField.setPreferredSize(new Dimension(textBoxWidth, 25));
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
			add(fanCheck,"wrap");
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
