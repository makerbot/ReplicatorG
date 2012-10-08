package replicatorg.drivers.gen3;


/**
 * Enum for VendorID and ProductId comparison, 
 * @author farmckon
 *
 */
public enum VidPid {
	UNKNOWN (0X0000, 0X000),
	MIGHTY_BOARD (0x23C1, 0xB404), //Board 404!
	THE_REPLICATOR(0x23C1, 0xD314), //Dean 314
	THE_REPLICATOR_2(0x23C1, 0xB015);  // BOTS as leet
	final int pid; //productId (same as USB product id)
	final int vid; //vendorId (same as USB vendor id)
	
	private VidPid(int pid, int vid)
	{
		this.pid = pid;
		this.vid = vid;
	}
	
	
	/** Create a PID/VID if we know how to, 
	 * otherwise return unknown.
	 * @param bytes 4 byte array of PID/VID
	 * @return
	 */
	public static VidPid getPidVid(byte[] bytes)
	{
		if (bytes != null && bytes.length >= 4){
			int vid = ((int) bytes[0]) & 0xff;
			vid += (((int) bytes[1]) & 0xff) << 8;
			int pid = ((int) bytes[2]) & 0xff;
			pid += (((int) bytes[3]) & 0xff) << 8;
			for (VidPid known : VidPid.values())
			{
				if(known.equals(vid,pid)) return known; 
			}
		}
		return VidPid.UNKNOWN;
	}
	
	public boolean equals(VidPid VidPid){
		if (VidPid.vid == this.vid && 
			VidPid.pid == this.pid)
			return true;
		return false;
	}
	public boolean equals(int pid, int vid){
		if (vid == this.vid && 
			pid == this.pid)
			return true;
		return false;
	}
	
}

