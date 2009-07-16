package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import replicatorg.app.Base;
import replicatorg.app.MachineController;
import replicatorg.app.Serial;
import replicatorg.app.TimeoutException;
import replicatorg.drivers.UsesSerial;
import replicatorg.drivers.Version;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;

/**
 * The MachineStatusPanel displays the current state of the connected machine,
 * or a message informing the user that no connected machine can be found.
 * 
 * @author phooky
 * 
 */
public class MachineStatusPanel extends JPanel implements MachineListener {
	private static final long serialVersionUID = -6944931245041870574L;

	protected MachineController machine = null;

	protected JLabel label = new JLabel();
	protected JLabel smallLabel = new JLabel();

	static final private Color BG_NO_MACHINE = new Color(0xff, 0x80, 0x60);

	static final private Color BG_READY = new Color(0x80, 0xff, 0x60);

	// static final private Color BG_WAIT = new Color(0xff,0xff,0x60);

	MachineStatusPanel() {
		Font smallFont = Base.getFontPref("status.font","SansSerif,plain,10");
		smallLabel.setFont(smallFont);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(Box.createHorizontalStrut(10));
		Box vbox = Box.createVerticalBox();
		vbox.add(label);
		vbox.add(smallLabel);
		add(vbox);
		add(Box.createHorizontalGlue());
		add(Box.createHorizontalStrut(10));
	}

	/**
	 * Indicate which machine, if any, the status panel should attach to.
	 * 
	 * @param machine
	 *            the machine's controller, or null if no machine is attached.
	 */
	public void setMachine(MachineController machine) {
		System.err.println("Machine set to "+machine);
		if (this.machine == machine)
			return;
		this.machine = machine;
		updateMachineStatus();
	}

	private boolean firmwareWarningIssued = false;

	
	protected String getMachineStateMessage() {
		if (machine == null) { return "No machine selected"; }
		MachineState state = machine.getState();
		if (state == MachineState.NOT_ATTACHED) {
			if (machine.getDriver() == null) {
				return "No machine selected";
			} else if (machine.driver instanceof UsesSerial && 
					((UsesSerial)machine.driver).getSerial() == null) {
				if (Serial.scanSerialNames().size() == 0) {
					return "No serial ports detected";
				} else {
					return "No serial port selected";
				}
			}
		}
		if (state == MachineState.CONNECTING) {
			StringBuffer buf = new StringBuffer("Connecting to "+machine.getName());
			if (machine.driver instanceof UsesSerial) {
				buf.append(" on ");
				buf.append(((UsesSerial)machine.driver).getSerial().getName());
			}
			buf.append("...");
			return buf.toString();
		}
		StringBuffer message = new StringBuffer("Machine "+machine.getName());
		message.append(" ("+machine.getDriver().getDriverName()+") ");
		if (state == MachineState.READY) { message.append("Ready"); }
		else if (state == MachineState.READY) { message.append("ready"); }
		else if (state == MachineState.BUILDING) { message.append("building"); }
		else if (state == MachineState.PAUSED) { message.append("paused"); }
		return message.toString();
	}
	
	/**
	 * Display the current status of this machine.
	 */
	protected synchronized void updateMachineStatus() {
		// update background to indicate high-level status
		Color bgColor = Color.WHITE;
		String text = getMachineStateMessage();
		if (machine == null || machine.driver == null) {
			bgColor = BG_NO_MACHINE;
		} else {
			if (!machine.isInitialized()) {
				bgColor = BG_NO_MACHINE;
			} else {
				bgColor = BG_READY;
				// Check version
				try {
					Version v = machine.driver.getVersion();
					if (v != null && v.compareTo(machine.driver.getPreferredVersion()) < 0) {
						if (!firmwareWarningIssued) {
							firmwareWarningIssued = true;
							JOptionPane.showMessageDialog(
									this,
									"Firmware version "+v+" was detected on your machine.  Firmware version "+
									machine.driver.getPreferredVersion() + " is recommended.\n" +
									"Please update your firmware and restart ReplicatorG.",
									"Old firmware detected", JOptionPane.WARNING_MESSAGE);
							
						}
					}
				} catch (TimeoutException te) {
					bgColor = BG_NO_MACHINE;
					// TODO: notify 
					text = "Machine connection timed out";
				}
			}
			
		}
		label.setText(text);
		smallLabel.setText(null);
		setBackground(bgColor);
	}

	public void machineStateChanged(MachineStateChangeEvent evt) {
		updateMachineStatus();
	}

	public void machineProgress(MachineProgressEvent event) {
		smallLabel.setText(event.toString());
	}
}
