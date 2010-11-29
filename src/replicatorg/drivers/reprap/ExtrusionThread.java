/**
 * 
 */
package replicatorg.drivers.reprap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The extrusion thread provides 5D extrude commands at a regular frequency so that manual 
 * extruder control will work with the RepRap 5D firmware.
 * @author rob
 */
public class ExtrusionThread extends Thread {
	
	private final RepRap5DDriver driver;
	/** The notifier is used to tell us when the extruder should start extruding */
	private final Object notifier = new Object();
	/** true if the RepRap should be extruding */
	private final AtomicBoolean isExtruding = new AtomicBoolean(false);
	private ReentrantLock feedrateLock = new ReentrantLock();
	private double feedrate = 0;
	private ReentrantLock directionLock = new ReentrantLock();
	private Direction direction = Direction.forward;
	
	public enum Direction
	{
		forward,
		reverse;
	}

	public ExtrusionThread(RepRap5DDriver driver)
	{
		super("RepRap Extrusion Thread");
		this.driver = driver;
		this.start();
	}
	
	

	private void sendExtrudeCommand(double distance, double feedrate) {
		String feedrateString = driver.df.format(feedrate);
		String eCode = driver.df.format(distance*((direction == Direction.forward)?1 : -1));
		driver.sendCommand(driver._getToolCode() + "G92 E0");
		driver.sendCommand(driver._getToolCode() + "G1 F"+feedrateString);
		driver.sendCommand(driver._getToolCode() + "G1 E"+ eCode +" F"+feedrateString);
	}
	
	public void setFeedrate(double feedrate)
	{
		feedrateLock.lock();
		this.feedrate = feedrate;
		feedrateLock.unlock();
	}

	private double getFeedrate()
	{
		feedrateLock.lock();
		double f = this.feedrate;
		feedrateLock.unlock();
//		directionLock.lock();
//		f *= (direction == Direction.forward)?1 : -1;
//		directionLock.unlock();
		return f;
	}
	
	public void setDirection(Direction direction)
	{
		directionLock.lock();
		this.direction = direction;
		directionLock.unlock();
	}

	@Override
	public void run() {
		synchronized(notifier)
		{
			/** the time length of each extrusion command */
			//long commandPeriod = 500; //ms
			/** the length of filament to extrude on each command */
			double distance = 2; //mm (not really) - FIXME: should be shorter when speed is low
			int queueSize = 0;
			
			/* Outer loop to wait for extrude commands */
			while (true)
			{
				try {
					notifier.wait();
				} catch (InterruptedException e1) {
					//This is actually what we want to happen!
				}
				/* creating a series of 5D extrude commands to
				 * keep the command queue full. */
				while(isExtruding.get() == true)
				{
					synchronized (driver.commands) {
						queueSize = driver.commands.size();
					}
					if (queueSize < 7)//7 keeps my buffer saturated, it was 6
					{
						// Send extrude command
						double feedrate = this.getFeedrate();
						synchronized (driver) {
							sendExtrudeCommand(distance, feedrate);
						}
					}
				}
			}
		}
	}


	/**
	 * resumes the extruder thread (start extruding)
	 */
	public void startExtruding() {
		synchronized(notifier)
		{
			this.isExtruding.set(true);
			this.notifier.notifyAll();
		}
	}
	
	/**
	 * stops the extruder thread (thus stopping the extruder within 0.5 seconds)
	 */
	public void stopExtruding() {
		this.isExtruding.set(false);
	}

}
