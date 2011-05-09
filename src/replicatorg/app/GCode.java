package replicatorg.app;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GCode {

	// These are the letter codes that we understand
	static protected char[] codes = { 
		'A', 'B', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
		'M', 'P', 'Q', 'R', 'S', 'T', 'X', 'Y', 'Z' };
	
	// pattern matchers.
	static Pattern parenPattern  = Pattern.compile("\\((.*)\\)");
	static Pattern semiPattern = Pattern.compile(";(.*)");
	static Pattern deleteBlockPattern = Pattern.compile("^(\\.*)");
	
	
	// The actual GCode command string
	private String command;

	// Parsed out comment
	private String comment = new String();

	private class gCodeParameter {
		final public char code;
		final public Double value;
		gCodeParameter(char code, Double value) {
			this.code = code;
			this.value = value;
		}
	}
	
	// The set of parameters in this GCode
	private List<gCodeParameter> parameters;

	public GCode(String command) {
		// Copy over the command
		this.command = new String(command);
		
		// Initialize the present and value tables
		this.parameters = new ArrayList<gCodeParameter>();
		
		// Parse (and strip) any comments out into a comment string
		parseComments();

		// Parse any codes out into the code tables
		parseCodes();
	}
	
	// Find any comments, store them, then remove them from the command 
	private void parseComments() {
		Matcher parenMatcher = parenPattern.matcher(command);
		Matcher semiMatcher = semiPattern.matcher(command);

		// Note that we only support one style of comments, and only one comment per row. 
		if (parenMatcher.find())
			comment = parenMatcher.group(1);

		if (semiMatcher.find())
			comment = semiMatcher.group(1);

		// clean it up.
		comment = comment.trim();
		comment = comment.replace('|', '\n');

		// Finally, remove the comments from the command string
		command = parenMatcher.replaceAll("");
		
		semiMatcher = semiPattern.matcher(command);
		command = semiMatcher.replaceAll("");
	}

	// Find any codes, and store them
	private void parseCodes() {
		for (char code : codes) {
			Pattern myPattern = Pattern.compile(code + "([0-9.+-]+)");
			Matcher myMatcher = myPattern.matcher(command);

			if (command.indexOf(code) >= 0) {
				double value = 0;
				
				if (myMatcher.find()) {
					String match = myMatcher.group(1);
					value = Double.parseDouble(match);
				}
				
				parameters.add(new gCodeParameter(code, value));
			}
		}
	}

	public String getCommand() {
		// TODO: Note that this is the command minus any comments.
		return new String(command);
	}
	
	public String getComment() {
		return new String(comment);
	}
	
	public boolean hasCode(char searchCode) {
		for (gCodeParameter parameter : parameters) {
			if (parameter.code == searchCode) {
				return true;
			}
		}
		
		return false;
	}

	public double getCodeValue(char searchCode) {
		for (gCodeParameter parameter : parameters) {
			if (parameter.code == searchCode) {
				return parameter.value;
			}
		}
		
		return -1;	// TODO: What do we return if there is no code?
	}
}