package replicatorg.machine.model;

public enum AxisId {
	/** The X axis */
	X(0),
	/** The Y axis */
	Y(1),
	/** The Z axis */
	Z(2),
	/** The A axis (traditionally, rotational around X) */
	A(3),
	/** The B axis (traditionally, rotational around Y) */
	B(4),
	/** The C axis (traditionally, rotational around Z) */
	C(5),
	/** The U axis (traditionally, parallel to X) */
	U(6),
	/** The V axis (traditionally, parallel to Y) */
	V(7),
	/** The W axis (traditionally, parallel to Z) */
	W(8);
	
	private int index;
	
	private AxisId(int index) { this.index = index; } 
	public int getIndex() { return index; }
}
