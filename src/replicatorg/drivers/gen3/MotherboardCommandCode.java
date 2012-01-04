package replicatorg.drivers.gen3;

/**
 * An enumeration of the available command codes for the three-axis CNC
 * stage.
 */
public enum MotherboardCommandCode {
	VERSION(0,""),
	INIT(1,""),
	GET_BUFFER_SIZE(2,""),
	CLEAR_BUFFER(3,""),
	GET_POSITION(4,""),
	GET_RANGE(5,""),
	SET_RANGE(6,""),
	ABORT(7,""),
	PAUSE(8,""),
	PROBE(9,""),
	TOOL_QUERY(10,""),
	IS_FINISHED(11,""),
	READ_EEPROM(12,""),
	WRITE_EEPROM(13,""),
	
	CAPTURE_TO_FILE(14,""),
	END_CAPTURE(15,""),
	PLAYBACK_CAPTURE(16,""),
	
	RESET(17,""),

	NEXT_FILENAME(18,""),
	// Get the build name
	GET_BUILD_NAME(20,""),
	GET_POSITION_EXT(21,""),
	EXTENDED_STOP(22,""),
	
	BUILD_START_NOTIFICATION(23, "Notify the bot this is an object build, and what it is called"),
	BUILD_END_NOTIFICATION(24, "Notify the bot object build is complete."),

	GET_COMMUNICATION_STATS(25,""),
	
	// QUEUE_POINT_INC(128) obsolete
	QUEUE_POINT_ABS(129,""),
	SET_POSITION(130,""),
	FIND_AXES_MINIMUM(131,""),
	FIND_AXES_MAXIMUM(132,""),
	DELAY(133,""),
	CHANGE_TOOL(134,""),
	WAIT_FOR_TOOL(135,""),
	TOOL_COMMAND(136,""),
	ENABLE_AXES(137,""),
	QUEUE_POINT_EXT(139,""),
	SET_POSITION_EXT(140,""),
	WAIT_FOR_PLATFORM(141,""),
	QUEUE_POINT_NEW(142,""),
	
	STORE_HOME_POSITIONS(143,""),
	RECALL_HOME_POSITIONS(144,""),
	
	SET_STEPPER_REFERENCE_POT(145, "set the digital pot for stepper power reference"),
	SET_LED_STRIP_COLOR(146, "set an RGB value to blink the leds, optional blink trigger"), 
	SET_BEEP(147, "set a beep frequency and length"),

	PAUSE_FOR_BUTTON(148, "Wait until a user button push is recorded"),
	DISPLAY_MESSAGE(149, "Display a user message on the machine display"),
	SET_BUILD_PERCENT(150, "Manually override Build % info"),
	QUEUE_SONG(151, "Trigger a song stored by by ID on the machine"),
	RESET_TO_FACTORY(152, "Reset onboard preferences to the factory settings");
	
	
	private int code; 	/// id code of this packet in the s3g protocol
	private String info;/// rough info on what this packet does

	final String s3GVersion = "4.02"; /// just for our own reference of what version of firmware this targets

	
	private MotherboardCommandCode(int code, String info) {
		this.code = code;
		this.info = info;
	}
	
	
	int getCode() { return code; }
	
	String getInfo() { return info; }
}