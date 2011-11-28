package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.drivers.EstimationDriver;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;
import replicatorg.machine.model.ToolModel;

/**
 * The MachineStatusPanel displays the current state of the connected machine,
 * or a message informing the user that no connected machine can be found.
 * This is the big bar (usually Red or Light Green) with status text in it.
 * 
 * @author phooky
 * 
 */
public class MachineStatusPanel extends BGPanel implements MachineListener {
	private static final long serialVersionUID = -6944931245041870574L;

	
	protected JLabel mainLabel= new JLabel();
	
	/// small text label for ongoing actions
	protected JLabel smallLabel = new JLabel();
	
	/// Temperature status string
	protected JLabel tempLabel = new JLabel();

	/// Machine tyle/connection string
	protected JLabel machineLabel = new JLabel();
	
	// Keep track of whether we are in a building state or not.
	private boolean isBuilding = false;
	
	static final private Color BG_ERROR = new Color(0xff, 0x80, 0x60);
	static final private Color BG_READY = new Color(0x80, 0xff, 0x60);
	static final private Color BG_BUILDING = new Color(0xff, 0xef, 0x00); // process yellow

	
	MachineStatusPanel() {
		Font statusFont = Base.getFontPref("status.font","SansSerif,plain,12");
		Font smallFont = statusFont.deriveFont(10f);
		smallLabel.setFont(smallFont);
		tempLabel.setFont(smallFont);
		machineLabel.setFont(smallFont);
		mainLabel.setFont(statusFont);
		mainLabel.setText("Not Connected");
	
		setLayout(new MigLayout("fill,novisualpadding, ins 5 10 5 10"));
		add(mainLabel, "top, left, growx, split");
		add(machineLabel, "top, right, wrap");
		add(smallLabel, "bottom, left, growx, split");
		add(tempLabel, "bottom, right");

		FontMetrics smallMetrics = this.getFontMetrics(smallFont);
		FontMetrics bigMetrics = this.getFontMetrics(statusFont);
		// Height should be ~3 lines 
		int height = (smallMetrics.getAscent() + smallMetrics.getDescent()) * 2 +
				bigMetrics.getAscent() + smallMetrics.getDescent();
		setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		setMinimumSize(new Dimension(0, height));
		int prefWidth = 80 * smallMetrics.charWidth('n');
		setPreferredSize(new Dimension(prefWidth, height));
		setBackground(BG_ERROR);
	}


	private void updatePanel(Color panelColor, String text, String smallText, String machineText) {
		setBackground(panelColor);
		mainLabel.setText(text);
		smallLabel.setText(smallText);
		machineLabel.setText(machineText);
	}
	
	/**
	 * Creatss a one line string of machine info
	 * @return
	 */
	private String machineStatusString(String machineId, boolean connected)
	{
		String machineText = Base.preferences.get("machine.name", machineId);
		if( connected )
		{
			String connection = Base.preferences.get("serial.last_selected", "Unknown Connection");
			machineText += " on " + connection;
		} else {
			machineText += " Not Connected";
		}
		return machineText;
	}
	
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
		
		String machineText = machineStatusString(evt.getSource().getMachineName(),
												 evt.getState().isConnected()); 
		
		if( evt.getState().isConnected() == false )
			tempLabel.setText("");

		if( evt.getState().isBuilding() && !(Base.preferences.getBoolean("build.monitor_temp", false) ))
			tempLabel.setText("Monitor build temp.: off");
		
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
		
		updatePanel(bgColor, text, null, machineText);
	}
	
	public void updateBuildStatus(MachineProgressEvent event) {
		if (isBuilding) {
			/** Calculate the % of the build that is complete **/
			double proportion = (double)event.getLines()/(double)event.getTotalLines();
			double percentComplete = Math.round(proportion*10000.0)/100.0;
	
			double remaining= event.getEstimated() * (1.0 - proportion);
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
				Vector<ToolModel> tools = e.getSource().getModel().getTools();
				String tempString = "";
				
				// This call makes sure our temperatures are up to date
				e.getSource().getDriver().readPlatformTemperature();
				
				for(ToolModel t : tools)
				{
					double temp= t.getCurrentTemperature();
					tempString += String.format("Toolhead "+t.getIndex()+": %1$3.1f\u00B0C  ", temp);
					if(t.hasHeatedPlatform())
					{
						double ptemp = t.getPlatformCurrentTemperature();
						tempString += String.format("Platform: %1$3.1f\u00B0C", ptemp);
					}
				}
				
				tempLabel.setText(tempString);
			}
		});
	}
}
