package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import replicatorg.app.Base;
import replicatorg.app.MachineController;
import replicatorg.app.MachineControllerInterface;
import replicatorg.drivers.EstimationDriver;
import replicatorg.drivers.UsesSerial;
import replicatorg.drivers.Version;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

/**
 * The MachineStatusPanel displays the current state of the connected machine,
 * or a message informing the user that no connected machine can be found.
 * 
 * @author phooky
 * 
 */
public class MachineStatusPanel extends BGPanel implements MachineListener {
	private static final long serialVersionUID = -6944931245041870574L;

	protected MachineControllerInterface machine = null;

	protected JLabel label = new JLabel();
	protected JLabel smallLabel = new JLabel();
	protected JLabel tempLabel = new JLabel();

	protected double currentTemperature = -1;
	
	static final private Color BG_NO_MACHINE = new Color(0xff, 0x80, 0x60);
	static final private Color BG_READY = new Color(0x80, 0xff, 0x60);
	static final private Color BG_BUILDING = new Color(0xff, 0xef, 0x00); // process yellow

	// static final private Color BG_WAIT = new Color(0xff,0xff,0x60);

	MachineStatusPanel() {
		Font smallFont = Base.getFontPref("status.font","SansSerif,plain,10");
		smallLabel.setFont(smallFont);
		tempLabel.setFont(smallFont);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		label.setAlignmentX(LEFT_ALIGNMENT);
		add(label);
		smallLabel.setAlignmentX(LEFT_ALIGNMENT);
		{
			Box b = Box.createHorizontalBox();
			b.add(smallLabel);
			b.add(Box.createHorizontalGlue());
			b.add(tempLabel);
			b.setAlignmentX(LEFT_ALIGNMENT);
			tempLabel.setAlignmentX(RIGHT_ALIGNMENT);
			add(b);
		}
		add(Box.createVerticalGlue());

		FontMetrics smallMetrics = this.getFontMetrics(smallFont);
		// Height should be ~3 lines 
		int height = (smallMetrics.getAscent() + smallMetrics.getDescent()) * 3;
		setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		setMinimumSize(new Dimension(0, height));
		int prefWidth = 80 * smallMetrics.charWidth('n');
		setPreferredSize(new Dimension(prefWidth, height));
	}

	/**
	 * Indicate which machine, if any, the status panel should attach to.
	 * 
	 * @param machine2
	 *            the machine's controller, or null if no machine is attached.
	 */
	public void setMachine(MachineControllerInterface machine2) {
		if (machine2 != null && this.machine == machine2)
			return;
		this.machine = machine2;
		MachineState state = (machine2!=null)?machine2.getMachineState():new MachineState();
		MachineStateChangeEvent e = new MachineStateChangeEvent(machine2,state);
		updateMachineStatus(e);
	}

	private boolean firmwareWarningIssued = false;

	
	protected String getMachineStateMessage(MachineControllerInterface machine2) {
		if (machine2 == null) { return "No machine selected"; }
		MachineState state = machine2.getMachineState();
		if (state.getState() == MachineState.State.NOT_ATTACHED) {
			if (machine2.getDriver() == null) {
				return "No machine selected";
			} else {
				return "Disconnected";
			}
		}
		if (state.getState() == MachineState.State.CONNECTING) {
			StringBuffer buf = new StringBuffer("Connecting to "+machine2.getName());
			if (machine2.getDriver() instanceof UsesSerial) {
				buf.append(" on ");
				buf.append(((UsesSerial)machine2.getDriver()).getSerial().getName());
			}
			buf.append("...");
			return buf.toString();
		}
		StringBuffer message = new StringBuffer("Machine "+machine2.getName());
		message.append(" ("+machine2.getDriver().getDriverName()+") ");
		if (state.getState() == MachineState.State.READY) { message.append("ready"); }
		else if (state.isPaused()) { message.append("paused"); }
		else if (state.isBuilding()) { 
			if (state.isSimulating()) {
				message.append("simulating");
			} else {
				message.append("building");
			}
		}
		return message.toString();
	}
	
	/**
	 * Display the current status of this machine.
	 */
	protected synchronized void updateMachineStatus(MachineStateChangeEvent evt) {
		// update background to indicate high-level status
		Color bgColor = Color.WHITE;
		MachineControllerInterface machine = evt.getSource();
		String text = getMachineStateMessage(machine);
		if (machine == null || machine.getDriver() == null) {
			bgColor = BG_NO_MACHINE;
		} else {
			boolean initialized = machine.isInitialized() &&
				evt.getState().getState() != MachineState.State.NOT_ATTACHED &&
				evt.getState().getState() != MachineState.State.CONNECTING;
			if (!initialized) {
				bgColor = BG_NO_MACHINE;
			} else {
				if (evt.getState().isBuilding()) {
					bgColor = BG_BUILDING;
				} else {
					bgColor = BG_READY;
				}
				currentTemperature = -1;
				// Check version
				Version v = machine.getDriver().getVersion();
				if (v != null && v.compareTo(machine.getDriver().getPreferredVersion()) < 0) {
					if (!firmwareWarningIssued) {
						firmwareWarningIssued = true;
						JOptionPane.showMessageDialog(
								this,
								"Firmware version "+v+" was detected on your machine.  Firmware version "+
								machine.getDriver().getPreferredVersion() + " is recommended.\n" +
								"Please update your firmware and restart ReplicatorG.",
								"Old firmware detected", JOptionPane.WARNING_MESSAGE);

					}
				} else if (v == null) {
					bgColor = BG_NO_MACHINE;
					// TODO: notify 
					text = "Machine connection timed out";
				}
			}
			
		}
		label.setText(text);
		smallLabel.setText(null);
		tempLabel.setText(null);
		setBackground(bgColor);
	}

	public void machineStateChanged(MachineStateChangeEvent evt) {
		final MachineStateChangeEvent e = evt;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateMachineStatus(e);
			}
		});
	}

	public void machineProgress(MachineProgressEvent event) {
		double remaining;
		double proportion = (double)event.getLines()/(double)event.getTotalLines();

		if (machine.getMachineState().getTarget() == MachineState.Target.SD_UPLOAD) {
			if (event.getLines() == 0) { 
				remaining = 0; // avoid NaN
			}
			remaining = event.getElapsed() * ((1.0/proportion)-1.0);
		} else {
			remaining = event.getEstimated() * (1.0 - proportion);
		}
			
		final String s = String.format(
				"Commands:  %1$7d / %2$7d  (%3$3.2f%%)   |   Elapsed:  %4$s  |  Estimated Remaining:  %5$s",
				event.getLines(), event.getTotalLines(), 
				Math.round(proportion*10000.0)/100.0,
				EstimationDriver.getBuildTimeString(event.getElapsed(), true),
				EstimationDriver.getBuildTimeString(remaining, true));
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				smallLabel.setText(s);
			}
		});
	}

	public void toolStatusChanged(MachineToolStatusEvent event) {
		currentTemperature = event.getTool().getCurrentTemperature();
		if (currentTemperature != -1) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					tempLabel.setText(String.format("Temp: %1$3.1f\u00B0C", currentTemperature));
				}
			});
		}
	}
}
