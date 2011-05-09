package replicatorg.drivers;

public interface UsesSerial {
	
	// Open the specified serial port
	public void openSerial(String portName);
	
	public void closeSerial();
	
	/**
	 * Retrieves the port name specified by the driver.  This may not
	 * be the same as the currently open port!  Examine the serial object
	 * to find out the actual port.
	 */
	public String getPortName();
	
	/**
	 * Retrieves the parity specified by the driver.  This may not
	 * be the same as the current parity!  Examine the serial object
	 * to find out the actual parity.
	 */
	public char getParity();
	
	/**
	 * Retrieves the baud rate specified by the driver.  This may not
	 * be the same as the current baud rate!  Examine the serial object
	 * to find out the actual baud rate.
	 */
	public int getRate();
	
	/**
	 * Retrieves the data bits specified by the driver.  This may not
	 * be the same as the current data bits!  Examine the serial object
	 * to find out the actual data bits.
	 */
	public int getDataBits();
	
	/**
	 * Retrieves the stop bits specified by the driver.  This may not
	 * be the same as the current stop bits!  Examine the serial object
	 * to find out the actual stop bits.
	 */
	public float getStopBits();
	
	/**
	 * @return True if a serial port has been opened
	 */
	public boolean isConnected();
	
	// Indicates that the serial port is explicitly specified in machines.xml
	public boolean isExplicit();
}
