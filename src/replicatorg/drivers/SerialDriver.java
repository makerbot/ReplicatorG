/**
 * 
 */
package replicatorg.drivers;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.exceptions.SerialException;
import replicatorg.app.tools.XML;
import replicatorg.app.util.serial.Serial;
import replicatorg.app.util.serial.SerialFifoEventListener;

/**
 * @author phooky
 *
 */
public abstract class SerialDriver extends DriverBaseImplementation implements UsesSerial {

	protected Serial serial;
	
    private String portName;
    private int rate;
    private char parity;
    private int databits;
    private int stopbits;

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
        stopbits = Base.preferences.getInt("serial.stopbits",1);
    }
    
	@Override public void loadXML(Node xml) {
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
	
	@Override public synchronized void openSerial(String portName) {
		// Grab a lock
		serialLock.writeLock().lock();
		
		// Now, try to create the new serial device
		Serial newConnection = null;
		try {

			Base.logger.info("Connecting to machine using serial port: " + portName);
			newConnection = new Serial(portName, rate, parity, databits, stopbits);
		} catch (SerialException e) {
			String msg = e.getMessage();
			Base.logger.severe("Connection error: " + msg);
			setError("Connection error: " + msg);
		}

		if (newConnection != null) {
			// TODO: Do we need to explicitly dispose this?
			if (this.serial != null) {
				synchronized(this.serial) {
					this.serial.dispose();
					this.serial = null;
				}
			}
			
			// Finally, set the new serial port
			setInitialized(false);
			this.serial = newConnection;

			// asynch option: the serial port forwards all received data in FIFO format via 
			// serialByteReceivedEvent if the driver implements SerialFifoEventListener.
			if (this instanceof SerialFifoEventListener && serial != null) {
				serial.listener.set( (SerialFifoEventListener) this );
			}
		}
		serialLock.writeLock().unlock();
	}
	
	// TODO: Move all of this to a new object that causes this when it is destroyed.
	@Override public void closeSerial() {
		serialLock.writeLock().lock();
		if (serial != null)
			serial.dispose();
		serial = null;
		serialLock.writeLock().unlock();
	}

	@Override public boolean isConnected() {
		return (this.serial != null && this.serial.isConnected());
	}
	
	@Override public char getParity() {
		return parity;
	}

	@Override public String getPortName() {
		return portName;
	}

	@Override public int getDataBits() {
		return databits;
	}
	
	@Override public int getRate() {
		return rate;
	}

	@Override public float getStopBits() {
		return stopbits;
	}
		
	@Override public boolean isExplicit() {
		return explicit;
	}
	
	@Override public void dispose() {
		closeSerial();
		super.dispose();
	}
}
