package replicatorg.drivers;

/**
 * InteractiveDisplay encapsulates functionality involving an interactive interface
 * connected to the machine (such as displaying messages or waiting for keypresses).
 */
public interface InteractiveDisplay {
	public void displayMessage(double seconds, String message) throws RetryException;
	public void userPause(double seconds, boolean resetOnTimeout) throws RetryException;
}
