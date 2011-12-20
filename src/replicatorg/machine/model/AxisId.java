package replicatorg.machine.model;

/**
 * This enum represents the available axis on a printing machine.
 * 
 * For MigtyBoard, we have remapped B/C to be extruder motors. 
 *
 */
public enum AxisId {
	X(0,"The X axis"),
	Y(1,"The Y axis"),
	Z(2,"The Z axis"),

	//A(3, "rotational around X"),
	A(3,"Extruder motor T0 (A) drive"),
	//B(4,"rotational around Y"),
	B(4,"Extruder motor T1 (B) drive"),
	C(5, "rotational around Z"),

	U(6,"parallel to X"),
	V(7,"parallel to Y"),
	W(8,"parallel to Z");
	
	private final int index;
	private final String info;
	
	private AxisId(int index, String info) { 
		this.index = index;
		this.info = info;
	} 
	
	public int getIndex() { return index; }
	
	public static AxisId getAxis(String name){
		for (AxisId ax : AxisId.values() ) {
			if ( ax.name().toLowerCase().equals(name.toLowerCase() ) )
				return ax;
		}
		return null;
	}
}
