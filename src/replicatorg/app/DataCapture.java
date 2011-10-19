package replicatorg.app;

import java.io.FileWriter;
import java.io.IOException;

import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

// We can open, close, and store data to disk. Easy. Also, we can have a timer that periodically requests info.
public class DataCapture implements MachineListener {
	
	FileWriter outFile;
	
	public DataCapture(String filename) {
		 try {
			outFile = new FileWriter(filename);
		} catch (IOException e) {
			Base.logger.severe("Couldn't open data capture file for writing:" + e.getMessage());
		}
		
		// TODO: Subscribe to machine events.
		Base.getMachineLoader().addMachineListener(this);
	}
	
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
	
	private String jsonString(String name, double value) {
		return "\"" + name + "\" : " + Double.toString(value);
	}

	private String jsonString(String name, String value) {
		return "\"" + name + "\" : \"" + value + "\"";
	}
	
	@Override
	public void toolStatusChanged(MachineToolStatusEvent event) {
		Base.logger.severe("Got machine tool status event!");
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
