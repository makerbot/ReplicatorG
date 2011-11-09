package replicatorg.app;

import java.io.FileWriter;
import java.io.IOException;

import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;


/**
 * Basic DataLogger class.
 * This is a quick and dirty class to open, close, and store data to disk. 
 * Registers as a Machine Listener, and watches for events and logs temperature from those events
 */ 
public class DataCapture implements MachineListener {
	
	
	FileWriter outFile; /// Manages our output file

	/**
	* Generic Constructor.  Creates an output file and registers as a machineListener
	* @filename : desired output file
	*/ 	
	public DataCapture(String filename) {
		 try {
			outFile = new FileWriter(filename);
		} catch (IOException e) {
			Base.logger.severe("Couldn't open data capture file for writing:" + e.getMessage());
		}
		// Listen to the machine, do you hear what it is telling you? 
		Base.getMachineLoader().addMachineListener(this);
	}

	/**
	*  apppends the string to our log file 
	* @message logfile string, please pass JSON dicts only
	*/	
	public void WriteMessage(String message) {
		try {
			outFile.write(message + "\n");
			outFile.flush();
		} catch (IOException e) {
			Base.logger.severe("Couldn't write to data capture file:" + e.getMessage());
		}
	}

	@Override
	public void machineStateChanged(MachineStateChangeEvent evt) {
	}

	@Override
	public void machineProgress(MachineProgressEvent event) {
	}
	
	/* Converts a name/value pair to a mini json string */
	private String jsonString(String name, double value) {
		return "\"" + name + "\" : " + Double.toString(value);
	}

	/* Converts a name/value pair to a mini json string */
	private String jsonString(String name, String value) {
		return "\"" + name + "\" : \"" + value + "\"";
	}
	
	/** on toolStatus changes, write the temperature info into the log file
	* in json format
	*/
	@Override
	public void toolStatusChanged(MachineToolStatusEvent event) {
		WriteMessage(
				"{"
				+ jsonString("time", event.getDateString())
				+ ", " + jsonString("tool_index", event.getTool().getIndex())
				+ ", " + jsonString("bed_temp", event.getTool().getPlatformCurrentTemperature())
				+ ", " + jsonString("ext_temp", event.getTool().getCurrentTemperature())
				+ "}"
				+ "\n"
		);
		
	}
}
