/**
 * 
 */
package replicatorg.drivers;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.Serial;
import replicatorg.app.exceptions.SerialException;
import replicatorg.app.tools.XML;

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

	public void setSerial(Serial serial) {
		if (this.serial == serial) return;
		if (this.serial != null) {
			this.serial.dispose();
			this.serial = null;
		}
		setInitialized(false);
		this.serial = serial;
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
	
	/**
	 * Autoscan manually scans available serial ports one by one. 
	 *
	 */
	public boolean autoscan() {
		assert !isInitialized();
		assert serial == null;
		
		if (explicit &&
			Base.preferences.getBoolean("serial.use_machines",true)) {
			// Attempt explicitly set port first
			try {
				Serial candidatePort = new Serial(portName,this);
				Base.logger.warning("Connecting to port "+portName+" specified in machine definition");
				setSerial(candidatePort);
				initialize();
				if (isInitialized()) return true;
				Base.logger.warning("failed: "+portName);
				setSerial(null);
			} catch (SerialException se) {
				se.printStackTrace();
			}
		}
		
		for (Serial.Name candidateName: Serial.scanSerialNames()) {
			if (!candidateName.isAvailable()) continue;
			// Open candidate and set it
			Serial candidatePort = null;
			try {
				candidatePort = new Serial(candidateName.getName(),this); 
			} catch (SerialException se) {
				se.printStackTrace();
			}
			if (candidatePort != null) {
				Base.logger.info("attempting candidate "+candidateName.getName());
				setSerial(candidatePort);
				initialize();
				if (isInitialized()) break;
				Base.logger.warning("failed: "+candidateName.getName());
				setSerial(null);
			}
		}
		return isInitialized();
	}
	
	public boolean isExplicit() { return explicit; }
	
	public void dispose() {
		super.dispose();
		if (serial != null)
			serial.dispose();
		serial = null;
	}
}
