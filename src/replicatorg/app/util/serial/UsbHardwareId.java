
package replicatorg.app.util.serial;

/**
 * This enum represents possible vendor and product ID's a USB device may connect via
 * This also flags if it's a verified branded USB ID (ie, exactly and only that device will 
 * use the idea) or not (ie, it might be an arduino, or some other general device)
 * 
 *
 */
enum UsbHardwareId {
	MIGHTY_BOARD(0x32C1, 0xB404, "MightyBoard", true), // "usb-MakerBot_Industries_(MightyBoard)_
	NONE(0x00, 0x00, "None", false);

	private int pid;			/// USB Product ID
	private int vid;			/// USB vendorId
	private int hwVersion;		/// < hw version of the device
	private String name;		/// < Display name of the device 
	private boolean verified;  /// < this is a valid verified VID/PID only for one hw config

	private UsbHardwareId(int vendorId,int productId, String name, boolean verified) 
	{ 
		this.pid= productId;
		this.vid= vendorId;
		this.name = name;
		this.verified = verified;
	} 

	public int getVid() { return vid;}
	public int getPid() { return pid;}
	public boolean isVerified() { return this.verified; }
	
	
}


