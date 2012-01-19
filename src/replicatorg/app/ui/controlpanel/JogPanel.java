package replicatorg.app.ui.controlpanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
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
import javax.vecmath.Point2d;
import javax.vecmath.Point2i;

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

	/*
	 * ButtonArrangement defines the variables needed for creating any 
	 * JogButton we might show. Subclasses of ButtonArrangement may 
	 * choose not use every available button.
	 * 
	 * ButtonArrangement also defines a function for retrieving the 
	 * jogbutton panel.
	 */
	private abstract class ButtonArrangement {
		public String buttonFolder = "images/jog/";
		
		public String overButtonString = "Over";
		public String pressedButtonString = "Down";

		public String backgroundImageString = null;
		public Point2i backgroundImageLocation = null;
		public String extruderImageString = "extruder";
		public Point2i extruderImageLocation = null;
		public String dualextruderImageString = "dual-extruder";
		public Point2i dualextruderImageLocation = null;
		public String buildplateImageString = "buildplate";
		public Point2i buildplateImageLocation = null;

		public String xMinusButtonString = "X-";
		public String xPlusButtonString = "X+";
		public String yMinusButtonString = "Y-";
		public String yPlusButtonString = "Y+";
		public String zMinusButtonString = "Z-";
		public String zPlusButtonString = "Z+";
		
		public String xMinusTooltip = "Jog X axis in negative direction";
		public String xPlusTooltip = "Jog X axis in positive direction";
		public String yMinusTooltip = "Jog Y axis in negative direction";
		public String yPlusTooltip = "Jog Y axis in positive direction";
		public String zMinusTooltip = "Jog Z axis in negative direction";
		public String zPlusTooltip = "Jog Z axis in positive direction";
		
		public Point2i xMinusButtonLocation;
		public Point2i xPlusButtonLocation;
		public Point2i yMinusButtonLocation;
		public Point2i yPlusButtonLocation;
		public Point2i zMinusButtonLocation;
		public Point2i zPlusButtonLocation;
		
		public String stopButtonString = "Stop";
		public String stopTooltip = "Emergency stop";
		public Point2i stopButtonLocation = null;

		public Point2i axisAPanelLocation;
		public Point2i axisBPanelLocation;

		public double xScale = 1;
		public double yScale = 1;
		
		public abstract JPanel getButtonPanel(MachineInterface machine);
		public Point2i scalePoint(double x, double y) {
			return new Point2i((int)(x * xScale), (int)(y * yScale));
		}
		public Image scaleImage(Image img) {
			return img.getScaledInstance((int)(img.getWidth(null)*xScale), (int)(img.getHeight(null)*yScale), Image.SCALE_SMOOTH);
		}
	}

	private class ThingomaticArrangement extends ButtonArrangement {
		public ThingomaticArrangement() {
			buttonFolder = "images/jog-new/thingomatic/";
			
			backgroundImageString = "tom";
			backgroundImageLocation = new Point2i(0, 0);
			extruderImageLocation = new Point2i(0, 0);
			dualextruderImageLocation = new Point2i(0, 0);
			buildplateImageLocation = new Point2i(0, 0);

			xMinusButtonLocation = new Point2i(140, 190);
			xPlusButtonLocation = new Point2i(50, 190);
			yMinusButtonLocation = new Point2i(75, 65);
			yPlusButtonLocation = new Point2i(10, 90);
			zMinusButtonLocation = new Point2i(255, 120);
			zPlusButtonLocation = new Point2i(255, 25);
			
			stopButtonLocation = new Point2i(0, 0);
		}

		@Override
		public JPanel getButtonPanel(MachineInterface machine) {
			
			JPanel panel = new JPanel(new MigLayout());
			
			JButton button = createJogButton(xMinusButtonString, xMinusTooltip, this);
			panel.add(button, "pos "+xMinusButtonLocation.x+" "+xMinusButtonLocation.y);
			button = createJogButton(xPlusButtonString, xPlusTooltip, this);
			panel.add(button, "pos "+xPlusButtonLocation.x+" "+xPlusButtonLocation.y);
			button = createJogButton(yMinusButtonString, yMinusTooltip, this);
			panel.add(button, "pos "+yMinusButtonLocation.x+" "+yMinusButtonLocation.y);
			button = createJogButton(yPlusButtonString, yPlusTooltip, this);
			panel.add(button, "pos "+yPlusButtonLocation.x+" "+yPlusButtonLocation.y);
			button = createJogButton(zMinusButtonString, zMinusTooltip, this);
			panel.add(button, "pos "+zMinusButtonLocation.x+" "+zMinusButtonLocation.y);
			button = createJogButton(zPlusButtonString, zPlusTooltip, this);
			panel.add(button, "pos "+zPlusButtonLocation.x+" "+zPlusButtonLocation.y);
			
			button = createJogButton(stopButtonString, stopTooltip, this);
			panel.add(button, "pos "+stopButtonLocation.x+" "+stopButtonLocation.y);

			Image buildplate = Base.getImage(buttonFolder+buildplateImageString+".png", panel);
			panel.add(new JLabel(new ImageIcon(buildplate)), 
								"pos "+buildplateImageLocation.x+" "+buildplateImageLocation.y);
			Image extruder = Base.getImage(buttonFolder+extruderImageString+".png", panel);
			panel.add(new JLabel(new ImageIcon(extruder)), 
								"pos "+extruderImageLocation.x+" "+extruderImageLocation.y);
			Image background = Base.getImage(buttonFolder+backgroundImageString+".png", panel); 
			panel.add(new JLabel(new ImageIcon(background)), 
								"pos "+backgroundImageLocation.x+" "+backgroundImageLocation.y);
			return panel;
		}
	}
	
	private class CupcakeArrangement extends ButtonArrangement {
		{
			buttonFolder = "images/jog-new/cupcake/";
		}

		@Override
		public JPanel getButtonPanel(MachineInterface machine) {
			return null;
			// TODO Auto-generated method stub
			
		}
	}
	
	private class ReplicatorArrangement extends ButtonArrangement {
		public ReplicatorArrangement() {
			buttonFolder = "images/jog-new/replicator/";

			xScale = .45;
			yScale = .45;
			
			backgroundImageString = "replicator";
			
			/*
			 * Yes, these numbers ARE magic. They're just some button
			 * positions I found to look okay.
			 */
			backgroundImageLocation = scalePoint(0, 0);
			extruderImageLocation = scalePoint(188, 60);
			dualextruderImageLocation = scalePoint(144, 60);
			buildplateImageLocation = scalePoint(100, 150);

			xMinusButtonLocation = scalePoint(185, 200);
			xPlusButtonLocation = scalePoint(95, 200);
			yMinusButtonLocation = scalePoint(65, 65);
			yPlusButtonLocation = scalePoint(0, 90);
			zMinusButtonLocation = scalePoint(357, 40);
			zPlusButtonLocation = scalePoint(357, 135);
			
			stopButtonLocation = scalePoint(165, 100);
		}

		@Override
		public JPanel getButtonPanel(MachineInterface machine) {
			
			JPanel panel = new JPanel(new MigLayout());
			
			JButton button = createJogButton(xMinusButtonString, xMinusTooltip, this);
			panel.add(button, "pos "+xMinusButtonLocation.x+" "+xMinusButtonLocation.y);
			button = createJogButton(xPlusButtonString, xPlusTooltip, this);
			panel.add(button, "pos "+xPlusButtonLocation.x+" "+xPlusButtonLocation.y);
			button = createJogButton(yMinusButtonString, yMinusTooltip, this);
			panel.add(button, "pos "+yMinusButtonLocation.x+" "+yMinusButtonLocation.y);
			button = createJogButton(yPlusButtonString, yPlusTooltip, this);
			panel.add(button, "pos "+yPlusButtonLocation.x+" "+yPlusButtonLocation.y);
			button = createJogButton(zMinusButtonString, zMinusTooltip, this);
			panel.add(button, "pos "+zMinusButtonLocation.x+" "+zMinusButtonLocation.y);
			button = createJogButton(zPlusButtonString, zPlusTooltip, this);
			panel.add(button, "pos "+zPlusButtonLocation.x+" "+zPlusButtonLocation.y);
			
			button = createJogButton(stopButtonString, stopTooltip, this);
			panel.add(button, "pos "+stopButtonLocation.x+" "+stopButtonLocation.y);

			Image buildplate = Base.getImage(buttonFolder+buildplateImageString+".png", panel);
			panel.add(new JLabel(new ImageIcon(scaleImage(buildplate))), 
								"pos "+buildplateImageLocation.x+" "+buildplateImageLocation.y);
			
			if(machine.getModel().getTools().size() > 1) {
				Image dualextruder = Base.getImage(buttonFolder+dualextruderImageString+".png", panel);
				panel.add(new JLabel(new ImageIcon(scaleImage(dualextruder))), 
									"pos "+dualextruderImageLocation.x+" "+dualextruderImageLocation.y);
			}
			else
			{
				Image extruder = Base.getImage(buttonFolder+extruderImageString+".png", panel);
				panel.add(new JLabel(new ImageIcon(scaleImage(extruder))), 
									"pos "+extruderImageLocation.x+" "+extruderImageLocation.y);
			}
			
			Image background = Base.getImage(buttonFolder+backgroundImageString+".png", panel); 
			panel.add(new JLabel(new ImageIcon(scaleImage(background))),
								"pos "+backgroundImageLocation.x+" "+backgroundImageLocation.y);
			return panel;
		}
	}
	
	private class OldArrangement extends ButtonArrangement {
		{
			buttonFolder = "images/jog/";
		}

		@Override
		public JPanel getButtonPanel(MachineInterface machine) {
			return null;
			// TODO Auto-generated method stub
			
		}
	}
	
	/**
	 * Creates a jog button, automatically fetching image resources from resources
	 * using the root string
	 * @author unknown
	 *
	 */
	public class JogButton extends JButton {
		
		public JogButton(String root, String tooltip, ButtonArrangement arrangement) {
			String baseImage = arrangement.buttonFolder + root;
			Image img = Base.getImage(baseImage + ".png", this);
			img = arrangement.scaleImage(img);
			setIcon(new ImageIcon(img));
			Image overImg = Base.getImage(baseImage + arrangement.overButtonString + ".png",this);
			if (overImg != null) {
				overImg = arrangement.scaleImage(overImg);
				setRolloverIcon(new ImageIcon(overImg));
				setRolloverEnabled(true);
			}
			Image downImg = Base.getImage(baseImage + arrangement.pressedButtonString + ".png",this);
			if (downImg == null) { downImg = overImg; }
			if (downImg != null) {
				downImg = arrangement.scaleImage(downImg);
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
			setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
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
	private JButton createJogButton(String root, String tooltip, ButtonArrangement arrangement) {
		JogButton b = new JogButton(root,tooltip,arrangement);
		b.addActionListener(this);
		b.addMouseListener(this);
		b.setActionCommand(root);
		return b;
	}
	private JButton createJogButton(String root, String tooltip) {
		return createJogButton(root,tooltip,new OldArrangement());
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
	private JButton createJogButton(String root, String tooltip, ButtonArrangement arrangement, String action) {
		JButton button = createJogButton(root,tooltip,arrangement);
		button.setActionCommand(action);
		return button;
	}
	private JButton createJogButton(String root, String tooltip, String action) {
		return createJogButton(root,tooltip,new OldArrangement(),action);
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
		JPanel positionPanel = new JPanel(new MigLayout("flowy"));
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
			parent.add(slider);
//			field.setMinimumSize(new Dimension(75, 22));
			field.setColumns(5);
			field.setEnabled(true);
			field.setText(Integer.toString(currentFeedrate));
			field.addFocusListener(this);
			field.addActionListener(this);
			parent.add(field);
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
	private JPanel makeRotationPanel(AxisId axis, ButtonArrangement arrangement) {
		JButton cwButton = createJogButton("CW", "Jog "+axis.name()+" axis in clockwise direction", axis.name()+"+");
		JButton ccwButton = createJogButton("CCW", "Jog "+axis.name()+" axis in counterclockwise direction", axis.name()+"-");
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
		
//		OnboardParameters onboardParam =  (OnboardParameters)machine.getDriver();
		
//		Set<AxisId> invertedAxes = EnumSet.noneOf(AxisId.class);
//		if(onboardParam != null )
//		{
//			invertedAxes = onboardParam.getInvertedAxes();
//		}
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
//		
//		MachineType machineType = machine.getMachineType();
//		
//		JButton xPlusButton = createJogButton("X+", "Jog X axis in positive direction", machineType, "X+");
//		JButton xMinusButton = createJogButton("X-", "Jog X axis in negative direction", machineType, "X-");
//		JButton yPlusButton = createJogButton("Y+", "Jog Y axis in positive direction", machineType, "Y+");
//		JButton yMinusButton = createJogButton("Y-", "Jog Y axis in negative direction", machineType, "Y-");
//		JButton panicButton = createJogButton("panic","Emergency stop","Stop");
//
//		JPanel jogButtonPanel = new JPanel(new MigLayout("nogrid, ins 0"));
//		JPanel xyPanel = new JPanel(new MigLayout("ins 0","[]2[]2[]","[]2[]2[]"));
//        //xyzPanel.add(zCenterButton, );
//		xyPanel.add(yPlusButton, "skip 1,wrap,growx,growy");
//		xyPanel.add(xMinusButton,"growx,growy");
//		if(this.machine.getDriver().hasEmergencyStop())
//			xyPanel.add(panicButton,"growx,growy");
//		
//		xyPanel.add(xPlusButton,"growx,growy,wrap");
//		xyPanel.add(yMinusButton, "skip 1,growx,growy,wrap");
//		jogButtonPanel.add(xyPanel);
//
//		if (axes.contains(AxisId.Z)) {
//			JPanel zPanel = new JPanel(new MigLayout("flowy"));
////			if (invertedAxes.contains(AxisId.Z) )
////			{
////				JButton zPlusButton = createJogButton("DownZ+", "Jog Z axis in positive direction", machineType, "Z+");
////				JButton zMinusButton = createJogButton("UpZ-", "Jog Z axis in negative direction", machineType, "Z-");
////				zPanel.add(zMinusButton);
////				zPanel.add(zPlusButton);
////			}
////			else {
//				JButton zMinusButton = createJogButton("Z-", "Jog Z axis in negative direction", machineType, "Z-");
//				JButton zPlusButton = createJogButton("Z+", "Jog Z axis in positive direction", machineType, "Z+");
//				zPanel.add(zPlusButton);
//				zPanel.add(zMinusButton);
//				
////			}
//			jogButtonPanel.add(zPanel,"wrap");
//		}
//		if (axes.contains(AxisId.A)) {
//			jogButtonPanel.add(makeRotationPanel(AxisId.A));
//		}		
//		if (axes.contains(AxisId.B)) {
//			jogButtonPanel.add(makeRotationPanel(AxisId.B));
//		}

		// create the xyfeedrate panel
		JPanel feedratePanel = new JPanel(new MigLayout());

		if (axes.contains(AxisId.X) || axes.contains(AxisId.Y)) {
			new FeedrateControl("XY Speed",AxisId.X,feedratePanel);
		}

		if (axes.contains(AxisId.Z)) {
			new FeedrateControl("Z Speed",AxisId.Z,feedratePanel);
		}
		if (axes.contains(AxisId.A)) {
			new FeedrateControl("A Speed",AxisId.A,feedratePanel);
		}
		if (axes.contains(AxisId.B)) {
			new FeedrateControl("B Speed",AxisId.B,feedratePanel);
		}
		// add it all to our jog panel
		add(new ReplicatorArrangement().getButtonPanel(machine), "split");
		add(buildPositionPanel(), "growx, wrap");
		add(feedratePanel, "growx");

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
