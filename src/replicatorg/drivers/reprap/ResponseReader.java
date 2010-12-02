/**
 * 
 */
package replicatorg.drivers.reprap;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author rob
 * Thread for the asynchronous handling of firmware responses to each driver-issued 
 * command.
 */
public class ResponseReader extends Thread {
	final RepRap5DDriver driver;
	private final AtomicBoolean keepAlive = new AtomicBoolean(true);
	/** Notifies all once the thread stops executing due to keepAlive being set to false */
	private final Object endNotifier = new Object();
	/** Notifies the thread when a new command has been sent*/
	private final Object commandSentNotifier = new Object();
	
	protected ResponseReader(RepRap5DDriver driver)
	{
		this.driver = driver;
		//avoids sketchy states where the ResponseReader exists but is not started.
		this.start();
	}

	/*
	 * Continually reads serial input from the RepRap and interprets it using the driver's
	 * readResponse method. Once all the commands sent by the driver have been acknowledged
	 * the thread waits for the next command before resuming it's readResponse loop.
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		//wait for the first command to be sent before trying to read responses
		waitForNotification(commandSentNotifier);

		while(this.keepAlive.get()==true) {
			// polling the read response function for new responses from the printer
			driver.readResponse();

			/* if there are no commands left waiting to receive acknowledgement. Go to 
			 * sleep until the next command is sent by the driver */
			synchronized(commandSentNotifier)
			{
				if (driver.isFinished())
				{
					waitForNotification(commandSentNotifier);
				}
			}

		}

		//Notify that the ResponseReader thread is exiting
		synchronized (endNotifier) {
			endNotifier.notifyAll();
		}
	}
	
	/**
	 * Notifies the ResponseReader that a new command has been sent and to wait for 
	 * new responses.
	 */
	public void notifyCommandSent()
	{
		synchronized (commandSentNotifier) {
			commandSentNotifier.notifyAll();
		}
	}
	
	
	/**
	 * Kills the response reader thread and waits for it to stop iterating before
	 * returning. The ResponseReader object will be unusable after this is called and 
	 * should not be used.
	 * @throws InterruptedException
	 */
	public void dispose()
	{
		synchronized (endNotifier) {
			keepAlive.set(false);
			//Stop waiting for the next command and exit the loop.
			synchronized (commandSentNotifier) {
				commandSentNotifier.notifyAll();
			}
			//wait for the thread to die.
			waitForNotification(endNotifier);
		}
	}
	
	private void waitForNotification(Object object)
	{
		synchronized (object) {
			try {
				object.wait();
			} catch (InterruptedException e) {/* Expected */}
		}
	}
}
