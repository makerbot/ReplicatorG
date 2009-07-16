package replicatorg.app.tools;

/**
 * This is a Java implementation of the IButton/Maxim 8-bit CRC. Code ported
 * from the AVR-libc implementation, which is used on the RR3G end.
 */
public class IButtonCrc {

	private int crc = 0;

	/**
	 * Construct a new, initialized object for keeping track of a CRC.
	 */
	public IButtonCrc() {
		crc = 0;
	}

	/**
	 * Update the CRC with a new byte of sequential data. See
	 * include/util/crc16.h in the avr-libc project for a full explanation of
	 * the algorithm.
	 * 
	 * @param data
	 *            a byte of new data to be added to the crc.
	 */
	public void update(byte data) {
		crc = (crc ^ data) & 0xff; // i loathe java's promotion rules
		for (int i = 0; i < 8; i++) {
			if ((crc & 0x01) != 0) {
				crc = ((crc >>> 1) ^ 0x8c) & 0xff;
			} else {
				crc = (crc >>> 1) & 0xff;
			}
		}
	}

	/**
	 * Get the 8-bit crc value.
	 */
	public byte getCrc() {
		return (byte) crc;
	}

	/**
	 * Reset the crc.
	 */
	public void reset() {
		crc = 0;
	}
}
