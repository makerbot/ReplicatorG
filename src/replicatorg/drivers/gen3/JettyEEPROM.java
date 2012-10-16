package replicatorg.drivers.gen3;

class JettyG3EEPROM extends Sanguino3GEEPRPOM {
	final public static int VERSION_LOW                = 0x0000;
	final public static int VERSION_HIGH               = 0x0001;
	final public static int AXIS_INVERSION             = 0x0002;
	final public static int ENDSTOP_INVERSION          = 0x0003;
	final public static int MACHINE_NAME               = 0x0020;
	final public static int AXIS_HOME_POSITIONS        = 0x0060;
	final public static int ESTOP_CONFIGURATION        = 0x0074;
	final public static int TOOL0_TEMP                 = 0x0080;
	final public static int TOOL1_TEMP                 = 0x0081;
	final public static int PLATFORM_TEMP              = 0x0082;
	final public static int EXTRUDE_DURATION           = 0x0083;
	final public static int EXTRUDE_MMS                = 0x0084;
	final public static int MOOD_LIGHT_SCRIPT          = 0x0085;
	final public static int MOOD_LIGHT_CUSTOM_RED      = 0x0086;
	final public static int MOOD_LIGHT_CUSTOM_GREEN    = 0x0087;
	final public static int MOOD_LIGHT_CUSTOM_BLUE     = 0x0088;
	final public static int JOG_MODE_SETTINGS          = 0x0089;
	final public static int BUZZER_REPEATS             = 0x008A;
	final public static int STEPS_PER_MM_X             = 0x008B;
	final public static int STEPS_PER_MM_Y             = 0x0093;
	final public static int STEPS_PER_MM_Z             = 0x009B;
	final public static int STEPS_PER_MM_A             = 0x00A3;
	final public static int STEPS_PER_MM_B             = 0x00AB;
	final public static int FILAMENT_USED              = 0x00B3;
	final public static int FILAMENT_USED_TRIP         = 0x00BB;
	final public static int ABP_COPIES                 = 0x00C3;
	final public static int PREHEAT_DURING_ESTIMATE    = 0x00C4;
	final public static int OVERRIDE_GCODE_TEMP        = 0x00C5;
	final public static int STEPPER_DRIVER             = 0x0126;
	final public static int ACCEL_MAX_FEEDRATE_X       = 0x0127;
	final public static int ACCEL_MAX_FEEDRATE_Y       = 0x012B;
	final public static int ACCEL_MAX_FEEDRATE_Z       = 0x012F;
	final public static int ACCEL_MAX_FEEDRATE_A       = 0x0133;
	final public static int ACCEL_MAX_FEEDRATE_B       = 0x0137;
	final public static int ACCEL_MAX_ACCELERATION_X   = 0x013B;
	final public static int ACCEL_MAX_ACCELERATION_Y   = 0x013F;
	final public static int ACCEL_MAX_ACCELERATION_Z   = 0x0143;
	final public static int ACCEL_MAX_ACCELERATION_A   = 0x0147;
	final public static int ACCEL_MAX_EXTRUDER_NORM    = 0x014B;
	final public static int ACCEL_MAX_EXTRUDER_RETRACT = 0x014F;
	final public static int ACCEL_E_STEPS_PER_MM       = 0x0153;
	final public static int ACCEL_MIN_FEED_RATE        = 0x0157;
	final public static int ACCEL_MIN_TRAVEL_FEED_RATE = 0x015B;
	final public static int ACCEL_ADVANCE_K2           = 0x015F;
	final public static int ACCEL_MIN_PLANNER_SPEED    = 0x0163;
	final public static int ACCEL_ADVANCE_K            = 0x0167;
	final public static int ACCEL_NOODLE_DIAMETER	   = 0x016B;
	final public static int ACCEL_MIN_SEGMENT_TIME     = 0x016F;
	final public static int LCD_TYPE                   = 0x0173;
	final public static int ENDSTOPS_USED              = 0x0174;
	final public static int HOMING_FEED_RATE_X         = 0x0175;
	final public static int HOMING_FEED_RATE_Y         = 0x0179;
	final public static int HOMING_FEED_RATE_Z         = 0x017D;
	final public static int ACCEL_REV_MAX_FEED_RATE    = 0x0181;
	final public static int ACCEL_EXTRUDER_DEPRIME     = 0x0185;
	final public static int ACCEL_SLOWDOWN_LIMIT       = 0x0189;
	final public static int ACCEL_CLOCKWISE_EXTRUDER   = 0x018D;
	final public static int INVERTED_EXTRUDER_5D       = 0x0191;
	final public static int ACCEL_MAX_SPEED_CHANGE_X   = 0x0192;
	final public static int ACCEL_MAX_SPEED_CHANGE_Y   = 0x0196;
	final public static int ACCEL_MAX_SPEED_CHANGE_Z   = 0x019A;
	final public static int ACCEL_MAX_SPEED_CHANGE_A   = 0x019E;
	final public static int RAM_USAGE_DEBUG            = 0x01D0;
}

class SailfishEEPROM extends JettyG3EEPROM {
	final public static int SLOWDOWN_FLAG			= 0x0189;
	final public static int ACCEL_MAX_SPEED_CHANGE_B	= 0x01A2;
	final public static int ACCEL_MAX_ACCELERATION_B	= 0x01A6;
	final public static int ACCEL_EXTRUDER_DEPRIME_B	= 0x01AA;
	final public static int TOOL_COUNT			= 0x01AE;
	final public static int TOOLHEAD_OFFSET_SETTINGS	= 0x01B0;
	final public static int AXIS_LENGTHS			= 0x01BC;
	final public static int FILAMENT_LIFETIME_B             = 0x01D4;
	final public static int DITTO_PRINT_ENABLED             = 0x01DC;
	final public static int VID_PID_INFO		        = 0x01E5;
}


class JettyMBEEPROM extends Sanguino3GEEPRPOM {
	final public static int ACCELERATION_STATE             = 0x023E;
	final public static int BOT_STATUS_BYTES               = 0x018A;
	final public static int AXIS_LENGTHS                   = 0x018C;
	final public static int AXIS_STEPS_PER_MM              = 0x01A0;
	final public static int FILAMENT_LIFETIME              = 0x01B4;
	final public static int FILAMENT_TRIP                  = 0x01C4;
	final public static int PROFILES_BASE                  = 0x01D5;
	final public static int PROFILES_INIT                  = 0x023D;
	final public static int MAX_ACCELERATION_AXIS          = 0x0240;
	final public static int MAX_ACCELERATION_NORMAL_MOVE   = 0x024A;
	final public static int MAX_ACCELERATION_EXTRUDER_MOVE = 0x024C;
	final public static int MAX_SPEED_CHANGE               = 0x024E;
	final public static int JKN_ADVANCE_K                  = 0x0258;
	final public static int JKN_ADVANCE_K2                 = 0x025C;
	final public static int EXTRUDER_DEPRIME_STEPS         = 0x0260;
	final public static int SLOWDOWN_FLAG                  = 0x0264;
	final public static int DEFAULTS_FLAG                  = 0x0265;
	final public static int FUTURE_USE                     = 0x0266;
	final public static int AXIS_MAX_FEEDRATES             = 0x027A;
	final public static int OVERRIDE_GCODE_TEMP            = 0x0FFD;
	final public static int HEAT_DURING_PAUSE              = 0x0FFE;
}
