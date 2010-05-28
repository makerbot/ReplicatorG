package replicatorg.model;

public interface BuildElement {
	public enum Type {
		MODEL("model"),
		GCODE("gcode");
		
		private String displayString;
		Type(String displayString) { this.displayString = displayString; }
		public String getDisplayString() { return displayString; }
	}

	public Type getType();
}
