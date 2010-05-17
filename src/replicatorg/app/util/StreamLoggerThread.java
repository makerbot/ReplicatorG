package replicatorg.app.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import replicatorg.app.Base;

public class StreamLoggerThread extends Thread {
	private BufferedReader reader; 
	private Level defaultLevel;
	
	public StreamLoggerThread(InputStream stream) {
		reader = new BufferedReader(new InputStreamReader(stream));
		defaultLevel = Level.INFO;
	}

	public void setDefaultLevel(Level level) {
		defaultLevel = level;
	}
	
	protected Level getLogLevel(String line) {
		return defaultLevel;
	}
	
	protected void logMessage(String line) {
		Level logLevel = getLogLevel(line);
		Base.logger.log(logLevel,line);		
	}
	
	public void run() {
		boolean atEnd = false;
		try {
			while (!atEnd) {
				String nextLine = reader.readLine();
				if (nextLine == null) {
					atEnd = true;
				} else {
					logMessage(nextLine);
				}
			}
		} catch (IOException e) {
			Base.logger.log(Level.SEVERE,"Stream logger interrupted",e);
		}
	}
	
}
