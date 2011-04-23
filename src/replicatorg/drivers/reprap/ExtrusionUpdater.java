/**
 * 
 */
package replicatorg.drivers.reprap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import replicatorg.app.Base;

/**
 * The extrusion thread provides 5D extrude commands at a regular frequency so that manual 
 * extruder control will work with the RepRap 5D firmware.
 * @author rob
 */
public class ExtrusionUpdater {
	
	private final RepRap5DDriver driver;
	public final AtomicBoolean isExtruding = new AtomicBoolean(false);
	private ReentrantLock feedrateLock = new ReentrantLock();
	private double feedrate = 0;
	private ReentrantLock directionLock = new ReentrantLock();
	private Direction direction = Direction.forward;
	/** the time (ms) the last extrude command will complete or zero if unset */
	private long extrudeQueueEnd = 0;

	/** the time length of each extrusion command 
	 *  will be overriden if motor_steps is configured, and is set to a 
	 *  different value than 200 steps.
	 * */
	private long commandPeriod = 100; //ms
	/** queue this many commands into the future. Larger = smoother extrusion, 
	 * smaller = more responsive manual extrusion. */
	private int maxQueuedExtrudeTime = 200; //ms
	
	
	
	public enum Direction
	{
		forward,
		reverse;
	}

	public ExtrusionUpdater(RepRap5DDriver driver)
	{
		this.driver = driver;
	}
	
	private void determineCommandPeriod() {
		// override with motor_steps from tool model (uses machine's XML file)
		// should be initialized elsewhere, but somehow the driver/tool_model isn't initialized yet.
		double motorSteps = driver.getMotorSteps();
		if(motorSteps == 0.0) {
			motorSteps = 200;
		}
		Base.logger.info("motorSteps="+motorSteps);
		this.commandPeriod = (long) (motorSteps/2); //ms
		Base.logger.info("commandPeriod="+this.commandPeriod);
		
	}
	

	private void sendExtrudeCommand(double distance, double feedrate) {
		determineCommandPeriod();
		String feedrateString = driver.df.format(feedrate);
//		Base.logger.info("commandPeriod="+this.commandPeriod);
		if (driver.feedrate.get() != feedrate)
		{
			driver.sendCommand(driver._getToolCode() + "G1 F"+feedrateString);
		}
		driver.sendCommand(driver._getToolCode() + "G1 E"+
				driver.df.format(distance+driver.ePosition.get())+" F"+feedrateString);
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
			long currentTime = System.currentTimeMillis();
			if (extrudeQueueEnd > currentTime + maxQueuedExtrudeTime)
			{
				Thread.sleep(extrudeQueueEnd-currentTime);
			}
			do {
				queueSize = driver.queueSize();
				if (queueSize > maxQueuedExtrudeTime) Thread.sleep(100);
			} while (queueSize > maxQueuedExtrudeTime);
				
			// Send extrude command
			/** mm/s */
			double feedrate = this.getFeedrate();// mm per minute
			if (feedrate == 0) return;
			/** the length of filament to extrude on each command */
			double distance = (feedrate*commandPeriod)/60000; // mm
			synchronized (driver) {
				if (extrudeQueueEnd < System.currentTimeMillis())
				{
					extrudeQueueEnd = System.currentTimeMillis()+commandPeriod;
				}
				else
				{
					extrudeQueueEnd += commandPeriod;
				}
				sendExtrudeCommand(distance, Math.abs(feedrate));
			}
		}
		else
		{
			Thread.sleep(100);
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
