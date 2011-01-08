/**
 * 
 */
package replicatorg.drivers;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.tools.XML;
import replicatorg.app.util.serial.Serial;
import replicatorg.app.util.serial.SerialFifoEventListener;

/**
 * @author phooky
 *
 */
public class SerialDriver extends DriverBaseImplementation implements UsesSerial {

	protected Serial serial;
	
    private String portName;
    private int rate;
    private char parity;
    private int databits;
    private float stopbits;

    private boolean explicit = false;
	
    /** Lock for multi-threaded access to this driver's serial port. */
	private final ReentrantReadWriteLock serialLock = new ReentrantReadWriteLock();
	/** Locks the serial object as in use so that it cannot be disposed until it is 
	 * unlocked. Multiple threads can hold this lock concurrently. */
	public final ReadLock serialInUse = serialLock.readLock();
	
    protected SerialDriver() {
    	portName = Base.preferences.get("serial.portname",null);
    	rate = Base.preferences.getInt("serial.debug_rate",19200);
    	String parityStr = Base.preferences.get("serial.parity","N");
    	if (parityStr == null || parityStr.length() < 1) { 
    		parity = 'N';
    	} else {
    		parity = parityStr.charAt(0);
    	}
        databits = Base.preferences.getInt("serial.databits",8);
        stopbits = Base.preferences.getFloat("serial.stopbits",1f);
    }
    
	public void loadXML(Node xml) {
		super.loadXML(xml);
        // load from our XML config, if we have it.
        if (XML.hasChildNode(xml, "portname")) {
                portName = XML.getChildNodeValue(xml, "portname");
                explicit = true;
        }
        if (XML.hasChildNode(xml, "rate"))
                rate = Integer.parseInt(XML.getChildNodeValue(xml, "rate"));
        if (XML.hasChildNode(xml, "parity"))
                parity = XML.getChildNodeValue(xml, "parity").charAt(0);
        if (XML.hasChildNode(xml, "databits"))
                databits = Integer.parseInt(XML.getChildNodeValue(xml, "databits"));
        if (XML.hasChildNode(xml, "stopbits"))
                stopbits = Integer.parseInt(XML.getChildNodeValue(xml, "stopbits"));
	}

// diplo1d: see this fix by kintel that conflicted when merging. You also fixed it but differently:
// https://github.com/makerbot/ReplicatorG/commit/899c7c8e059d986ac0744ca4ab1f2f44efaae4b5
// your fix:
//	public void setSerial(Serial serial) {
//		serialLock.writeLock().lock();
		
// kintel had just changed it to "synchronized".
//	public synchronized void setSerial(Serial serial) {
      public synchronized void setSerial(Serial serial) {
                serialLock.writeLock().lock();
//		serialWriteLock.lock();
		if (this.serial == serial)
		{
			serialLock.writeLock().unlock();
			return;
		}
		if (this.serial != null) {
			synchronized(this.serial) {
				this.serial.dispose();
				this.serial = null;
			}
		}
		setInitialized(false);
		this.serial = serial;

		// asynch option: the serial port forwards all received data in FIFO format via 
		// serialByteReceivedEvent if the driver implements SerialFifoEventListener.
		if (this instanceof SerialFifoEventListener && serial != null)
			serial.listener.set( (SerialFifoEventListener) this );

		serialLock.writeLock().unlock();
	}

	public Serial getSerial() { return serial; }
	
	public char getParity() {
		return parity;
	}

	public String getPortName() {
		return portName;
	}

	public int getDataBits() {
		return databits;
	}
	
	public int getRate() {
		return rate;
	}

	public float getStopBits() {
		return stopbits;
	}
		
	public boolean isExplicit() { return explicit; }
	
	public void dispose() {
		serialLock.writeLock().lock();
		super.dispose();
		if (serial != null)
			serial.dispose();
		serial = null;
		serialLock.writeLock().unlock();
	}
}
