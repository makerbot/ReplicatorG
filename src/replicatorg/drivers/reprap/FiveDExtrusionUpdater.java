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
public class FiveDExtrusionUpdater {
	
	private final RepRap5DDriver driver;
	public final AtomicBoolean isExtruding = new AtomicBoolean(false);
	private ReentrantLock feedrateLock = new ReentrantLock();
	private double feedrate = 0;
	private ReentrantLock directionLock = new ReentrantLock();
	private Direction direction = Direction.forward;

	/** the time length of each extrusion command */
	private long commandPeriod = 500; //ms
	/** queue this many commands at a time. Larger = smoother extrusion, 
	 * smaller = more responsive manual extrusion. */
	private int maxQueuedExtrudeCommands = 2;
	
	public enum Direction
	{
		forward,
		reverse;
	}

	public FiveDExtrusionUpdater(RepRap5DDriver driver)
	{
		this.driver = driver;
	}
	
	

	private void sendExtrudeCommand(double distance, double feedrate) {
		String feedrateString = driver.df.format(feedrate);
		driver.sendCommand(driver._getToolCode() + "G92 E0");
		driver.sendCommand(driver._getToolCode() + "G1 F"+feedrateString);
		driver.sendCommand(driver._getToolCode() + "G1 E"+
				driver.df.format(distance)+" F"+feedrateString);
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
		directionLock.lock();
		f *= (direction == Direction.forward)?1 : -1;
		directionLock.unlock();
		return f;
	}
	
	public void setDirection(Direction direction)
	{
		directionLock.lock();
		this.direction = direction;
		directionLock.unlock();
	}

	public void update() throws InterruptedException {
		int queueSize;

		/* creating a series of 5D extrude commands to
		 * keep the command queue full. */
		if(isExtruding.get() == true)
		{
			do {
				synchronized (driver.commands) {
					queueSize = driver.commands.size();
				}
				if (queueSize > maxQueuedExtrudeCommands) Thread.sleep(100);
			} while (queueSize > maxQueuedExtrudeCommands);
				
			// Send extrude command
			/** mm/s */
			double feedrate = this.getFeedrate();
			/** the length of filament to extrude on each command */
			double distance = (feedrate*commandPeriod)/1000; //mm
			synchronized (driver) {
				sendExtrudeCommand(distance, feedrate);
			}
		}
	}


	/**
	 * resumes the extruder thread (start extruding)
	 */
	public void startExtruding() {
		this.isExtruding.set(true);
	}
	
	/**
	 * stops the extruder thread (thus stopping the extruder within 0.5 seconds)
	 */
	public void stopExtruding() {
		this.isExtruding.set(false);
	}

}
