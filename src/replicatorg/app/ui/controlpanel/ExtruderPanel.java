package replicatorg.app.ui.controlpanel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
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

import replicatorg.app.Base;
import replicatorg.app.ui.CallbackTextField;
import replicatorg.drivers.commands.DriverCommand.AxialDirection;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.model.ToolModel;

public class ExtruderPanel extends JPanel{
	private final MachineInterface machine;
	private final ToolModel tool0, tool1;
	
	private JFormattedTextField t0CurrentTemperatureField;
	private JFormattedTextField t1CurrentTemperatureField;
	private JFormattedTextField pCurrentTemperatureField;
	private JFormattedTextField t0TargetTemperatureField;
	private JFormattedTextField t1TargetTemperatureField;
	private JFormattedTextField pTargetTemperatureField;

	private double t0TargetTemperature;
	private double t1TargetTemperature;
	private double pTargetTemperature;

	final private static Color t0TargetColor = Color.MAGENTA;
	final private static Color t0MeasuredColor = Color.RED;
	final private static Color t1TargetColor = Color.CYAN;
	final private static Color t1MeasuredColor = Color.BLUE;
	final private static Color pTargetColor = Color.YELLOW;
	final private static Color pMeasuredColor = Color.GREEN;
	
	long startMillis = System.currentTimeMillis();

	private TimeTableXYDataset t0MeasuredDataset = new TimeTableXYDataset();
	private TimeTableXYDataset t0TargetDataset = new TimeTableXYDataset();
	private TimeTableXYDataset t1MeasuredDataset = new TimeTableXYDataset();
	private TimeTableXYDataset t1TargetDataset = new TimeTableXYDataset();
	private TimeTableXYDataset pMeasuredDataset = new TimeTableXYDataset();
	private TimeTableXYDataset pTargetDataset = new TimeTableXYDataset();

	protected Pattern extrudeTimePattern;
	
	protected String[] extrudeTimeStrings = { /* "Continuous Move", */ "1s", "2s", "5s", "10s", "30s", "60s", "300s" };
	
	protected boolean continuousJogMode = false;
	protected long extrudeTime;
	private final String EXTRUDE_TIME_PREF_NAME = "extruderpanel.extrudetime";
	
	
	
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

//	public ChartPanel makeChart(ToolModel tool) {
//		JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, 
//				t0MeasuredDataset, PlotOrientation.VERTICAL, 
//				false, false, false);
//		chart.setBorderVisible(false);
//		chart.setBackgroundPaint(null);
//		XYPlot plot = chart.getXYPlot();
//		ValueAxis axis = plot.getDomainAxis();
//		axis.setLowerMargin(0);
//		axis.setFixedAutoRange(3L*60L*1000L); // auto range to three minutes
//		TickUnits unitSource = new TickUnits();
//		unitSource.add(new NumberTickUnit(60L*1000L)); // minutes
//		unitSource.add(new NumberTickUnit(1L*1000L)); // seconds
//		axis.setStandardTickUnits(unitSource);
//		axis.setTickLabelsVisible(false); // We don't need to see the millisecond count
//		axis = plot.getRangeAxis();
//		axis.setRange(0,300); // set termperature range from 0 to 300 degrees C so you can see overshoots 
//		// Tweak L&F of chart
//		//((XYAreaRenderer)plot.getRenderer()).setOutline(true);
//		XYStepRenderer renderer = new XYStepRenderer();
//		plot.setDataset(1, t0TargetDataset);
//		plot.setRenderer(1, renderer);
//		plot.getRenderer(1).setSeriesPaint(0, t0TargetColor);
//		plot.getRenderer(0).setSeriesPaint(0, t0MeasuredColor);
//		if (tool.hasHeatedPlatform()) {
//			plot.setDataset(2,pMeasuredDataset);
//			plot.setRenderer(2, new XYLineAndShapeRenderer(true,false)); 
//			plot.getRenderer(2).setSeriesPaint(0, pMeasuredColor);
//			plot.setDataset(3,pTargetDataset);
//			plot.setRenderer(3, new XYStepRenderer()); 
//			plot.getRenderer(3).setSeriesPaint(0, pTargetColor);
//		}
//		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
//		ChartPanel chartPanel = new ChartPanel(chart);
//		chartPanel.setPreferredSize(new Dimension(400,160));
//		chartPanel.setOpaque(false);
//		return chartPanel;
//	}

	public ChartPanel makeChart() {
		JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, 
				t0MeasuredDataset, PlotOrientation.VERTICAL, 
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
		plot.setDataset(1, t0TargetDataset);
		plot.setRenderer(1, renderer);
		plot.getRenderer(1).setSeriesPaint(0, t0TargetColor);
		plot.getRenderer(0).setSeriesPaint(0, t0MeasuredColor);
		boolean hasPlatform = tool0.hasHeatedPlatform();
		if(machine.getModel().getTools().size() > 1)
		{
			plot.setDataset(4, t1MeasuredDataset);
			plot.setRenderer(4, new XYLineAndShapeRenderer(true,false)); 
			plot.getRenderer(4).setSeriesPaint(0, t1MeasuredColor);
			plot.setDataset(5, t1TargetDataset);
			plot.setRenderer(5, new XYStepRenderer()); 
			plot.getRenderer(5).setSeriesPaint(0, t1TargetColor);
			
			if(!hasPlatform)
				hasPlatform = tool1.hasHeatedPlatform();
		}
		if (hasPlatform) {
			plot.setDataset(2,pMeasuredDataset);
			plot.setRenderer(2, new XYLineAndShapeRenderer(true,false)); 
			plot.getRenderer(2).setSeriesPaint(0, pMeasuredColor);
			plot.setDataset(3,pTargetDataset);
			plot.setRenderer(3, new XYStepRenderer()); 
			plot.getRenderer(3).setSeriesPaint(0, pTargetColor);
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
	
	/*
	 * This creates a motor control panel, wrapping its own itemListener and actionListener 
	 */
	protected JPanel getMotorControls(final ToolModel tool) {
		JPanel panel = new JPanel(new MigLayout("fill"));
		
		// Call handleItemChange with this particular tool
		ItemListener itemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				handleChangedItem(e, tool);
			}
			
		};

		// Call handleMotorAction with this particular tool
		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleMotorAction(e, tool);
			}
			
		};
		
		if (tool.hasMotor()) {
			// Due to current implementation issues, we need to send the PWM
			// before the RPM for a stepper motor. Thus we display both controls in these
			// cases. This shouldn't be necessary for a Gen4 stepper extruder. (it's not!)
			if ((tool.getMotorStepperAxisName() == "") && 
					!(tool.motorHasEncoder() || tool.motorIsStepper())) {
				// our motor speed vars
				JLabel label = makeLabel("Motor Speed (PWM)");

				JFormattedTextField field = new CallbackTextField(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e) {
						JFormattedTextField source = (JFormattedTextField)e.getSource();
						if (source.getText().length() > 0) {
							double newValue = ((Number)source.getValue()).doubleValue();
							machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedPWM((int)newValue, tool.getIndex()));
						}
					}
				}, "handleTextField", "motor-speed-pwm", 5, Base.getLocalFormat());
				field.setValue(machine.getDriverQueryInterface().getMotorSpeedPWM());
				panel.add(label);
				panel.add(field,"wrap");
			}

			if (tool.motorHasEncoder() || tool.motorIsStepper()) {
				// our motor speed vars
				JLabel label = makeLabel("Motor Speed (RPM)");

				JFormattedTextField field = new CallbackTextField(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent e) {
						JFormattedTextField source = (JFormattedTextField)e.getSource();
						if (source.getText().length() > 0) {
							double newValue = ((Number)source.getValue()).doubleValue();
							machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedRPM(newValue, tool.getIndex()));
						}
					}
				}, "handleTextField", "motor-speed", 5, Base.getLocalFormat());
				field.setValue(tool.getMotorSpeedReadingRPM() );// <-- should be
				//field.setValue(machine.getDriver().getMotorRPM());
				panel.add(label, "");
				panel.add(field,"wrap");

				if (tool.getMotorStepperAxisName() != "") {
					label = makeLabel("Extrude duration");

					JComboBox timeList = new JComboBox(extrudeTimeStrings);
					timeList.setSelectedItem(Base.preferences.get(EXTRUDE_TIME_PREF_NAME,"5s"));
					timeList.setActionCommand("Extrude-duration");
					timeList.addActionListener(actionListener);
					setExtrudeTime((String)timeList.getSelectedItem());
					panel.add(label);
					panel.add(timeList,"wrap");
				}
			}
			
			// create our motor options
			JLabel motorEnabledLabel = makeLabel("Motor Control");
			
			if (tool.motorHasEncoder() || (tool.motorIsStepper() && tool.getMotorStepperAxisName() != "")) {

				JButton motorReverseButton = new JButton("reverse");
				motorReverseButton.setActionCommand("reverse");
				motorReverseButton.addActionListener(actionListener);

				JButton motorStoppedButton = new JButton("stop");
				motorStoppedButton.setActionCommand("stop");
				motorStoppedButton.addActionListener(actionListener);

				JButton motorForwardButton = new JButton("forward");
				motorForwardButton.setActionCommand("forward");
				motorForwardButton.addActionListener(actionListener);

				ButtonGroup motorControl = new ButtonGroup();
				motorControl.add(motorReverseButton);
				motorControl.add(motorStoppedButton);
				motorControl.add(motorForwardButton);

				// add components in.
				panel.add(motorEnabledLabel,"split,spanx");
				panel.add(motorReverseButton);
				panel.add(motorStoppedButton);
				panel.add(motorForwardButton,"wrap");
			}
			else {
				JRadioButton motorReverseButton = new JRadioButton("reverse");
				motorReverseButton.setName("motor-reverse");
				motorReverseButton.setActionCommand("motor-reverse");
				motorReverseButton.addItemListener(itemListener);

				JRadioButton motorStoppedButton = new JRadioButton("stop");
				motorStoppedButton.setName("motor-stop");
				motorStoppedButton.setActionCommand("motor-stop");
				motorStoppedButton.addItemListener(itemListener);

				JRadioButton motorForwardButton = new JRadioButton("forward");
				motorForwardButton.setName("motor-forward");
				motorForwardButton.setActionCommand("motor-forward");
				motorForwardButton.addItemListener(itemListener);

				ButtonGroup motorControl = new ButtonGroup();
				motorControl.add(motorReverseButton);
				motorControl.add(motorStoppedButton);
				motorControl.add(motorForwardButton);

				// add components in.
				panel.add(motorEnabledLabel,"split,spanx");
				panel.add(motorReverseButton);
				panel.add(motorStoppedButton);
				panel.add(motorForwardButton,"wrap");		
			}

		}
		if (tool.hasAutomatedPlatform()) {
			String abpString = "Build platform belt";
			String enableString = "enable";
			JLabel abpLabel = makeLabel(abpString);
	
			JCheckBox abpCheck = new JCheckBox(enableString);
			abpCheck.setName("abp-check");
			abpCheck.addItemListener(itemListener);
	
			panel.add(abpLabel);
			panel.add(abpCheck,"wrap");
		}
		return panel;
	}
	
	public ExtruderPanel(final MachineInterface machine) {
		this.machine = machine;
		List<ToolModel> tools = machine.getModel().getTools();
		if(tools.size() > 0)
			tool0 = tools.get(0);
		else
			tool0 = null;
		if(tools.size() > 1)
			tool1 = tools.get(1);
		else
			tool1 = null;
		
		
		extrudeTimePattern = Pattern.compile("([.0-9]+)");
		
		ActionListener temperatureListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleChangedTextField((JFormattedTextField)e.getSource());
			}
		};
		
		// create our initial panel
		setLayout(new MigLayout("fill"));
		
		// Display the motor controls
		if(tools.size() == 1)
		{
			JPanel motorControl = getMotorControls(tool0);
			motorControl.setBorder(BorderFactory.createTitledBorder("Extruder Motor Control"));
			add(motorControl, "aligny top, spanx, growx, wrap");
		}
		else if(tools.size() > 1)
		{
			JTabbedPane motorTabs = new JTabbedPane();

			final String tool0Name = tool0.getName() + " Plastic Extruder";
			final String tool1Name = tool1.getName() + " Plastic Extruder";
			// Left on the left
			if(tool0.getIndex() == 0) {
				motorTabs.addTab(tool1Name, getMotorControls(tool1));
				motorTabs.addTab(tool0Name, getMotorControls(tool0));
			}
			else if(tool0.getIndex() == 1) {
				motorTabs.addTab(tool0Name, getMotorControls(tool0));
				motorTabs.addTab(tool1Name, getMotorControls(tool1));
			}
			
			add(motorTabs, "aligny top, spanx, growx, wrap");
		}
		
		// The temperature display
		JPanel temperaturePanel = new JPanel(new MigLayout("fillx, filly"));
		temperaturePanel.setBorder(BorderFactory.createTitledBorder("Extruder Temperature Controls"));
		
		if(tool0 != null)
		{
			if (tool0.hasHeater()) {
				JLabel targetTempLabel = makeKeyLabel("<html>"+tool0.getName()+" Target (&deg;C)</html>",t0TargetColor);
				
				t0TargetTemperatureField = new CallbackTextField(temperatureListener, "handleTextField", "target-temp", 5, Base.getLocalFormat());
				t0TargetTemperatureField.setValue(tool0.getTargetTemperature());
				
				JLabel currentTempLabel = makeKeyLabel("<html>"+tool0.getName()+" Current (&deg;C)</html>",t0MeasuredColor);
				t0CurrentTemperatureField = new JFormattedTextField(Base.getLocalFormat());
				t0CurrentTemperatureField.setColumns(5);
				t0CurrentTemperatureField.setEnabled(false);
				
				temperaturePanel.add(targetTempLabel);
				temperaturePanel.add(t0TargetTemperatureField);
				temperaturePanel.add(currentTempLabel);
				temperaturePanel.add(t0CurrentTemperatureField, "wrap");
			}
		}
		if(tool1 != null)
		{
			if (tool1.hasHeater()) {
				JLabel targetTempLabel = makeKeyLabel("<html>"+tool1.getName()+" Target (&deg;C)</html>",t1TargetColor);
				
				t1TargetTemperatureField = new CallbackTextField(temperatureListener, "handleTextField", "target-temp", 5, Base.getLocalFormat());
				t1TargetTemperatureField.setValue(tool1.getTargetTemperature());
				
				JLabel currentTempLabel = makeKeyLabel("<html>"+tool1.getName()+" Current (&deg;C)</html>",t1MeasuredColor);
				t1CurrentTemperatureField = new JFormattedTextField(Base.getLocalFormat());
				t1CurrentTemperatureField.setColumns(5);
				t1CurrentTemperatureField.setEnabled(false);
				
				temperaturePanel.add(targetTempLabel);
				temperaturePanel.add(t1TargetTemperatureField);
				temperaturePanel.add(currentTempLabel);
				temperaturePanel.add(t1CurrentTemperatureField, "wrap");
			}
		}
		
		// If either head has a HBP
		if (tool0.hasHeatedPlatform() || (tool1 != null && tool1.hasHeatedPlatform())) {
			// Get the tool with the HBP
			ToolModel tool = tool0.hasHeatedPlatform() ? tool0 : tool1;
			
			JLabel targetTempLabel = makeKeyLabel("<html>Platform Target (&deg;C)</html>",pTargetColor);
			
			pTargetTemperatureField = new CallbackTextField(temperatureListener, "handleTextField", "platform-target-temp", 5, Base.getLocalFormat());
			pTargetTemperatureField.setValue(tool.getPlatformTargetTemperature());

			JLabel currentTempLabel = makeKeyLabel("<html>Platform Current (&deg;C)</html>",pMeasuredColor);
			
			pCurrentTemperatureField = new JFormattedTextField(Base.getLocalFormat());
			pCurrentTemperatureField.setColumns(5);
			pCurrentTemperatureField.setEnabled(false);

			temperaturePanel.add(targetTempLabel);
			temperaturePanel.add(pTargetTemperatureField);
			temperaturePanel.add(currentTempLabel);
			temperaturePanel.add(pCurrentTemperatureField, "wrap");
		}
		
		
		temperaturePanel.add(new JLabel("Temperature Chart"),"growx,spanx,wrap");
		temperaturePanel.add(makeChart(),"growx, growy, spanx, wrap");
		add(temperaturePanel, "growx, growy");
	}
	
//	public ExtruderPanel(MachineInterface machine, ToolModel tool) {
//		this.machine = machine;
//		tool
//
//		extrudeTimePattern = Pattern.compile("([.0-9]+)");
//		
//		// create our initial panel
//		setLayout(new MigLayout());
//		add(new JLabel("Plastic Extruder Controls"), "align 50%, spanx, wrap, gapbottom 10");
//
//		// create our motor options
//		add(getMotorControls(tool), "spanx, wrap");
//
//		// our temperature fields
//		if (tool.hasHeater()) {
//			JLabel targetTempLabel = makeKeyLabel("Target Temperature (C)",targetColor);
//			JFormattedTextField targetTempField = new CallbackTextField(this, "handleTextField", "target-temp", 9, Base.getLocalFormat());
//			
//			targetTemperature = tool.getTargetTemperature();
//			targetTempField.setValue(targetTemperature);
//
//			JLabel currentTempLabel = makeKeyLabel("Current Temperature (C)",measuredColor);
//			currentTempField = new JFormattedTextField(Base.getLocalFormat());
//			currentTempField.setColumns(9);
//			currentTempField.setEnabled(false);
//			
//			add(targetTempLabel);
//			add(targetTempField,"wrap");
//			add(currentTempLabel);
//			add(currentTempField,"wrap");
//		}
//
//		// our heated platform fields
//		if (tool.hasHeatedPlatform()) {
//			JLabel targetTempLabel = makeKeyLabel("Platform Target Temp (C)",targetPlatformColor);
//			JFormattedTextField targetTempField = new CallbackTextField(this, "handleTextField", "platform-target-temp", 9, Base.getLocalFormat());
//
//			targetPlatformTemperature = tool.getPlatformTargetTemperature();
//			targetTempField.setValue(targetPlatformTemperature);
//
//			JLabel currentTempLabel = makeKeyLabel("Platform Current Temp (C)",measuredPlatformColor);
//			platformCurrentTempField = new JFormattedTextField(Base.getLocalFormat());
//			platformCurrentTempField.setColumns(9);
//			platformCurrentTempField.setEnabled(false);
//
//			add(targetTempLabel);
//			add(targetTempField,"wrap");
//			add(currentTempLabel);
//			add(platformCurrentTempField,"wrap");
//			
//		}
//
//		if (tool.hasHeater() || tool.hasHeatedPlatform()) {
//			add(new JLabel("Temperature Chart"),"growx,spanx,wrap");
//			add(makeChart(tool),"growx,spanx,wrap");
//		}
//
//		// flood coolant controls
//		if (tool.hasFloodCoolant()) {
//			Base.logger.severe("hasFloodCoolant not supported due to toolhead madness.");
////			JLabel floodCoolantLabel = makeLabel("Flood Coolant");
////
////			JCheckBox floodCoolantCheck = new JCheckBox("enable");
////			floodCoolantCheck.setName("flood-coolant");
////			floodCoolantCheck.addItemListener(this);
////
////			add(floodCoolantLabel);
////			add(floodCoolantCheck,"wrap");
//		}
//
//		// mist coolant controls
//		if (tool.hasMistCoolant()) {
//			Base.logger.severe("hasMistCoolant not supported due to toolhead madness.");
////			JLabel mistCoolantLabel = makeLabel("Mist Coolant");
////
////			JCheckBox mistCoolantCheck = new JCheckBox("enable");
////			mistCoolantCheck.setName("mist-coolant");
////			mistCoolantCheck.addItemListener(this);
////
////			add(mistCoolantLabel);
////			add(mistCoolantCheck,"wrap");
//		}
//
//		// cooling fan controls
//		if (tool.hasFan()) {
//			Base.logger.finer("ExtruderPanel.hasFan(): fan automatic and not user controlled in The Replicator.");
////			String fanString = "Cooling Fan";
////			String enableString = "enable";
////			Element xml = findMappingNode(tool.getXml(),"fan");
////			if (xml != null) {
////				fanString = xml.getAttribute("name");
////				enableString = xml.getAttribute("actuated");
////			}
////			JLabel fanLabel = makeLabel(fanString);
////
////			JCheckBox fanCheck = new JCheckBox(enableString);
////			fanCheck.setName("fan-check");
////			fanCheck.addItemListener(this);
////
////			add(fanLabel);
////			add(fanCheck,"wrap");
//		}
//
//		// cooling fan controls
//		if (tool.hasAutomatedPlatform()) {
//			String abpString = "Build platform belt";
//			String enableString = "enable";
//			JLabel abpLabel = makeLabel(abpString);
//
//			JCheckBox abpCheck = new JCheckBox(enableString);
//			abpCheck.setName("abp-check");
//			abpCheck.addItemListener(this);
//
//			add(abpLabel);
//			add(abpCheck,"wrap");
//		}
//
//		// valve controls
//		if (tool.hasValve()) {
//			Base.logger.severe("hasValve not supported due to toolhead madness.");
////			String valveString = "Valve";
////			String enableString = "open";
////
////			Element xml = findMappingNode(tool.getXml(),"valve");
////			if (xml != null) {
////				valveString = xml.getAttribute("name");
////				enableString = xml.getAttribute("actuated");
////			}
////			
////			JLabel valveLabel = makeLabel(valveString);
////
////			JCheckBox valveCheck = new JCheckBox(enableString);
////			valveCheck.setName("valve-check");
////			valveCheck.addItemListener(this);
////
////			add(valveLabel);
////			add(valveCheck,"wrap");
//		}
//
//		// valve controls
//		if (tool.hasCollet()) {
//			Base.logger.severe("hasCollect not supported due to toolhead madness.");
////			JLabel colletLabel = makeLabel("Collet");
////
////			JCheckBox colletCheck = new JCheckBox("open");
////			colletCheck.setName("collet-check");
////			colletCheck.addItemListener(this);
////
////			JPanel colletPanel = new JPanel();
////			colletPanel.setLayout(new BoxLayout(colletPanel,
////					BoxLayout.LINE_AXIS));
////			colletPanel.setMaximumSize(panelSize);
////			colletPanel.setMinimumSize(panelSize);
////			colletPanel.setPreferredSize(panelSize);
////
////			add(colletLabel);
////			add(colletCheck,"wrap");
//		}
//	}

//	private Element findMappingNode(Node xml,String portName) {
//		// scan the remapping nodes.
//		NodeList children = xml.getChildNodes();
//		for (int j=0; j<children.getLength(); j++) {
//			Node child = children.item(j);
//			if (child.getNodeName().equals("remap")) {
//				Element e = (Element)child;
//				if (e.getAttribute("port").equals(portName)) {
//					return e;
//				}
//			}
//		}
//		return null;
//	}

	public void updateStatus() {
		Second second = new Second(new Date(System.currentTimeMillis() - startMillis));

		ToolModel platform = null;
		
		// Some changes to the way (& frequency) temperatures are read make it easier
		// to just read this cached value which will be updated regularly
		if (tool0 != null) {
			t0CurrentTemperatureField.setValue(tool0.getCurrentTemperature());
			t0MeasuredDataset.add(second, tool0.getCurrentTemperature(),"a");
			t0TargetDataset.add(second, t0TargetTemperature,"a");
			
			if(tool0.hasHeatedPlatform())
				platform = tool0;
		}
		if (tool1 != null) {
			t1CurrentTemperatureField.setValue(tool1.getCurrentTemperature());
			t1MeasuredDataset.add(second, tool1.getCurrentTemperature(),"a");
			t1TargetDataset.add(second, t1TargetTemperature,"a");
			
			if(tool1.hasHeatedPlatform())
				platform = tool1;
		}
		
		if (platform != null) {
			pCurrentTemperatureField.setValue(platform.getPlatformCurrentTemperature());
			pMeasuredDataset.add(second, platform.getPlatformCurrentTemperature(),"a");
			pTargetDataset.add(second, pTargetTemperature,"a");
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

	private void handleChangedTextField(JFormattedTextField source)
	{
		String name = source.getName();
		int toolhead;
		double newValue = Double.NaN;

		// tools position may not match index
		if(source == t0TargetTemperatureField)
			toolhead = tool0.getIndex();
		else if(source == t1TargetTemperatureField)
			toolhead = tool1.getIndex();
		else if(source == pTargetTemperatureField)
			toolhead = -1; // -1 means autodetect
		else {
			Base.logger.warning("Unhandled text field: "+name);
			return;
		}
		
		// make sure there's something there
		if (source.getText().length() > 0) {
			newValue = ((Number)source.getValue()).doubleValue();
			
			// if we have a toolhead temperature
			if(toolhead != -1) {
				newValue = confirmTemperature(newValue,"temperature.acceptedLimit",260.0);
				if (newValue == Double.MIN_VALUE) {
					return;
				}
				machine.runCommand(new replicatorg.drivers.commands.SetTemperature(newValue, toolhead));
			} else { // platform
				newValue = confirmTemperature(newValue,"temperature.acceptedLimit.bed",130.0);
				if (newValue == Double.MIN_VALUE) {
					return;
				}
				machine.runCommand(new replicatorg.drivers.commands.SetPlatformTemperature(newValue, toolhead));
			}
		}
		if(newValue != Double.NaN) {
			if(source == t0TargetTemperatureField)
				t0TargetTemperature = newValue;
			else if(source == t1TargetTemperatureField)
				t1TargetTemperature = newValue;
			else if(source == pTargetTemperatureField)
				pTargetTemperature = newValue;
		}
	}
	
	private void handleChangedItem(ItemEvent e, ToolModel tool) {
		int toolhead = tool.getIndex();
		Component source = (Component) e.getItemSelectable();
		String name = source.getName();
		if (e.getStateChange() == ItemEvent.SELECTED) {
			/* Handle DC extruder commands */
			if (name.equals("motor-forward")) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.CLOCKWISE,toolhead));
				// TODO: Hack to support RepRap/Ultimaker- always re-send RPM
				if (tool.motorHasEncoder() || tool.motorIsStepper()) {
					machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedRPM(machine.getDriver().getMotorRPM(),toolhead));
				}
				machine.runCommand(new replicatorg.drivers.commands.EnableExtruderMotor(toolhead));
			} else if (name.equals("motor-reverse")) {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.COUNTERCLOCKWISE,toolhead));
				// TODO: Hack to support RepRap/Ultimaker- always re-send RPM
				if (tool.motorHasEncoder() || tool.motorIsStepper()) {
					machine.runCommand(new replicatorg.drivers.commands.SetMotorSpeedRPM(machine.getDriver().getMotorRPM(),toolhead));
				}
				machine.runCommand(new replicatorg.drivers.commands.EnableExtruderMotor(toolhead));
			} else if (name.equals("motor-stop")) {
				machine.runCommand(new replicatorg.drivers.commands.DisableMotor(toolhead));
			}
			else if (name.equals("abp-check")) {
				machine.runCommand(new replicatorg.drivers.commands.ToggleAutomatedBuildPlatform(true,toolhead));
				machine.runCommand(new replicatorg.drivers.commands.EnableFan(toolhead));
			}
		} else {
			if (name.equals("motor-enabled"))
				machine.runCommand(new replicatorg.drivers.commands.DisableMotor(toolhead));
			else if (name.equals("fan-check"))
				machine.runCommand(new replicatorg.drivers.commands.DisableFan(toolhead));
			else if (name.equals("abp-check")) {
				// TODO: Debugging. Run both!
				machine.runCommand(new replicatorg.drivers.commands.ToggleAutomatedBuildPlatform(false,toolhead));
				machine.runCommand(new replicatorg.drivers.commands.DisableFan(toolhead));
			}
		}
	}
	private void handleMotorAction(ActionEvent e, ToolModel tool) {
		int toolhead = tool.getIndex();
		String actionName = e.getActionCommand();

		if (actionName.equals("Extrude-duration")) {
			JComboBox cb = (JComboBox) e.getSource();
			String timeText = (String) cb.getSelectedItem();
			setExtrudeTime(timeText);
		}
		/* Handle stepper extruder commands */
		if (actionName.equals("forward")) {

			if (tool.getMotorStepperAxisName() != "") {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.CLOCKWISE,toolhead));
				// Reverted to one single command for RepRap5D driver
				if (machine.getDriver().getDriverName().equals("RepRap5D")) {
					machine.runCommand(new replicatorg.drivers.commands.EnableExtruderMotor(extrudeTime*1000,toolhead));
				} else {
					// See Note (***) Below:
					machine.runCommand(new replicatorg.drivers.commands.SelectTool(toolhead));

					machine.runCommand(new replicatorg.drivers.commands.EnableExtruderMotor(toolhead));
					machine.runCommand(new replicatorg.drivers.commands.Delay(extrudeTime*1000,toolhead));
					machine.runCommand(new replicatorg.drivers.commands.DisableMotor(toolhead));
				}
			}
		} else if (actionName.equals("reverse")) {
			if (tool.getMotorStepperAxisName() != "") {
				machine.runCommand(new replicatorg.drivers.commands.SetMotorDirection(AxialDirection.COUNTERCLOCKWISE,toolhead));
				// Reverted to one single command for RepRap5D driver
				if (machine.getDriver().getDriverName().equals("RepRap5D")) {
					machine.runCommand(new replicatorg.drivers.commands.EnableExtruderMotor(extrudeTime*1000,toolhead));
				} else {
					// See Note (***) Below:
					machine.runCommand(new replicatorg.drivers.commands.SelectTool(toolhead));

					machine.runCommand(new replicatorg.drivers.commands.EnableExtruderMotor(toolhead));
					machine.runCommand(new replicatorg.drivers.commands.Delay(extrudeTime*1000,toolhead));
					machine.runCommand(new replicatorg.drivers.commands.DisableMotor(toolhead));
				}
			}
		} else if (actionName.equals("stop")) {
			machine.runCommand(new replicatorg.drivers.commands.DisableMotor(toolhead));
			
			if (tool.getMotorStepperAxisName() != "") {
				machine.stopMotion();
			}
		}
		/* Note (***):
		 *   For some reason passing the toolhead into the driver commands isn't working for one of
		 *   the heads. However, selecting the tool before sending commands does seem to work.
		 *   It could, however, have unintended consequences. We'll see, I guess.
		 *     -Ted
		 */
		
	}
}
