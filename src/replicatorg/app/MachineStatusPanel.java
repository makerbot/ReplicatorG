package replicatorg.app;

import java.awt.Color;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The MachineStatusPanel displays the current state of the connected machine,
 * or a message informing the user that no connected machine can be found.
 * 
 * @author phooky
 * 
 */
public class MachineStatusPanel extends JPanel implements Runnable {
	protected MachineController machine = null;

	protected JLabel label = new JLabel();

	protected Thread statusThread = null;

	static final private Color BG_NO_MACHINE = new Color(0xff, 0x80, 0x60);

	static final private Color BG_READY = new Color(0x80, 0xff, 0x60);

	// static final private Color BG_WAIT = new Color(0xff,0xff,0x60);

	MachineStatusPanel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(Box.createHorizontalStrut(10));
		add(label);
		add(Box.createHorizontalGlue());
		add(Box.createHorizontalStrut(10));
		statusThread = new Thread(this);
		statusThread.start();
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
		synchronized (machine) {
			this.machine = machine;
		}
		updateMachineStatus();
	}

	/**
	 * Display the current status of this machine.
	 */
	protected void updateMachineStatus() {
		// update background to indicate high-level status
		Color bgColor = Color.WHITE;
		String text;
		if (machine == null) {
			bgColor = BG_NO_MACHINE;
			text = "No machine currently connected";
		} else {
			if (!machine.isInitialized()) {
				bgColor = BG_NO_MACHINE;
			} else {
				bgColor = BG_READY;
			}
			text = machine.getStatusText();
		}
		label.setText(text);
		setBackground(bgColor);
	}

	public void run() {

		while (true) {
			try {
				updateMachineStatus();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// We're probably being shut down.
				break;
			}
		}
	}
}
