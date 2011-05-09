package replicatorg.drivers;

/**
 * A stop exception indicates that the machine controller should stop processing gcodes.
 * @author matt.mets
 */
public class StopException extends Exception {
	public enum StopType {
		UNCONDITIONAL_HALT,
		PROGRAM_END,
		OPTIONAL_HALT,
		PROGRAM_REWIND,
	}
	
	String message;
	StopType type;
	
	public StopException(String message, StopType type) {
		this.message = message;
		this.type = type;
	}
	
	public StopType getType() {
		return type;
	}
	
	public String getMessage() {
		return message;
	}
}
