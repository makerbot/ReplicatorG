package replicatorg.drivers.gen3;

import java.util.logging.Level;

import replicatorg.app.Base;

/**
 * Packet response wrapper, with convenience functions for reading off values in
 * sequence and retrieving the response code.
 */
public class PacketResponse {

	/** The response codes at the start of every response packet. */
	enum ResponseCode {
		GENERIC_ERROR("Generic Error"), 
		OK("OK"), 
		BUFFER_OVERFLOW("Buffer full"), 
		CRC_MISMATCH("CRC mismatch"), 
		QUERY_OVERFLOW("Query overflow"), 
		UNSUPPORTED("Unsupported command"),
		TIMEOUT("Packet timeout"),
		UNKNOWN("Unknown code")
		;
		
		private final String message;
		
		ResponseCode(String message) {
			this.message = message;
		}
		
		public String getMessage() { return message; }
		
		// TODO: This allows us to respond to both pre and post 2.92 commands.
		public static ResponseCode fromInt(int value) { 
			switch(value) {
			case 0x0:
			case 0x80:
				return GENERIC_ERROR;
			case 0x1:
			case 0x81:
				return OK;
			case 0x2:
			case 0x82:
				return BUFFER_OVERFLOW;
			case 0x3:
			case 0x83:
				return CRC_MISMATCH;
			case 0x4:
			case 0x84:
				return QUERY_OVERFLOW;
			case 0x5:
			case 0x85:
				return UNSUPPORTED;
			case 0x6:
			case 0x86:
				return OK; // more packets expected?
			case 127:
				return TIMEOUT;
			}
			return UNKNOWN;
		}
	};


	byte[] payload;

	int readPoint = 1;

	public PacketResponse() {
		payload = null;
	}

	public PacketResponse(byte[] p) {
		payload = p;
	}

	public boolean isEmpty() {
		return payload == null;
	}
	
	/**
	 * Prints a debug message with the packet response code decoded, along with
	 * the packet's contents in hex.
	 */
	public void printDebug() {
		ResponseCode code = getResponseCode(); 
		String msg = code.getMessage();
		// only print certain messages
		Level level = Level.INFO;
		if (code != ResponseCode.OK && code != ResponseCode.BUFFER_OVERFLOW) level = Level.WARNING;
		
		if (Base.logger.isLoggable(level)) {
			Base.logger.log(level,"Packet response code: " + msg);
			StringBuffer buf = new StringBuffer("Packet payload: ");
			if (payload.length <= 1) {
				buf.append("empty");
			} else for (int i = 1; i < payload.length; i++) {
				buf.append(Integer.toHexString(payload[i] & 0xff));
				buf.append(" ");
			}
			Base.logger.log(level,buf.toString());
		}
	}

	/**
	 * Retrieve the packet payload.
	 * 
	 * @return an array of bytes representing the payload.
	 */
	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Get the next 8-bit value from the packet payload.
	 */
	int get8() {
		if (payload.length > readPoint)
			return ((int) payload[readPoint++]) & 0xff;
		else {
			Base.logger.fine("Error: payload not big enough.");
			return 0;
		}
	}

	/**
	 * Get the next 16-bit value from the packet payload.
	 */
	int get16() {
		return get8() + (get8() << 8);
	}

	/**
	 * Get the next 32-bit value from the packet payload.
	 */
	int get32() {
		return get16() + (get16() << 16);
	}

	/**
	 * Does the response code indicate that the command was successful?
	 */
	public boolean isOK() {
		return getResponseCode() == ResponseCode.OK;
	}

	public ResponseCode getResponseCode() {
		if (payload != null && payload.length > 0)
			return ResponseCode.fromInt(payload[0] & 0xff);
		else return ResponseCode.GENERIC_ERROR;
	}

	public static PacketResponse okResponse() {
		final byte[] okPayload = {1,1,1,1,1,1,1,1}; // repeated 1s to fake out queries
		return new PacketResponse(okPayload);
	}

	public static PacketResponse timeoutResponse() {
		final byte[] errorPayload = {127,0,0,0,0,0,0,0}; // repeated 0s to fake out queries
		return new PacketResponse(errorPayload);
	}
}
