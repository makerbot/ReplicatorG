package replicatorg.drivers;

/**
 * InteractiveDisplay encapsulates functionality involving an interactive interface
 * connected to the machine (such as displaying messages or waiting for keypresses).
 */
public interface InteractiveDisplay {
	public void displayMessage(double seconds, String message, boolean ButtonWait) throws RetryException;
	public void userPause(double seconds, boolean resetOnTimeout, int buttonMask) throws RetryException;
	
	public void playSong(int songId) throws RetryException;
	public void updateBuildPercent(int percentDone) throws RetryException;
	
	public void sendBuildStartNotification(String string, int i) throws RetryException;
	public void sendBuildEndNotification(int endCode)  throws RetryException ;

}

