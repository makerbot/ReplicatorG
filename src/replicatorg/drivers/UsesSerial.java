package replicatorg.drivers;

import replicatorg.app.Serial;

public interface UsesSerial {
	
	public void setSerial(Serial serial);
	
	public Serial getSerial();
	
	public String getPortName();
	
	public char getParity();
	
	public int getRate();
	
	public int getDataBits();
	
	public float getStopBits();
}
