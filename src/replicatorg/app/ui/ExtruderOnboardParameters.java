package replicatorg.app.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.Version;
import replicatorg.drivers.gen3.Sanguino3GDriver;

public class ExtruderOnboardParameters extends JPanel {
	private static final long serialVersionUID = 6353987389397209816L;
	private OnboardParameters target;
	
	interface Commitable {
		public void commit();
		// In a sane universe, this would be called "validate".  In a sane universe
		// where Java actually implemented inheritance in a sane and happy manner.
		public boolean isCommitable();
	}

	final int FIELD_WIDTH = 10;
	
	class ThermistorTablePanel extends JPanel implements Commitable {
		private static final long serialVersionUID = 7765098486598830410L;
		private JTextField betaField = new JTextField();
		private JTextField r0Field = new JTextField();
		private JTextField t0Field = new JTextField();
		private int which;
		ThermistorTablePanel(int which, String titleText) {
			super(new MigLayout());
			this.which = which;
			setBorder(BorderFactory.createTitledBorder(titleText));
			betaField.setColumns(FIELD_WIDTH);
			r0Field.setColumns(FIELD_WIDTH);
			t0Field.setColumns(FIELD_WIDTH);
			
			double beta = target.getBeta(which);
			if (beta == -1) { beta = 4066; }
			betaField.setText(Integer.toString((int)beta));
			add(new JLabel("Beta"));
			add(betaField,"wrap");

			double r0 = target.getR0(which);
			if (r0 == -1) { r0 = 100000; }
			r0Field.setText(Integer.toString((int)r0));
			add(new JLabel("Thermistor Resistance"));
			add(r0Field,"wrap");

			double t0 = target.getT0(which);
			if (t0 == -1) { t0 = 25; }
			t0Field.setText(Integer.toString((int)t0));
			add(new JLabel("Base Temperature"));
			add(t0Field,"wrap");
		}

		public void commit() {
			int beta = Integer.parseInt(betaField.getText());
			int r0 = Integer.parseInt(r0Field.getText());
			int t0 = Integer.parseInt(t0Field.getText());
			target.createThermistorTable(which,r0,t0,beta);
		}
		
		public boolean isCommitable() {
			return true;
		}
	}

	Vector<Commitable> commitList = new Vector<Commitable>();
	
	private boolean commit() {		
		for (Commitable c : commitList) {
			if (!c.isCommitable()) {
				return false;
			}
		}
		
		for (Commitable c : commitList) {
			c.commit();
		}
		JOptionPane.showMessageDialog(this,
				"Changes will not take effect until the extruder board is reset.  You can \n" +
				"do this by turning your machine off and then on, or by disconnecting and \n" +
				"reconnecting the extruder cable.  Make sure you don't still have a USB2TTL \n" +
				"cable attached to the extruder controller, as the cable will keep the board \n" +
				"from resetting.",
			    "Extruder controller reminder",
			    JOptionPane.INFORMATION_MESSAGE);
		return true;
	}

	private class BackoffPanel extends JPanel implements Commitable {
		private static final long serialVersionUID = 6593800743174557032L;
		private JTextField stopMsField = new JTextField();
		private JTextField reverseMsField = new JTextField();
		private JTextField forwardMsField = new JTextField();
		private JTextField triggerMsField = new JTextField();
		BackoffPanel() {
			setLayout(new MigLayout());
			setBorder(BorderFactory.createTitledBorder("Reversal parameters"));
			stopMsField.setColumns(FIELD_WIDTH);
			reverseMsField.setColumns(FIELD_WIDTH);
			forwardMsField.setColumns(FIELD_WIDTH);
			triggerMsField.setColumns(FIELD_WIDTH);

			add(new JLabel("Time to pause (ms)"));
			add(stopMsField,"wrap");
			add(new JLabel("Time to reverse (ms)"));
			add(reverseMsField,"wrap");
			add(new JLabel("Time to advance (ms)"));
			add(forwardMsField,"wrap");
			add(new JLabel("Min. extrusion time before reversal (ms)"));
			add(triggerMsField,"wrap");
			OnboardParameters.BackoffParameters bp = target.getBackoffParameters();
			stopMsField.setText(Integer.toString(bp.stopMs));
			reverseMsField.setText(Integer.toString(bp.reverseMs));
			forwardMsField.setText(Integer.toString(bp.forwardMs));
			triggerMsField.setText(Integer.toString(bp.triggerMs));
		}

		public void commit() {
			OnboardParameters.BackoffParameters bp = new OnboardParameters.BackoffParameters();
			bp.forwardMs = Integer.parseInt(forwardMsField.getText());
			bp.reverseMs = Integer.parseInt(reverseMsField.getText());
			bp.stopMs = Integer.parseInt(stopMsField.getText());
			bp.triggerMs = Integer.parseInt(triggerMsField.getText());
			target.setBackoffParameters(bp);
		}
		
		public boolean isCommitable() {
			return true;
		}
	}

	private class ExtraFeaturesPanel extends JPanel implements Commitable {
		private JCheckBox swapMotors;
		private JComboBox extCh, hbpCh, abpCh;
		private OnboardParameters.ExtraFeatures ef;
		ExtraFeaturesPanel() {
			setLayout(new MigLayout());
			ef = target.getExtraFeatures();
			swapMotors = new JCheckBox("Use 2A/2B to drive DC motor instead of 1A/1B", ef.swapMotorController);
			add(swapMotors,"span 3,growx,wrap");
			Vector<String> choices = new Vector<String>();
			choices.add("Channel A");
			choices.add("Channel B");
			choices.add("Channel C");
			extCh = new JComboBox(choices);
			extCh.setSelectedIndex(ef.heaterChannel);
			add(new JLabel("Extruder heater uses:"));
			add(extCh);
			add(new JLabel("(default ch. B)"),"wrap");
			hbpCh = new JComboBox(choices);
			hbpCh.setSelectedIndex(ef.hbpChannel);
			add(new JLabel("Platform heater uses:"));
			add(hbpCh);
			add(new JLabel("(default ch. A)"),"wrap");
			abpCh = new JComboBox(choices);
			abpCh.setSelectedIndex(ef.abpChannel);
			add(new JLabel("ABP motor uses:"));
			add(abpCh);
			add(new JLabel("(default ch. C)"),"wrap");
		}

		public void commit() {
			ef.swapMotorController = swapMotors.isSelected();
			ef.heaterChannel = extCh.getSelectedIndex();
			ef.hbpChannel = hbpCh.getSelectedIndex();
			ef.abpChannel = abpCh.getSelectedIndex();
			target.setExtraFeatures(ef);
		}
		
		public boolean isCommitable() {
			int a = extCh.getSelectedIndex();
			int b = hbpCh.getSelectedIndex();
			int c = abpCh.getSelectedIndex();
			if (a == b || b == c || a == c) {
				JOptionPane.showMessageDialog(this, "Two or more features are using the same mosfet channel!", 
						"Channel conflict", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			return true;
		}
	}
	
	private class PIDPanel extends JPanel implements Commitable {
		private JTextField pField = new JTextField();
		private JTextField iField = new JTextField();
		private JTextField dField = new JTextField();
		private int which;
		PIDPanel(int which, String name) {
			this.which = which;
			setLayout(new MigLayout());
			setBorder(BorderFactory.createTitledBorder(name+" PID parameters"));
			pField.setColumns(FIELD_WIDTH);
			iField.setColumns(FIELD_WIDTH);
			dField.setColumns(FIELD_WIDTH);

			add(new JLabel("P parameter"));
			add(pField,"wrap");
			add(new JLabel("I parameter"));
			add(iField,"wrap");
			add(new JLabel("D parameter"));
			add(dField,"wrap");
			OnboardParameters.PIDParameters pp = target.getPIDParameters(which);
			pField.setText(Float.toString(pp.p));
			iField.setText(Float.toString(pp.i));
			dField.setText(Float.toString(pp.d));
		}

		public void commit() {
			OnboardParameters.PIDParameters pp = new OnboardParameters.PIDParameters();
			pp.p = Float.parseFloat(pField.getText());
			pp.i = Float.parseFloat(iField.getText());
			pp.d = Float.parseFloat(dField.getText());
			target.setPIDParameters(which,pp);
		}
		
		public boolean isCommitable() {
			return true;
		}
	}
	
	class RegulatedCoolingFan extends JPanel implements Commitable {
		private static final long serialVersionUID = 7765098486598830410L;
		private JCheckBox coolingFanEnabled;
		private JTextField coolingFanSetpoint = new JTextField();
		RegulatedCoolingFan() {
			super(new MigLayout());			
			coolingFanEnabled = new JCheckBox("Enable regulated cooling fan (stepper extruders only)",
					target.getCoolingFanEnabled());
			add(coolingFanEnabled,"growx,wrap");
			
			coolingFanSetpoint.setColumns(FIELD_WIDTH);
	
			coolingFanSetpoint.setText(Integer.toString((int)target.getCoolingFanSetpoint()));
			add(new JLabel("Setpoint (C)"));
			add(coolingFanSetpoint,"wrap");

		}

		public void commit() {
			boolean enabled = coolingFanEnabled.isSelected();   
			int setpoint = Integer.parseInt(coolingFanSetpoint.getText());
			target.setCoolingFanParameters(enabled, setpoint);
		}
		
		public boolean isCommitable() {
			return true;
		}
	}
	
	private JPanel makeButtonPanel() {
		JPanel panel = new JPanel(new MigLayout());
		JButton commitButton = new JButton("Commit Changes");
		panel.add(commitButton);
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (ExtruderOnboardParameters.this.commit()) {
				}
			}
		});
		return panel;
	}

	public ExtruderOnboardParameters(OnboardParameters target) {
		this.target = target;

		Version v = new Version(0,0);
		if (target instanceof Sanguino3GDriver) {
			v = ((Sanguino3GDriver)target).getToolVersion();
		}

		setLayout(new MigLayout());
		ThermistorTablePanel ttp;
		ttp = new ThermistorTablePanel(0,"Extruder thermistor");
		add(ttp);
		commitList.add(ttp);
		ttp = new ThermistorTablePanel(1,"Heated build platform thermistor");
		add(ttp,"wrap");
		commitList.add(ttp);
		if (!v.atLeast(new Version(2,5))) {
			BackoffPanel backoffPanel = new BackoffPanel();
			add(backoffPanel,"span 2,growx,wrap");
			commitList.add(backoffPanel);
		}
		if (v.atLeast(new Version(2,5))) {
			ExtraFeaturesPanel efp = new ExtraFeaturesPanel();
			add(efp,"span 2,growx,wrap");
			commitList.add(efp);
		}
		PIDPanel pidPanel = new PIDPanel(0,"Extruder");
		add(pidPanel,"growx");
		commitList.add(pidPanel);
		if (v.atLeast(new Version(2,4))) {
			PIDPanel pp = new PIDPanel(1,"Heated build platform");
			add(pp,"growx,wrap");
			commitList.add(pp);
		}

		if (v.atLeast(new Version(2,9))) {
			RegulatedCoolingFan rcf = new RegulatedCoolingFan();
			add(rcf,"span 2,growx,wrap");
			commitList.add(rcf);
		}
		
		add(makeButtonPanel(),"span 2,newline");

	}
}
