/*
 * @author Erik de Bruijn
 * For Ultimaker & RepRaps with 5D control, allows you to tune the printer with 5D controls
 */
package replicatorg.app.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.RealtimeControl;
import replicatorg.machine.MachineInterface;

public class RealtimePanel extends JFrame implements ChangeListener, WindowListener {
	
	private static final long serialVersionUID = -6193762098842247273L;

	protected MachineInterface machine;

	protected Driver driver;

	private static RealtimePanel instance = null;

	// GUI items
	JPanel mainPanel;
	ControlSlider feedrateControl, travelFeedrateControl, extrusionControl;

	public RealtimePanel(MachineInterface machine2) {
		super("Real time control and tuning");
		Image icon = Base.getImage("images/icon.gif", this);
		setIconImage(icon);
		
		machine = machine2;
		driver = machine.getDriver();

		((RealtimeControl) driver).enableRealtimeControl(true);
		
		// create all our GUI interfaces
		JPanel speedPanel = new JPanel();
		JPanel extrusionPanel = new JPanel();
		add(new JLabel("Build speed (during extrusion)"));

		// Speed
		feedrateControl = new ControlSlider("Feedrate","%",5,800,100,speedPanel);
		//feedrateControl.getSlider().setMajorTickSpacing(10);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put( new Integer( 10 ), new JLabel("Slow") );
		labelTable.put( new Integer( 100 ), new JLabel("") );
		labelTable.put( new Integer( 300 ), new JLabel("Fast") );
		labelTable.put( new Integer( 500 ), new JLabel("Insane!") );
		feedrateControl.slider.setLabelTable( labelTable );
		
//		add(new JLabel("Travel feedrate (no extrusion"),"growx,wrap");
		travelFeedrateControl = new ControlSlider("Travel feedrate","%",5,800,100,speedPanel);
		travelFeedrateControl.slider.setLabelTable( labelTable );

		// Extrusion
		extrusionPanel.add(new JLabel("Extrusion"),"growx,wrap");
		extrusionControl = new ControlSlider("Material muliplier","%",5,500,100,extrusionPanel);
		// TODO: extrusion scaling is not implemented in the driver yet.
		extrusionControl.slider.setEnabled(false);
		extrusionControl.field.setEnabled(false);

		mainPanel = new JPanel();
		mainPanel.setLayout(new MigLayout());
		mainPanel.add(speedPanel,"flowy,wrap");
		mainPanel.add(extrusionPanel,"flowy,wrap");
		
		new SpeedLimit(mainPanel);
		
		// Show comms debug checkbox
		JCheckBox showCommsDebug = new JCheckBox("Show communications");
		if(((RealtimeControl) driver).getDebugLevel()>=2)
			showCommsDebug.setSelected(true);
		
		showCommsDebug.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(((JCheckBox) e.getSource()).isSelected()) {
					((RealtimeControl) driver).setDebugLevel(2);
				} else {
					((RealtimeControl) driver).setDebugLevel(1);
				}
			}
		}
		);
		mainPanel.add(showCommsDebug,"flowy,wrap");
		add(mainPanel);
	}
	
	public static synchronized RealtimePanel getRealtimePanel(MachineInterface machine2) {
		if (instance == null) {
			instance = new RealtimePanel(machine2);
		} else {
			if (instance.machine != machine2) {
				instance.dispose();
				instance = new RealtimePanel(machine2);
			}
		}
		return instance;
	}
	
	private class ControlSlider implements ActionListener, FocusListener, ChangeListener {
		final JSlider slider;
		final JTextField field;
	
		private ControlSlider(String labelText, String unitText, int minVal, int maxVal, int defaultVal, JPanel myParent) {
			JPanel sliderPanel = new JPanel();
			slider = new JSlider(JSlider.HORIZONTAL);
			field = new JTextField();
			sliderPanel.add(new JLabel(labelText));
			
//			int maxFeedrate = (int)machine.getModel().getMaximumFeedrates().axis(axis);
//			int currentFeedrate = Math.min(maxFeedrate, Base.preferences.getInt(getPrefName(),480));
			slider.setMinimum(minVal);
			slider.setMaximum(maxVal);
			slider.setValue(defaultVal);
			slider.addChangeListener(this);
			slider.setPaintLabels(true);
			slider.setPaintTicks(true);
			slider.setMajorTickSpacing(100);
			sliderPanel.add(slider,"growx");
			field.setMinimumSize(new Dimension(75, 22));
			field.setEnabled(true);
			field.setText(Integer.toString(defaultVal));
			field.addFocusListener(this);
			field.addActionListener(this);
			sliderPanel.add(field,"growx,wrap");
			sliderPanel.add(new JLabel(unitText),"growx,wrap");
			
			myParent.add(sliderPanel,"wrap");
		}
		public JSlider getSlider() {
			return slider;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void focusGained(FocusEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void focusLost(FocusEvent e) {
			try {
				Object s = e.getSource();
				if(s == field) {
					int val = Integer.parseInt(field.getText());
					slider.setValue(val);
				}
			} catch (Exception exception) {	}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			Object s = e.getSource();
			if(s instanceof JSlider)
			{
				int val = ((JSlider) s).getValue();
				//Base.logger.info("Slider value: "+val);
				field.setText(Integer.toString(val));
				if(s == feedrateControl.getSlider()) {
					((RealtimeControl) driver).setFeedrateMultiplier((double) val/100);
				} else if(s == extrusionControl.getSlider()) {
					((RealtimeControl) driver).setExtrusionMultiplier((double) val/100);
				} else if(s == travelFeedrateControl.getSlider()) {
					((RealtimeControl) driver).setTravelFeedrateMultiplier((double) val/100);
				}
			}
		}
	}
	public void actionPerformed(ActionEvent e) {
		String s = e.getActionCommand();
	}
	
	@Override
	public void stateChanged(ChangeEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}
/*
	@Override
	public void machineProgress(MachineProgressEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void machineStateChanged(MachineStateChangeEvent evt) {
		// TODO Auto-generated method stub

	}

	@Override
	public void toolStatusChanged(MachineToolStatusEvent event) {
		// TODO Auto-generated method stub

	}*/
	// A field that allows you to specify the maximum feedrate.
	private class SpeedLimit implements FocusListener {
		JTextField speedLimitField;
		public SpeedLimit(JPanel myPanel) {
			speedLimitField = new JTextField();
			double frLimit = ((RealtimeControl) driver).getFeedrateLimit();
			speedLimitField.setText(""+frLimit);
			speedLimitField.setSize(20,220);
			speedLimitField.addFocusListener(this);
			myPanel.add(new JLabel("Speed limit: "),"");
			myPanel.add(speedLimitField,"flowy,wrap");
		}
		public Component getComponent() {
			return speedLimitField;
		}
		public void focusLost(FocusEvent e) {
			double limit = Double.parseDouble(((JTextField) e.getSource()).getText());
			Base.logger.info("Setting feedrate limit to "+limit);
			((RealtimeControl) driver).setFeedrateLimit(limit);
		}
		public void focusGained(FocusEvent e) {
		}
	}
	
}
