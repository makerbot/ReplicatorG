package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import replicatorg.app.Base;
import replicatorg.drivers.EstimationDriver;
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

	protected JLabel label = new JLabel();
	protected JLabel smallLabel = new JLabel();
	protected JLabel tempLabel = new JLabel();
	
	// Keep track of whether we are in a building state or not.
	private boolean isBuilding = false;
	
	static final private Color BG_ERROR = new Color(0xff, 0x80, 0x60);
	static final private Color BG_READY = new Color(0x80, 0xff, 0x60);
	static final private Color BG_BUILDING = new Color(0xff, 0xef, 0x00); // process yellow

	MachineStatusPanel() {
		Font smallFont = Base.getFontPref("status.font","SansSerif,plain,10");
		smallLabel.setFont(smallFont);
		tempLabel.setFont(smallFont);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		label.setText("Not Connected");
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
		setBackground(BG_ERROR);
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
	public void updateMachineStatus(MachineStateChangeEvent evt) {
		MachineState.State state = evt.getState().getState();
		
		// Determine what color to use
		Color bgColor = null;
		
		switch (state) {
		case READY:
			bgColor = BG_READY;
			break;
		case BUILDING:
		case BUILDING_OFFLINE:
		case PAUSED:
			bgColor = BG_BUILDING;
			break;
		case NOT_ATTACHED:	
		case ERROR:
		default:
			bgColor = BG_ERROR;
			break;
		}
		
		String text = evt.getMessage();
		
		// Make up some text to describe the state
		if (text == null ) {
			text = state.toString();
		}
		
		// And mark which state we are in.
		switch (state) {
		case BUILDING:
		case BUILDING_OFFLINE:
		case PAUSED:
			isBuilding = true;
			break;
		default:
			isBuilding = false;
			break;
		}
		
		updatePanel(bgColor, text, null, null);
	}
	
	public void updateBuildStatus(MachineProgressEvent event) {
		if (isBuilding) {
			/** Calculate the % of the build that is complete **/
			double proportion = (double)event.getLines()/(double)event.getTotalLines();
			double percentComplete = Math.round(proportion*10000.0)/100.0;
	
			double remaining= event.getEstimated() * (1.0 - proportion);;
			if (event.getTotalLines() == 0) {
				remaining = 0;
			}
				
			final String s = String.format(
					"Commands:  %1$7d / %2$7d  (%3$3.2f%%)   |   Elapsed:  %4$s  |  Estimated Remaining:  %5$s",
					event.getLines(), event.getTotalLines(), 
					percentComplete,
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
