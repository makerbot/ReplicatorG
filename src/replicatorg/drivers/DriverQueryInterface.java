package replicatorg.drivers;

import javax.vecmath.Point3d;

import replicatorg.util.Point5d;

/**
 * Interface for querying a Driver about its current state. This is used by
 * the parser when devolving GCodes into DriverCommands.
 * 
 * @author matt.mets
 */
public interface DriverQueryInterface {
	public int getCurrentToolIndex();
	
	public String getDriverName();
	public boolean hasEmergencyStop();
	public boolean hasSoftStop();
	
	public Point3d getOffset(int i);
	
	public Point5d getMaximumFeedrates();

	public double getSpindleRPM(int toolhead);
	
	public double getMotorRPM(int toolhead);
	
	public int getMotorSpeedPWM(int toolhead);

	public double getTemperature(int toolhead);

	public double getTemperatureSetting(int toolhead);

	public boolean hasAutomatedBuildPlatform(int toolhead);
	
	public double getPlatformTemperature(int toolhead);
	public double getPlatformTemperatureSetting(int toolhead);

	public Point5d getCurrentPosition(boolean b);

	public boolean isPassthroughDriver();
	
	public Version getVersion();
	public Version getPreferredVersion();
}
