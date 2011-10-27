package replicatorg.drivers.gen3;

/**
 * An enumeration of the available command codes for a tool.
 */
public enum ToolCommandCode {
	VERSION(0),
	INIT(1),
	GET_TEMP(2),
	SET_TEMP(3),
	SET_MOTOR_1_PWM(4),
	SET_MOTOR_2_PWM(5),
	SET_MOTOR_1_RPM(6),
	SET_MOTOR_2_RPM(7),
	SET_MOTOR_1_DIR(8),
	SET_MOTOR_2_DIR(9),
	TOGGLE_MOTOR_1(10),
	TOGGLE_MOTOR_2(11),
	TOGGLE_FAN(12),
	TOGGLE_VALVE(13),
	SET_SERVO_1_POS(14),
	SET_SERVO_2_POS(15),
	FILAMENT_STATUS(16),
	GET_MOTOR_1_RPM(17),
	GET_MOTOR_2_RPM(18),
	GET_MOTOR_1_PWM(19),
	GET_MOTOR_2_PWM(20),
	SELECT_TOOL(21),
	IS_TOOL_READY(22),
	READ_FROM_EEPROM(25),
	WRITE_TO_EEPROM(26),
	TOGGLE_ABP(27), 
	GET_PLATFORM_TEMP(30),
	SET_PLATFORM_TEMP(31),
	GET_SP(32),
	GET_PLATFORM_SP(33),
	GET_BUILD_NAME(34),
	IS_PLATFORM_READY(35),
	GET_TOOL_STATUS(36),
	GET_PID_STATE(37);
	
	private int code;
	private ToolCommandCode(int code) {
		this.code = code;
	}
	int getCode() { return code; }
}