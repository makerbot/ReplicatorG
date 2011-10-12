package replicatorg.machine.model;

public class WipeBoundary {

	boolean enabled;
	float loc;
	public WipeBoundary(float f, boolean t)
	{
		loc = f;
		enabled = t;
	}
	public boolean isEnabled()
	{
		return enabled;
	}
	public float getValue()
	{
		return loc;
	}
}
