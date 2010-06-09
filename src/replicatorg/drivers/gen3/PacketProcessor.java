package replicatorg.drivers.gen3;

import java.util.logging.Level;

import replicatorg.app.Base;
import replicatorg.app.tools.IButtonCrc;

/**
 * A class for keeping track of the state of an incoming packet and storing
 * its payload.
 */
public class PacketProcessor implements PacketConstants {
	
	enum PacketState {
		START, LEN, PAYLOAD, CRC, LAST
	}

	PacketState packetState = PacketState.START;

	int payloadLength = -1;

	int payloadIdx = 0;

	byte[] payload;

	byte targetCrc = 0;

	IButtonCrc crc;

	/**
	 * Reset the packet's state. (The crc is (re-)generated on the length byte
	 * and thus doesn't need to be reset.(
	 */
	public void reset() {
		packetState = PacketState.START;
	}

	/**
	 * Create a PacketResponse object that contains this packet's payload.
	 * 
	 * @return A valid PacketResponse object
	 */
	public PacketResponse getResponse() {
		PacketResponse pr = new PacketResponse(payload);
		return pr;
	}

	/**
	 * Process the next byte in an incoming packet.
	 * 
	 * @return true if the packet is complete and valid; false otherwise.
	 */
	public boolean processByte(byte b) {

		if (Base.logger.isLoggable(Level.FINER)) {
			if (b >= 32 && b <= 127)
				Base.logger.log(Level.FINER,"IN: Processing byte "
						+ Integer.toHexString((int) b & 0xff) + " (" + (char) b
						+ ")");
			else
				Base.logger.log(Level.FINER,"IN: Processing byte "
						+ Integer.toHexString((int) b & 0xff));
		}

		switch (packetState) {
		case START:
			if (b == START_BYTE) {
				packetState = PacketState.LEN;
			} else {
				// throw exception?
			}
			break;

		case LEN:
			if (Base.logger.isLoggable(Level.FINER)) {
				Base.logger.log(Level.FINER,"Length: " + (int) b);
			}

			payloadLength = ((int) b) & 0xFF;
			payload = new byte[payloadLength];
			crc = new IButtonCrc();
			packetState = (payloadLength > 0) ? PacketState.PAYLOAD : PacketState.CRC;
			break;

		case PAYLOAD:
			// sanity check
			if (payloadIdx < payloadLength) {
				payload[payloadIdx++] = b;
				crc.update(b);
			}
			if (payloadIdx >= payloadLength) {
				packetState = PacketState.CRC;
			}
			break;

		case CRC:
			targetCrc = b;

			if (Base.logger.isLoggable(Level.FINER)) {
				Base.logger.log(Level.FINER,"Target CRC: "
						+ Integer.toHexString((int) targetCrc & 0xff)
						+ " - expected CRC: "
						+ Integer.toHexString((int) crc.getCrc() & 0xff));
			}
			if (crc.getCrc() != targetCrc) {
				throw new java.lang.RuntimeException("CRC mismatch on reply");
			}
			return true;
		}
		return false;
	}
}

