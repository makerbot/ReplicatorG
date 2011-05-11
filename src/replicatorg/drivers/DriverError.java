package replicatorg.drivers;

public class DriverError {
	/** Message explaining the error **/
	final String message;
	final boolean disconnected;
		
	public DriverError(String message, boolean disconnected) {
		this.message = message;
		this.disconnected = disconnected;
	}
	
	/** Text string describing the error **/
	public String getMessage() {
		return message;
	}
	
	/** True if this error caused the connection to the device to be lost **/
	public boolean getDisconnected() {
		return disconnected;
	}
}
