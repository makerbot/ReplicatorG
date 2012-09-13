package replicatorg.drivers.gen3;

import java.util.logging.Level;

import replicatorg.app.Base;
import replicatorg.drivers.InteractiveDisplay;
import replicatorg.drivers.Version;
import replicatorg.machine.model.ToolModel;

public class Replicator2 extends MightyBoard 
	implements InteractiveDisplay
{

	
	public Replicator2() {
		super();
		minimumVersion = new Version(6,0);
		preferredVersion = new Version(6,0);
		minimumAccelerationVersion = new Version(6,0);
		minAdvancedFeatureVersion = new Version(6,0);
	}
	
	
	/** try to verify our acutal machine matches our selected machine
	 * @param vid vendorId (same as usb vendorId)
	 * @param pid product (same as usb productId)
	 * @return true if we can verify this is a valid machine match
	 */
	@Override
	public boolean verifyMachineId()
	{
		if ( this.machineId == VidPid.UNKNOWN ) {
			readMachineVidPid();
		}
		return this.machineId.equals(VidPid.THE_REPLICATOR_2);
	}
	
	/**
	 * This function is called just after a connection is made, to do initial
	 * sycn of any values stored in software that are not available by 
	 * s3g command. For example, stepper vRef
	 * @see replicatorg.drivers.gen3.Sanguino3GDriver#pullInitialValuesFromBot()
	 */
	@Override
	public boolean initializeBot()
	{
		boolean initOk = super.initializeBot();
		if (initOk) {
			//		minimumVersion = new Version(6,0);
			if (this.version.compareTo(getMinimumVersion()) < 0) {
				Base.logger.log(Level.SEVERE,
                            "\n********************************************************\n"
                            + "The Replicator 2 Requires Firmware newer than " + getMinimumVersion() +"\n"
                            + "Current firmware is version"+ this.version + "\n"
                            + "Update your firmware  or select a new bot.\n"
                            + "********************************************************");
				return false;
			}			
		}
		return initOk;
	}
	
	/**
	 * Gets the 'Advanced Version field, containing extended
	 * build version info
	 *  (_Adv functions are advanced, tied to minAdvancedVersionNumber 
	 * @return
	 */
	public boolean getVersion_Adv()
	{
		/*TODO:
		  payload = struct.pack(
	      '<BH',
	      host_query_command_dict['GET_ADVANCED_VERSION'],
	      s3g_version,
	    )

	    response = self.writer.send_query_payload(payload)
	    [response_code, version, internal_version, 
	    reserved_a, reserved_b] = makerbot_driver.Encoder.unpack_response('<BHHHH', response)

	    version_info = {
	    'Version' : version,
	    'InternalVersion' : internal_version,
	    'ReservedA'  : reserved_a,
	    'ReservedB'  : reserved_b,
	    }
	   */
		return false;
	}

	public boolean getBuildStatistics_Adv()
	{
		/*
		payload = struct.pack(
		  '<B',
		  host_query_command_dict['GET_BUILD_STATS'],
		)		
		response = self.writer.send_query_payload(payload)
		
		[response_code, build_state,
		 build_hours, build_minutes,
		 line_number, reserved] = makerbot_driver.Encoder.unpack_response('<BBBBLL', response)
		
		info = {
		'BuildState' : build_state,
		'BuildHours' : build_hours,
		'BuildMinutes' : build_minutes,
		'LineNumber' : line_number,
		'Reserved' : reserved
		}
		return info
		 */
		return false;
	}

	public boolean getMotheboardModStats_Adv() 
	{/* """
		Retrieve bits of information about the motherboard
		POWER_ERRPR : An error was detected with the system power.
		HEAT_SHUTDOWN : The heaters were shutdown because the bot was inactive for over 20 minutes
		@return: A python dictionary of various flags and whether theywere set or not at reset
		"""
		payload = struct.pack(
		  '<B',
		  host_query_command_dict['GET_MOTHERBOARD_STATUS'],
		)
		
		response = self.writer.send_query_payload(payload)
		
		
		[response_code, bitfield] = makerbot_driver.Encoder.unpack_response('<BB', response)
		
		bitfield = makerbot_driver.Encoder.decode_bitfield(bitfield)
		flags = {
		'POWER_ERROR'   : bitfield[7],
		'HEAT_SHUTDOWN' : bitfield[6],
		}
		return flags
		
		 */
		return false;
	}

	public boolean getToolheadStats_Adv()
	{
		/*
		 *  def get_tool_status(self, tool_index):
	    """
	    Retrieve some information about the tool, as a status dictionary
	    statusDict = {
	      ExtruderReady : The extruder has reached target temp
	      ExtruderNotPluggedIn : The extruder thermocouple is not detected by the bot
	      ExturderOverMaxTemp : The temperature measured at the extruder is greater than max allowed
	      ExtruderNotHeating : In the first 40 seconds after target temp was set, the extruder is not heating up as expected
	      ExtruderDroppingTemp : After reaching and maintaining temperature, the extruder temp has dropped 30 degrees below target
	      PlatformError: an error was detected with the platform heater (if the tool supports one).  
	        The platform heater will fail if an error is detected with the sensor (thermocouple) 
	        or if the temperature reading appears to be unreasonable.
	      ExtruderError: An error was detected with the extruder heater (if the tool supports one).  
	        The extruder heater will fail if an error is detected with the sensor (thermocouple) or 
	        if the temperature reading appears to be unreasonable
	      }
	     @param int tool_index: The tool we would like to query for information
	     @return A dictionary containing status information specified above 
	   """
	    response = self.tool_query(tool_index, slave_query_command_dict['GET_TOOL_STATUS'])
	
	    [resonse_code, bitfield] = makerbot_driver.Encoder.unpack_response('<BB', response)
	
	    bitfield = makerbot_driver.Encoder.decode_bitfield(bitfield)
	
	    returnDict = {
	      "ExtruderReady"        : bitfield[0],
	      "ExtruderNotPluggedIn" : bitfield[1],
	      "ExtruderOverMaxTemp"  : bitfield[2],
	      "ExtruderNotHeating"   : bitfield[3],
	      "ExtruderDroppingTemp" : bitfield[4],
	      "PlatformError"        : bitfield[6],
	      "ExtruderError"        : bitfield[7],
	    }
	    return returnDict
		 */
		return false;
	}

	public boolean getCommunicationStats()
	{
		/*
	  def get_communication_stats(self):
	    """
	    Get some communication statistics about traffic on the tool network from the Host.
	    @return a dictionary of communication stats, keyed by stat name
	    """
	    payload = struct.pack(
	      '<B',
	      host_query_command_dict['GET_COMMUNICATION_STATS'],
	    )
	
	    response = self.writer.send_query_payload(payload)
	
	    [response_code,
	     packetsReceived,
	     packetsSent,
	     nonResponsivePacketsSent,
	     packetRetries,
	     noiseBytes] = makerbot_driver.Encoder.unpack_response('<BLLLLL', response)
	
	    info = {
	    'PacketsReceived' : packetsReceived,
	    'PacketsSent' : packetsSent,
	    'NonResponsivePacketsSent' : nonResponsivePacketsSent,
	    'PacketRetries' : packetRetries,
	    'NoiseBytes' : noiseBytes,
	    }
	    return info
	
		 */
		return false;
	}

}
