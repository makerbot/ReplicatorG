package replicatorg.app.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

import replicatorg.drivers.OnboardParameters;
import replicatorg.machine.model.Axis;

public class ExtruderOnboardParameters extends JFrame {
	private OnboardParameters target;
	
	class ThermistorTablePanel extends JPanel {
		private static final long serialVersionUID = 7765098486598830410L;
		private JTextField betaField = new JTextField();
		private JTextField r0Field = new JTextField();
		private JTextField t0Field = new JTextField();
		private int which;
		ThermistorTablePanel(int which, String titleText) {
			super(new MigLayout());
			this.which = which;
			setBorder(BorderFactory.createTitledBorder(titleText));
			final int FIELD_WIDTH = 20;
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

		void commit() {
			int beta = Integer.parseInt(betaField.getText());
			int r0 = Integer.parseInt(r0Field.getText());
			int t0 = Integer.parseInt(t0Field.getText());
			target.createThermistorTable(which,r0,t0,beta);
		}
	}

	private void commit() {
		for (ThermistorTablePanel p : thermistorTablePanels) {
			p.commit();
		}
		JOptionPane.showMessageDialog(this,
				"Changes will not take effect until the extruder board is reset.  You can \n" +
				"do this by turning your machine off and then on, or by disconnecting and \n" +
				"reconnecting the extruder cable.  Make sure you don't still have a USB2TTL \n" +
				"cable attached to the extruder controller, as the cable will keep the board \n" +
				"from resetting.",
			    "Extruder controller reminder",
			    JOptionPane.INFORMATION_MESSAGE);
	}

	private JPanel makeButtonPanel() {
		JPanel panel = new JPanel(new MigLayout());
		JButton commitButton = new JButton("Commit Changes");
		panel.add(commitButton);
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ExtruderOnboardParameters.this.commit();
				ExtruderOnboardParameters.this.dispose();				
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

	private List<ThermistorTablePanel> thermistorTablePanels = new LinkedList<ThermistorTablePanel>();
	
	public ExtruderOnboardParameters(OnboardParameters target) {
		super("Update onboard machine options");
		this.target = target;

		thermistorTablePanels.add(new ThermistorTablePanel(0,"Extruder thermistor"));
		thermistorTablePanels.add(new ThermistorTablePanel(1,"Heated build platform thermistor"));

		Box panel = Box.createVerticalBox();
		for (ThermistorTablePanel p : thermistorTablePanels) {
			panel.add(p);
		}
		panel.add(makeButtonPanel());
		add(panel);
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2,
				(screen.height - getHeight()) / 2);

	}
}
