package replicatorg.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.Version;
import replicatorg.drivers.gen3.Sanguino3GDriver;
import replicatorg.machine.model.ToolModel;

public class ExtruderOnboardParameters extends JPanel {
	private static final long serialVersionUID = 6353987389397209816L;
	private OnboardParameters target;
	
	// Float gui objects show at least 2 places, max 8 places for clarity it's a float
    private static final NumberFormat floatFormat = (NumberFormat) Base.getLocalFormat().clone();
    {
        floatFormat.setMaximumFractionDigits(8);
        floatFormat.setMinimumFractionDigits(2);
    }
	
	interface Commitable {
		public void commit();
		// In a sane universe, this would be called "validate".  In a sane universe
		// where Java actually implemented inheritance in a sane and happy manner.
		public boolean isCommitable();
	}

	final int FIELD_WIDTH = 10;
	
	class ThermistorTablePanel extends JPanel implements Commitable {
		private static final long serialVersionUID = 7765098486598830410L;

		private JFormattedTextField betaField = new JFormattedTextField(floatFormat);
		private JFormattedTextField r0Field = new JFormattedTextField(floatFormat);
		private JFormattedTextField t0Field = new JFormattedTextField(floatFormat);
		// Toolhead or Heated Platform?
		private final int which;
		private final ToolModel tool;
		ThermistorTablePanel(int which, String titleText, ToolModel tool) {
			super(new MigLayout());
			this.which = which;
			this.tool = tool;
			setBorder(BorderFactory.createTitledBorder(titleText));
			betaField.setColumns(FIELD_WIDTH);
			r0Field.setColumns(FIELD_WIDTH);
			t0Field.setColumns(FIELD_WIDTH);
			
			double beta = target.getBeta(which, tool.getIndex());
			if (beta == -1)
				beta = 4066;
			betaField.setValue((int)beta);
			add(new JLabel("Beta"));
			add(betaField,"wrap");

			double r0 = target.getR0(which, tool.getIndex());
			if (r0 == -1)
				r0 = 100000;
			r0Field.setValue((int)r0);
			add(new JLabel("Thermistor Resistance"));
			add(r0Field,"wrap");

			double t0 = target.getT0(which, tool.getIndex());
			if (t0 == -1)
				t0 = 25;
			t0Field.setValue((int)t0);
			add(new JLabel("Base Temperature"));
			add(t0Field,"wrap");
		}

		public void commit() {
			int beta = ((Number)betaField.getValue()).intValue();
			int r0 = ((Number)r0Field.getValue()).intValue();
			int t0 = ((Number)t0Field.getValue()).intValue();
			target.createThermistorTable(which,r0,t0,beta,tool.getIndex());
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
		
		private JFormattedTextField stopMsField = new JFormattedTextField(floatFormat);
		private JFormattedTextField reverseMsField = new JFormattedTextField(floatFormat);
		private JFormattedTextField forwardMsField = new JFormattedTextField(floatFormat);
		private JFormattedTextField triggerMsField = new JFormattedTextField(floatFormat);
		private final ToolModel tool;

		BackoffPanel(ToolModel tool) {
			this.tool = tool;
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
			OnboardParameters.BackoffParameters bp = target.getBackoffParameters(tool.getIndex());
			stopMsField.setValue(bp.stopMs);
			reverseMsField.setValue(bp.reverseMs);
			forwardMsField.setValue(bp.forwardMs);
			triggerMsField.setValue(bp.triggerMs);
		}

		public void commit() {
			OnboardParameters.BackoffParameters bp = new OnboardParameters.BackoffParameters();
			bp.forwardMs = ((Number)forwardMsField.getValue()).intValue();
			bp.reverseMs = ((Number)reverseMsField.getValue()).intValue();
			bp.stopMs = ((Number)stopMsField.getValue()).intValue();
			bp.triggerMs = ((Number)triggerMsField.getValue()).intValue();
			target.setBackoffParameters(bp, tool.getIndex());
		}
		
		public boolean isCommitable() {
			return true;
		}
	}

	private class ExtraFeaturesPanel extends JPanel implements Commitable {
		private JCheckBox swapMotors;
		private JComboBox extCh, hbpCh, abpCh;
		private OnboardParameters.ExtraFeatures ef;
		
		private final ToolModel tool;
		ExtraFeaturesPanel(ToolModel tool) {
			this.tool = tool;
			setLayout(new MigLayout());
			ef = target.getExtraFeatures(tool.getIndex());
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
			target.setExtraFeatures(ef, tool.getIndex());
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
	    private NumberFormat eightPlaces = (NumberFormat) floatFormat.clone();
	    {
	    	eightPlaces.setMaximumFractionDigits(8);
	    }

	    private JFormattedTextField pField = new JFormattedTextField(floatFormat);
		private JFormattedTextField iField = new JFormattedTextField(eightPlaces);
		private JFormattedTextField dField = new JFormattedTextField(floatFormat);
		private final int which;
		private final ToolModel tool;
		PIDPanel(int which, String name, ToolModel tool) {
			this.which = which;
			this.tool = tool;
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
			OnboardParameters.PIDParameters pp = target.getPIDParameters(which, tool.getIndex());
			pField.setValue(pp.p);
			iField.setValue(pp.i);
			dField.setValue(pp.d);
		}

		public void commit() {
			OnboardParameters.PIDParameters pp = new OnboardParameters.PIDParameters();
			pp.p = ((Number)pField.getValue()).floatValue();
			pp.i = ((Number)iField.getValue()).floatValue();
			pp.d = ((Number)dField.getValue()).floatValue();
			target.setPIDParameters(which,pp, tool.getIndex());
		}
		
		public boolean isCommitable() {
			return true;
		}
	}
	
	class RegulatedCoolingFan extends JPanel implements Commitable {
		private static final long serialVersionUID = 7765098486598830410L;
		private JCheckBox coolingFanEnabled;
		
		private JFormattedTextField coolingFanSetpoint = new JFormattedTextField(floatFormat);
		
		private final ToolModel tool;
		RegulatedCoolingFan(ToolModel tool) {
			super(new MigLayout());	
			
			this.tool = tool;
			
			coolingFanEnabled = new JCheckBox("Enable regulated cooling fan (stepper extruders only)",
					target.getCoolingFanEnabled(tool.getIndex()));
			add(coolingFanEnabled,"growx,wrap");
			
			coolingFanSetpoint.setColumns(FIELD_WIDTH);
	
			coolingFanSetpoint.setValue((int)target.getCoolingFanSetpoint(tool.getIndex()));
			add(new JLabel("Setpoint (C)"));
			add(coolingFanSetpoint,"wrap");

		}

		public void commit() {
			boolean enabled = coolingFanEnabled.isSelected();   
			int setpoint = ((Number)coolingFanSetpoint.getValue()).intValue();
			target.setCoolingFanParameters(enabled, setpoint, tool.getIndex());
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

	public ExtruderOnboardParameters(OnboardParameters target, ToolModel tool) {
		this.target = target;

		Version v = new Version(0,0);
		if (target instanceof Sanguino3GDriver) {
			v = ((Sanguino3GDriver)target).getToolVersion();
		}
		
		setLayout(new MigLayout());
		
		ThermistorTablePanel ttp;
		ttp = new ThermistorTablePanel(0,"Extruder thermistor", tool);
		add(ttp);
		commitList.add(ttp);
		ttp = new ThermistorTablePanel(1,"Heated build platform thermistor", tool);
		add(ttp,"wrap");
		commitList.add(ttp);
		
		if (!v.atLeast(new Version(2,5))) {
			BackoffPanel backoffPanel = new BackoffPanel(tool);
			add(backoffPanel,"span 2,growx,wrap");
			commitList.add(backoffPanel);
		}

		if (v.atLeast(new Version(2,5))) {
			ExtraFeaturesPanel efp = new ExtraFeaturesPanel(tool);
			add(efp,"span 2,growx,wrap");
			commitList.add(efp);
		}
		
		PIDPanel pidPanel = new PIDPanel(0,"Extruder", tool);
		add(pidPanel,"growx");
		commitList.add(pidPanel);
		
		if (v.atLeast(new Version(2,4))) {
			PIDPanel pp = new PIDPanel(1,"Heated build platform", tool);
			add(pp,"growx,wrap");
			commitList.add(pp);
		}

		if (v.atLeast(new Version(2,9))) {
			RegulatedCoolingFan rcf = new RegulatedCoolingFan(tool);
			add(rcf,"span 2,growx,wrap");
			commitList.add(rcf);
		}
			
		
		add(makeButtonPanel(),"span 2,newline");
	}
}
