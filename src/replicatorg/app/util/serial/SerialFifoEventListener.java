package replicatorg.app.util.serial;

public interface SerialFifoEventListener {
	/**
	 * Called by serial when a byte is received and queued in the fifo.
	 * The fifo is synchronzied for the period of this call so it will not be 
	 * modified by any external threads.
	 * @param fifo
	 */
	public void serialByteReceivedEvent(ByteFifo fifo);
}
