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
import replicatorg.drivers.EstimationDriver;
import replicatorg.drivers.UsesSerial;
import replicatorg.drivers.Version;
import replicatorg.machine.MachineController;
import replicatorg.machine.MachineControllerInterface;
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
	
	// Keep track of whether we are in a building state or not.
	private boolean isBuilding = false;
	private boolean firmwareWarningIssued = false;
	
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
	 * @param machine
	 *            the machine's controller, or null if no machine is attached.
	 */
	public void setMachine(MachineControllerInterface machine) {
		if (machine != null && this.machine == machine) {
			return;
		}
		
		this.machine = machine;
		
		// Manufacture a machine state event so that we update properly
		MachineState state = (machine!=null)?machine.getMachineState():new MachineState(MachineState.State.NOT_ATTACHED);
		MachineStateChangeEvent e = new MachineStateChangeEvent(machine,state);
		updateMachineStatus(e);
	}
	
	private void updatePanel(Color panelColor, String text, String smallText, String tempText) {
		setBackground(panelColor);
		label.setText(text);
		smallLabel.setText(smallText);
		tempLabel.setText(tempText);
	}
	
// TODO: this has no business being here.
//	private boolean checkVersionCompatibility() {
//		Version v = machine.getDriverQueryInterface().getVersion();
//		
//		 if (v == null) {
//			return false;
//		}
//		
//		if (v.compareTo(machine.getDriverQueryInterface().getPreferredVersion()) < 0) {
//			if (!firmwareWarningIssued) {
//				firmwareWarningIssued = true;
//				JOptionPane.showMessageDialog(
//						this,
//						"Firmware version "+v+" was detected on your machine.  Firmware version "+
//						machine.getDriverQueryInterface().getPreferredVersion() + " is recommended.\n" +
//						"Please update your firmware and restart ReplicatorG.",
//						"Old firmware detected", JOptionPane.WARNING_MESSAGE);
//			}
//		}
//		return true;
//	}
	
	
	/**
	 * Display the current status of this machine.
	 */
	// TODO: Clean this up just ask the machine for all of this info.
	public void updateMachineStatus(MachineStateChangeEvent evt) {
		// If we don't have a machine, its a no-go.
//		if (machine == null || !(machine.isInitialized())) {
//			updatePanel(BG_NO_MACHINE, "No machine selected", null, null);
//			return;
//		}
		
		// TODO: Should we verify we're on the right machine???
		MachineControllerInterface machine = evt.getSource();
		MachineState.State state = evt.getState().getState();
		
		// Determine what color to use
		Color bgColor = null;
		
		switch (state) {
		case READY:
			bgColor = BG_READY;
			break;
		case BUILDING:
		case PAUSED:
			bgColor = BG_BUILDING;
			break;
		default:
			bgColor = BG_NO_MACHINE;
			break;
		}
		
		String text = null;
		
		// Make up some text to describe the state
		switch (state) {
		case NOT_ATTACHED:
			if (machine.getDriver() == null) {
				text = "No machine selected";
			} else {
				text = "Disconnected";
			}
			break;
		case CONNECTING:
			StringBuffer buf = new StringBuffer("Connecting to "+machine.getMachineName());
			if (machine.getDriver() instanceof UsesSerial) {
				buf.append(" on ");
				buf.append(((UsesSerial)machine.getDriver()).getPortName());
			}
			buf.append("...");
			text = buf.toString();
			break;
		case READY:
		case BUILDING:
		case PAUSED:
		{
			StringBuffer message = new StringBuffer("Machine "+machine.getMachineName());
			message.append(" ("+machine.getDriver().getDriverName()+") ");

				//message.append("ready");
				//mmessage.append("paused");
				//message.append("simulating");
				//message.append("building");
			text = message.toString();
		}
			break;
		case ERROR:
		{
			StringBuffer message = new StringBuffer();
			message.append("Error!");
			text = message.toString();
		}
			break;
		}
		
		// And mark which state we are in.
		switch (state) {
		case BUILDING:
		case PAUSED:
			isBuilding = true;
			break;
		default:
			isBuilding = false;
			break;
		}
		
		
		// This is all good, but if the version is bad, give a warning color 
//		if (!checkVersionCompatibility()) {
//			bgColor = BG_NO_MACHINE;
//		}
		
		updatePanel(bgColor, text, null, null);
	}
	
	public void updateBuildStatus(MachineProgressEvent event) {
		if (isBuilding) {
			double remaining;
			double proportion = (double)event.getLines()/(double)event.getTotalLines();
	
			if (machine.getTarget() == MachineController.JobTarget.REMOTE_FILE) {
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
			
			smallLabel.setText(s);
		}
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
		final MachineProgressEvent e = event;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateBuildStatus(e);
			}
		});
	}

	public void toolStatusChanged(MachineToolStatusEvent event) {
		final MachineToolStatusEvent e = event;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				double temperature = e.getTool().getCurrentTemperature();
				tempLabel.setText(String.format("Temp: %1$3.1f\u00B0C", temperature));
			}
		});
	}
}
