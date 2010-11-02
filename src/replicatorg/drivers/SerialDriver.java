/**
 * 
 */
package replicatorg.drivers;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.Serial;
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
			synchronized(serial) {
				this.serial.dispose();
				this.serial = null;
			}
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
		
	public boolean isExplicit() { return explicit; }
	
	public void dispose() {
		super.dispose();
		if (serial != null)
			serial.dispose();
		serial = null;
	}
}
