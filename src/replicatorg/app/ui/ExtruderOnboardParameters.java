package replicatorg.app.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFormattedTextField;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.Version;
import replicatorg.drivers.gen3.Sanguino3GDriver;

public class ExtruderOnboardParameters extends JFrame {
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
	    private NumberFormat threePlaces = Base.getLocalFormat();
	    {
	        threePlaces.setMaximumFractionDigits(3);
	    }

		private JFormattedTextField betaField = new JFormattedTextField(threePlaces);
		private JFormattedTextField r0Field = new JFormattedTextField(threePlaces);
		private JFormattedTextField t0Field = new JFormattedTextField(threePlaces);
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
			betaField.setValue((int)beta);
			add(new JLabel("Beta"));
			add(betaField,"wrap");

			double r0 = target.getR0(which);
			if (r0 == -1) { r0 = 100000; }
			r0Field.setValue((int)r0);
			add(new JLabel("Thermistor Resistance"));
			add(r0Field,"wrap");

			double t0 = target.getT0(which);
			if (t0 == -1) { t0 = 25; }
			t0Field.setValue((int)t0);
			add(new JLabel("Base Temperature"));
			add(t0Field,"wrap");
		}

		public void commit() {
			NumberFormat nf = Base.getLocalFormat();
			try {
				int beta = nf.parse(betaField.getText()).intValue();
				int r0 = nf.parse(r0Field.getText()).intValue();
				int t0 = nf.parse(t0Field.getText()).intValue();
				target.createThermistorTable(which,r0,t0,beta);
			} catch (ParseException pe) {
				Base.logger.log(Level.WARNING,"Could not parse value!",pe);
				JOptionPane.showMessageDialog(this, "Error parsing value: "+pe.getMessage()+"\nPlease try again.", "Could not parse value", JOptionPane.ERROR_MESSAGE);
				return;
			}
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
	    private NumberFormat threePlaces = Base.getLocalFormat();
	    {
	        threePlaces.setMaximumFractionDigits(3);
	    }
		private JFormattedTextField stopMsField = new JFormattedTextField(threePlaces);
		private JFormattedTextField reverseMsField = new JFormattedTextField(threePlaces);
		private JFormattedTextField forwardMsField = new JFormattedTextField(threePlaces);
		private JFormattedTextField triggerMsField = new JFormattedTextField(threePlaces);
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
			stopMsField.setValue(bp.stopMs);
			reverseMsField.setValue(bp.reverseMs);
			forwardMsField.setValue(bp.forwardMs);
			triggerMsField.setValue(bp.triggerMs);
		}

		public void commit() {
			NumberFormat nf = Base.getLocalFormat();
			OnboardParameters.BackoffParameters bp = new OnboardParameters.BackoffParameters();
			try {
				bp.forwardMs = nf.parse(forwardMsField.getText()).intValue();
				bp.reverseMs = nf.parse(reverseMsField.getText()).intValue();
				bp.stopMs = nf.parse(stopMsField.getText()).intValue();
				bp.triggerMs = nf.parse(triggerMsField.getText()).intValue();
				target.setBackoffParameters(bp);
			} catch (ParseException pe) {
				Base.logger.log(Level.WARNING,"Could not parse value!",pe);
				JOptionPane.showMessageDialog(this, "Error parsing value: "+pe.getMessage()+"\nPlease try again.", "Could not parse value", JOptionPane.ERROR_MESSAGE);
				return;
			}
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
	    private NumberFormat threePlaces = Base.getLocalFormat();
	    {
	        threePlaces.setMaximumFractionDigits(3);
	    }
	    private NumberFormat eightPlaces = Base.getLocalFormat();
	    {
	    	eightPlaces.setMaximumFractionDigits(8);
	    }

	    private JFormattedTextField pField = new JFormattedTextField(threePlaces);
		private JFormattedTextField iField = new JFormattedTextField(eightPlaces);
		private JFormattedTextField dField = new JFormattedTextField(threePlaces);
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
			pField.setValue(pp.p);
			iField.setValue(pp.i);
			dField.setValue(pp.d);
		}

		public void commit() {
			OnboardParameters.PIDParameters pp = new OnboardParameters.PIDParameters();
			NumberFormat nf = Base.getLocalFormat();
			try{ 
				pp.p = nf.parse(pField.getText()).floatValue();
				pp.i = nf.parse(iField.getText()).floatValue();
				pp.d = nf.parse(dField.getText()).floatValue();
				target.setPIDParameters(which,pp);
			} catch (ParseException pe) {
				Base.logger.log(Level.WARNING,"Could not parse value!",pe);
				JOptionPane.showMessageDialog(this, "Error parsing value: "+pe.getMessage()+"\nPlease try again.", "Could not parse value", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		public boolean isCommitable() {
			return true;
		}
	}
	
	class RegulatedCoolingFan extends JPanel implements Commitable {
		private static final long serialVersionUID = 7765098486598830410L;
		private JCheckBox coolingFanEnabled;
	    private NumberFormat threePlaces = Base.getLocalFormat();
	    {
	        threePlaces.setMaximumFractionDigits(3);
	    }
		private JFormattedTextField coolingFanSetpoint = new JFormattedTextField(threePlaces);
		RegulatedCoolingFan() {
			super(new MigLayout());			
			coolingFanEnabled = new JCheckBox("Enable regulated cooling fan (stepper extruders only)",
					target.getCoolingFanEnabled());
			add(coolingFanEnabled,"growx,wrap");
			
			coolingFanSetpoint.setColumns(FIELD_WIDTH);
	
			coolingFanSetpoint.setValue((int)target.getCoolingFanSetpoint());
			add(new JLabel("Setpoint (C)"));
			add(coolingFanSetpoint,"wrap");

		}

		public void commit() {
			NumberFormat nf = Base.getLocalFormat();
			boolean enabled = coolingFanEnabled.isSelected();   
			try {
				int setpoint = nf.parse(coolingFanSetpoint.getText()).intValue();
				target.setCoolingFanParameters(enabled, setpoint);
			} catch (ParseException pe) {
				Base.logger.log(Level.WARNING,"Could not parse value!",pe);
				JOptionPane.showMessageDialog(this, "Error parsing value: "+pe.getMessage()+"\nPlease try again.", "Could not parse value", JOptionPane.ERROR_MESSAGE);
				return;
			}
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
					ExtruderOnboardParameters.this.dispose();
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ExtruderOnboardParameters.this.dispose();
			}
		});
		panel.add(cancelButton);
		return panel;
	}

	public ExtruderOnboardParameters(OnboardParameters target) {
		super("Update onboard extruder options");
		this.target = target;

		Version v = new Version(0,0);
		if (target instanceof Sanguino3GDriver) {
			v = ((Sanguino3GDriver)target).getToolVersion();
		}

		JPanel panel = new JPanel(new MigLayout());
		ThermistorTablePanel ttp;
		ttp = new ThermistorTablePanel(0,"Extruder thermistor");
		panel.add(ttp);
		commitList.add(ttp);
		ttp = new ThermistorTablePanel(1,"Heated build platform thermistor");
		panel.add(ttp,"wrap");
		commitList.add(ttp);
		if (!v.atLeast(new Version(2,5))) {
			BackoffPanel backoffPanel = new BackoffPanel();
			panel.add(backoffPanel,"span 2,growx,wrap");
			commitList.add(backoffPanel);
		}
		if (v.atLeast(new Version(2,5))) {
			ExtraFeaturesPanel efp = new ExtraFeaturesPanel();
			panel.add(efp,"span 2,growx,wrap");
			commitList.add(efp);
		}
		PIDPanel pidPanel = new PIDPanel(0,"Extruder");
		panel.add(pidPanel,"growx");
		commitList.add(pidPanel);
		if (v.atLeast(new Version(2,4))) {
			PIDPanel pp = new PIDPanel(1,"Heated build platform");
			panel.add(pp,"growx,wrap");
			commitList.add(pp);
		}

		if (v.atLeast(new Version(2,9))) {
			RegulatedCoolingFan rcf = new RegulatedCoolingFan();
			panel.add(rcf,"span 2,growx,wrap");
			commitList.add(rcf);
		}
		
		panel.add(makeButtonPanel(),"span 2,newline");
		add(panel);

		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2,
				(screen.height - getHeight()) / 2);

	}
}
