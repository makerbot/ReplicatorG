package replicatorg.app.util.serial;

/**
 * Serial.Name objects are simple compact objects that hold the name
 * of a serial port, along with the port's current availability,
 * and what kind of hardware the connection is (if it's known)
 */
public class Name implements Comparable<Name> {
	private String name;
	private String alias;
	private boolean available;
	private UsbHardwareId hardwareId;/// Id to represent hardware type.
									 /// do not set unless the id is well known
	
	
	public Name(String name, boolean available) {
		this.name = name;
		this.alias = null;
		this.available = available;
		this.hardwareId = UsbHardwareId.NONE;
	}
	
	public Name(String name, String alias, boolean available) {
		this.name = name;
		this.alias = alias;
		this.available = available;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public String getName() {
		return name;
	}
	
	public void setHardwareId(UsbHardwareId newId) {
		this.hardwareId = newId;
	}

	/** Checks if this Serial.name is a valid connection port
	 * for a machine name (as specified in machines.xml
	 */
	public boolean isValidConnectorForMachineName(String machineName) {
		/// if this Serial.Name is a MightyBoard, and the machine contains
		// Replicator or MightyBoard, we can verif we are connectable
		if( this.hardwareId == UsbHardwareId.MIGHTY_BOARD ) {
			if ( machineName.contains("MightyBoard") || machineName.contains("Replicator") )
				return true;
		}
		return false;
	}
	
	public boolean isVerified() { return this.hardwareId.isVerified(); }
	
	/**
	 * @return true if the port can be successfully opened by ReplicatorG.
	 */
	public boolean isAvailable() {
		return available;
	}
	public int compareTo(Name other) {
		// There should only be one entry per name, so we're going to presume
		// that any two entries with identical names are effectively the same.
		// This also simplifies sorting, etc.
		return name.compareTo(other.name);
	}
	
	public String toString() {
		if (alias != null) {
			return this.name + " (" + alias + ")";
		}
		return this.name;
	}

}