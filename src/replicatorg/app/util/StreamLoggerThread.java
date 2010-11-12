package replicatorg.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import replicatorg.app.Base;

public class StreamLoggerThread extends Thread {
	private InputStreamReader reader; 
	private Level defaultLevel;
	
	public StreamLoggerThread(InputStream stream) {
		reader = new InputStreamReader(stream);
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
			StringBuffer nextLine = new StringBuffer();
			while (!atEnd) {
				int nextChar = reader.read();
				// The \r is for Skeinforge-31 ->, which outputs \r between progress lines
				if (nextChar == '\n' || nextChar == '\r' || nextChar == -1) {
					if (nextLine.length() > 0) {
						logMessage(nextLine.toString());
					}
					if (nextChar == -1) { atEnd = true; }
					nextLine = new StringBuffer();
				} else {
					nextLine.append((char)nextChar);
				}
			}
		} catch (IOException e) {
			Base.logger.log(Level.SEVERE,"Stream logger interrupted",e);
		}
	}
	
}
