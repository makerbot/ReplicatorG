/**
 * @author Erik de Bruijn
 * For RepRap and Ultimaker's with 5D control
 */
package replicatorg.drivers;

public interface RealtimeControl {

	public void enableRealtimeControl(boolean enable);
	public boolean hasFeatureRealtimeControl();
	public double getFeedrateMultiplier();
	public double getTravelFeedrateMultiplier();
	public double getExtrusionMultiplier();
	public boolean setFeedrateMultiplier(double multiplier);
	public boolean setTravelFeedrateMultiplier(double multiplier);
	public boolean setExtrusionMultiplier(double multiplier);
	public boolean setFeedrateLimit(double limit);
	public double getFeedrateLimit();
	public void setDebugLevel(int level);
	public int getDebugLevel();
	public void enableFan();
	public void disableFan();
	// TODO: realtime backlash compensation?
	// TODO: realtime X-Y offsetting? (for when it has skipped steps)
	
}
