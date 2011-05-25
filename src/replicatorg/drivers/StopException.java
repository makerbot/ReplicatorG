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
	
	private String getFormattedMessage(String message) {
		StringBuilder output = new StringBuilder();
		
		output.append("<html>");

		String[] words = message.split("\\s+");
		int charCount = 0;
		
		for (String word : words) {
			charCount += word.length();
			if (charCount > 60) {
				output.append("<br />");
				charCount = word.length();
			}
			output.append(word);
			output.append(" ");
		}
		
		output.append("</html>");
		
		return output.toString();
	}
	
	public String getMessage() {
		return getFormattedMessage(message);
	}
}
