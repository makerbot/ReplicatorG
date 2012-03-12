package replicatorg.util;

/**
 * This class provides static methods for concisely enforcing preconditions,
 * e.g. on the arguments to a method or constructor.  It's analogous to the
 * class with the same name in the Guava library.
 */
public class Preconditions {
	private Preconditions() {}  // Disallow construction
	
	public static int checkNonNegative(int value, String message) {
		if (value < 0) throw new IllegalArgumentException(message);
		return value;
	}
}
