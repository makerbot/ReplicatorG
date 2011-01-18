package replicatorg.app.util.serial;

/**
 * Serial.Name objects are simple compact objects that hold the name
 * of a serial port, along with the port's current availability.
 */
public class Name implements Comparable<Name> {
	private String name;
	private String alias;
	private boolean available;
	public Name(String name, boolean available) {
		this.name = name;
		this.alias = null;
		this.available = available;
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