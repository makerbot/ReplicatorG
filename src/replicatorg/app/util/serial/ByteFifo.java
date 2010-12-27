package replicatorg.app.util.serial;

/**
 * Non-growable FIFO.  In theory we only need enough space for a single
 * packet.  Currently set at 1K.
 * @author phooky
 *
 */
public class ByteFifo {
	final static int INITIAL_FIFO_SIZE = 1 * 1024; // 1 K
	private byte[] buffer = new byte[INITIAL_FIFO_SIZE];
	private int head = 0; 
	private int tail = 0;
	/** the point our last new line search ended or the head if it has moved past that point. */
	private int newLineSearchHead = head;

	final private int moduloLength(int value) {
		value = value % buffer.length;
		if (value < 0) value += buffer.length;
		return value;
	}
	public synchronized void enqueue(byte b) {
		buffer[tail++] = b;
		tail = moduloLength(tail);
	}
	public synchronized void clear() { head = tail = 0; }
	public synchronized int size() { return moduloLength(tail-head); }
	public synchronized byte dequeue() {
		int nextHead = moduloLength(head++);
		if (newLineSearchHead == head) newLineSearchHead = nextHead;
		
		byte b = buffer[head];
		head = nextHead;
		return b;
	}
	
	/**
	 * deques the byte array up to and including the first instance of a newline (\n) 
	 * byte. If the \n character is not in the fifo a empty byte array is returned.
	 * @param pattern
	 * @return
	 */
	public synchronized byte[] dequeueLine() {
		int i = newLineSearchHead;
		while(i !=tail)
		{
			if (buffer[i] == (byte)'\n')
			{
				byte[] match = new byte[i+1];
				for ( int j = head; j != moduloLength(i+1); moduloLength(j++) )
				{
					match[j-head] = dequeue();
				}
				return match;
			}

			i = moduloLength(i+1);
		}
		return new byte[0];
	}
}