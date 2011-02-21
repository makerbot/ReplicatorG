package replicatorg.app;

import java.util.EnumSet;

import replicatorg.app.DriverCommand.Command;
import replicatorg.machine.model.AxisId;
import replicatorg.util.Point5d;


/**
 * Driver commands are a set of atomic actions that can be run on the driver.
 * These commands must have a 1:1 relationship with calls to the Driver interface. 
 * @author matt.mets
 *
 */
public class DriverCommand {
	public enum Direction {
		CLOCKWISE,
		COUNTERCLOCKWISE,
	}
	
	public enum Command {
		READ_TEMPERATURE,
		GET_POSITION,
		
		INITIALIZE,
		SET_FEEDRATE,
		QUEUE_POINT,
		DELAY,
		EXECUTE_GCODE_LINE,
		SELECT_TOOL,
		SET_SPINDLE_DIRECTION,
		ENABLE_SPINDLE,
		DISABLE_SPINDLE,
		REQUEST_TOOL_CHANGE,
		ENABLE_FLOOD_COOLANT,
		DISABLE_FLOOD_COOLANT,
		ENABLE_MIST_COOLANT,
		DISABLE_MIST_COOLANT,
		OPEN_VALVE,
		CLOSE_VALVE,
		OPEN_CLAMP,
		CLOSE_CLAMP,
		ENABLE_DRIVES,
		DISABLE_DRIVES,
		OPEN_COLLET,
		CLOSE_COLLET,
		CHANGE_GEAR_RATIO,
		SET_MOTOR_DIRECTION,
		ENABLE_MOTOR,
		DISABLE_MOTOR,
		SET_TEMPERATURE,
		SET_PLATFORM_TEMPERATURE,
		SET_CHAMBER_TEMPERATURE,
		ENABLE_FAN,
		DISABLE_FAN,
		SET_MOTOR_SPEED_PWM,
		SET_MOTOR_RPM,
		STORE_HOME_POSITIONS,
		RECALL_HOME_POSITIONS,
		SET_OFFSET_X,
		SET_OFFSET_Y,
		SET_OFFSET_Z,
		HOME_AXIS_POSITIVE,
		HOME_AXIS_NEGATIVE,
		SET_SPINDLE_RPM,
		WAIT_UNTIL_BUFFER_EMPTY,
		SET_CURRENT_POSITION,
		RECONCILE_POSITION,
	}
	
	
	// TODO: This is questionable but should work for the moment.
	// replace with factory or similar.
	
	Command command;
	
	private int intParam;

	private double doubleParam;
	
	private Point5d pointParam;
	
	private Direction directionParam;
	
	private EnumSet<AxisId> axesParam;
	
	private String stringParam;
	
	public Command getCommand() {
		return this.command;
	}
	
	public int getInt() {
		return this.intParam;
	}

	public double getDouble() {
		return this.doubleParam;
	}
	
	public Point5d getPoint() {
		return this.pointParam;
	}
	
	public Direction getDirection() {
		return this.directionParam;
	}
	
	public EnumSet<AxisId> getAxes() {
		return this.axesParam;
	}

	public String getString() {
		return this.stringParam;
	}
	
	/**
	 * Constructor for a command with no arguments
	 * @param command
	 */
	public DriverCommand(Command command) {
		this.command = command;
	}
	
	/**
	 * Constructor for a command with an integer argument
	 * @param command
	 * @param param
	 */
	public DriverCommand(Command command, int param) {
		this.command = command;
		this.intParam = param;
	}
	
	public DriverCommand(Command command, int param, double d) {
		this.command = command;
		this.intParam = param;
		this.doubleParam = d;
	}
	
	public DriverCommand(Command command, double param) {
		this.command = command;
		this.doubleParam = param;
	}
	
	public DriverCommand(Command command, Point5d param) {
		this.command = command;
		this.pointParam = param;
	}
	
	public DriverCommand(Command command, Direction param) {
		this.command = command;
		this.directionParam = param;
	}
	
	public DriverCommand(Command command, EnumSet<AxisId> param) {
		this.command = command;
		this.axesParam = param;
	}
	
	public DriverCommand(Command command, String param) {
		this.command = command;
		this.stringParam = param;
	}

	public DriverCommand(Command command, EnumSet<AxisId> param, double d) {
		this.command = command;
		this.axesParam = param;
		this.doubleParam = d;
	}
	
	public String contentsToString() {
		String contents = new String();
		
		contents += "command=" + command.name();
		contents += " i=" + intParam;
		contents += " d=" + doubleParam;
		if (stringParam != null)
			contents += " s=" + stringParam;
		if (pointParam != null)
			contents += " p=" + pointParam.toString();
		if (directionParam != null)
			contents += " dir=" + directionParam.name();
		if (axesParam != null)
			contents += " a=" + axesParam.toString();
				
		return contents;
	}
}
