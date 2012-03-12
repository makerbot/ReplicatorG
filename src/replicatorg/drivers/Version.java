package replicatorg.drivers;

public class Version implements Comparable<Version> {
	private int minor;
	private int major;

	public Version( int major, int minor ) {
		this.major = major;
		this.minor = minor;
	}
	
	@Override public boolean equals(Object o) {
		if (o instanceof Version) {
			Version v = (Version)o;
			return major == v.major && minor == v.minor;
		}
		return false;
	}
	
	@Override public int hashCode() {
		return (major * 31) + minor;
	}

	public boolean atLeast(Version v) {
		return compareTo(v) >= 0;
	}
	
	@Override public int compareTo(Version v) {
		if (major > v.major || ((major == v.major) && (minor > v.minor))) return 1;
		if (equals(v)) return 0;
		return -1;
	}
	
	@Override public String toString() {
		return Integer.toString(major) + "." + Integer.toString(minor);
	}

	public int getMajor() { return major; }
	public int getMinor() { return minor; }
}
