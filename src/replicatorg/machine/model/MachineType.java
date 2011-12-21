package replicatorg.machine.model;

/**
 * An enumeration of the names of machines
 * @author Ted
 *
 */
public enum MachineType
{
	THE_REPLICATOR("The Replicator"),
	THINGOMATIC("Thing-O-Matic"),
	CUPCAKE("Cupcake");
	
	private String name;
	
	private MachineType(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
}
