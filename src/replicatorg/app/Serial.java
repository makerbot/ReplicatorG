/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Serial - serial port wrapper

 Copyright (c) 2004-05 Ben Fry & Casey Reas
 Copyright (c) 2010 Adam Mayer

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General
 Public License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 Boston, MA  02111-1307  USA
 */

package replicatorg.app;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import replicatorg.app.exceptions.SerialException;
import replicatorg.app.exceptions.UnknownSerialPortException;
import replicatorg.drivers.UsesSerial;

public class Serial implements SerialPortEventListener {
	/**
	 * Serial.Name objects are simple compact objects that hold the name
	 * of a serial port, along with the port's current availability.
	 */
	public static class Name implements Comparable<Name> {
		private String name;
		private boolean available;
		public Name(String name, boolean available) {
			this.name = name;
			this.available = available;
		}
		public String getName() {
			return name;
		}
		/**
		 * @return true if the port can be successfully opened by ReplicatorG.
		 */
		public boolean isAvailable() {
			return available;
		}
		public int compareTo(Name other) {
			// There should only be one entry per name, so we're going to presume
			// that any two entries with identical names are effectively the same.
			// This also simplifies sorting, etc.
			return name.compareTo(other.name);
		}
		public String toString() { return this.name; }
	}
	
	/**
	 * We maintain our own set of ports in current use, because RXTX can't be trusted.
	 * (NB: may be obsoleted at some point on some platforms?)
	 */
	private static Set<Serial> portsInUse = new HashSet<Serial>();
	
	/**
	 * Scan the port ids for a list of potential serial ports that we can use.
	 * @return A vector of serial port names and availability information.
	 */
	public static Vector<Name> scanSerialNames()
	{
		Vector<Name> v = new Vector<Name>();
		try {
			Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
			while (portList.hasMoreElements()) {
				CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
						Name sn = new Name(portId.getName(),!portId.isCurrentlyOwned());
						v.add(sn);
					}
				}
			}
		} catch (Exception e) {
		}
		// In-use ports may not end up in the enumeration (thanks, RXTX), so
		// we'll scan for them, and insert them if necessary.  (The app wants
		// to display in-use ports to reduce user confusion.)
		for (Serial port: portsInUse) {
			Name n = new Name(port.getName(),false);
			boolean contains = false;
			for (Name vi : v) { // vector.contains doesn't use comparable.
				if (vi.compareTo(n) == 0) {
					contains = true; 
					break; 
				}
			}
			if (!contains) { v.add(n); }
		}
		return v;
	}
	
	// Properties can be passed in for default values.
	// Otherwise, we default to 9600 N81

	private SerialPort port;
	private String name;
	private int rate;
	private int parity;
	private int data;
	private int stop;

	public String getName() { return name; }
	
	private InputStream input;
	private OutputStream output;

	public Serial(String name, int rate, char parity, int data, float stop) throws SerialException {
		init(name, rate, parity, data, stop);
	}

	public Serial(String name, UsesSerial us) throws SerialException {
		if (name == null) { name = us.getPortName(); }
		init(name,us.getRate(),us.getParity(),us.getDataBits(),us.getStopBits());
	}
	
	public Serial(String name) throws SerialException {
		init(name,38400,'N',8,1);
	}

	private CommPortIdentifier findPortIdentifier(String name) {
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier id = (CommPortIdentifier)portList.nextElement();
			if (id.getPortType() == CommPortIdentifier.PORT_SERIAL && 
					id.getName().equals(name)) {
				return id;
			}
		}
		return null;
	}

	private void init(String name, int rate, char parity, int data, float stop) throws SerialException {
		// Prepare parameters
		this.name = name;
		this.rate = rate;
		this.parity = SerialPort.PARITY_NONE;
		if (parity == 'E')
			this.parity = SerialPort.PARITY_EVEN;
		if (parity == 'O')
			this.parity = SerialPort.PARITY_ODD;
		this.data = data;
		this.stop = (int)stop;
		if (stop == 1.5f)
			this.stop = SerialPort.STOPBITS_1_5;
		if (stop == 2)
			this.stop = SerialPort.STOPBITS_2;
		// Attempt to find the port identifier for the designated name
		CommPortIdentifier portId = findPortIdentifier(name);
		if (portId == null) {
			throw new UnknownSerialPortException(name);
		}
		// Attempt to open the given port
		try {
			port = (SerialPort)portId.open("replicatorG", 2000);
			port.setSerialPortParams(this.rate, this.data, this.stop, this.parity);

			input = port.getInputStream();
			output = port.getOutputStream();
			port.addEventListener(this);
			port.notifyOnDataAvailable(true);
		} catch (PortInUseException e) {
			throw new SerialException(
					"Serial port '"
					+ name
					+ "' already in use.  Try quiting any programs that may be using it.");
		} catch (Exception e) {
			throw new SerialException("Error opening serial port '" + name
					+ "'.", e);
		}
		portsInUse.add(this);
	}

	/**
	 * Unregister and close the port.
	 */
	public synchronized void dispose() {
		if (port != null) port.removeEventListener();
		if (input != null)
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		input = null;
		if (output != null)
			try {
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		output = null;
		if (port != null) port.close();
		portsInUse.remove(this);
		port = null;
	}

	/**
	 * Briefly pulse the RTS line low.  On most arduino-based boards, this will hard reset the
	 * device.
	 */
	public void pulseRTSLow() {
		port.setDTR(false);
		port.setRTS(false);
		try {
			Thread.sleep(100);
		} catch (java.lang.InterruptedException ie) {
		}
		port.setDTR(true);
		port.setRTS(true);
	}
	
	/**
	 * Non-growable FIFO.  In theory we only need enough space for a single
	 * packet.  Currently set at 1K.
	 * @author phooky
	 *
	 */
	class ByteFifo {
		final static int INITIAL_FIFO_SIZE = 1 * 1024; // 1 K
		private byte[] buffer = new byte[INITIAL_FIFO_SIZE];
		private int head = 0; 
		private int tail = 0;
		final private int moduloLength(int value) {
			value = value % buffer.length;
			if (value < 0) value += buffer.length;
			return value;
		}
		public void enqueue(byte b) {
			buffer[tail++] = b;
			tail = moduloLength(tail);
		}
		public void clear() { head = tail = 0; }
		public int size() { return moduloLength(tail-head); }
		public byte dequeue() {
			byte b = buffer[head++];
			head = moduloLength(head);
			return b;
		}
	}
	
	private ByteFifo readFifo = new ByteFifo();
	
	/**
	 * Attempt to read a single byte.
	 * @return the byte read, or -1 to indicate a timeout.
	 */
	public int read() {
		synchronized(readFifo) {
			try {
				if (readFifo.size() == 0) readFifo.wait(timeoutMillis);
			} catch (InterruptedException e) {
				// We are most likely amidst a shutdown.  Propegate the interrupt
				// status.
				Thread.currentThread().interrupt();
				return -1;
			}
			if (readFifo.size() > 0) {
				byte b = readFifo.dequeue();
				return b & 0xff; 
			} else {
				Base.logger.warning("Read timed out.");
				return -1;
			}
		}
	}

	/**
	 * Attempt to fill the given buffer.  This method blocks until input data is available, 
	 * end of file is detected, or an exception is thrown.  It is meant to emulate the
	 * behavior of the call of the same signature on InputStream, with the significant
	 * difference that it will terminate when the timeout is exceeded.
	 * @param bytes The buffer to fill with as much data as is available.
	 * @return the number of characters read.
	 */
 	public int read(byte bytes[]) {
		synchronized(readFifo) {
			try {
				if (readFifo.size() == 0) readFifo.wait(timeoutMillis);
			} catch (InterruptedException e) {
				// We are most likely amidst a shutdown.  Propegate the interrupt
				// status.
				Thread.currentThread().interrupt();
				return -1;
			}
			int idx = 0;
			while (readFifo.size() > 0 && idx < bytes.length) {
				bytes[idx++] = readFifo.dequeue();
			}
			return idx;
		}
	}

	public void write(byte bytes[]) {
		try {
			output.write(bytes);
			output.flush(); // Reconsider?

		} catch (Exception e) { // null pointer or serial port dead
			e.printStackTrace();
		}
	}

	/**
	 * Write a String to the output. Note that this doesn't account for Unicode
	 * (two bytes per char), nor will it send UTF8 characters.. It assumes that
	 * you mean to send a byte buffer (most often the case for networking and
	 * serial i/o) and will only use the bottom 8 bits of each char in the
	 * string. (Meaning that internally it uses String.getBytes)
	 * 
	 * If you want to move Unicode data, you can first convert the String to a
	 * byte stream in the representation of your choice (i.e. UTF8 or two-byte
	 * Unicode data), and send it as a byte array.
	 */
	public void write(String what) {
		write(what.getBytes());
	}

	/**
	 * The amount of time we're willing to wait for a read to timeout.  Defaults to 1s.
	 */
	private int timeoutMillis = 1000;

	/**
	 * Set the amount of time we're willing to wait for a read to timeout.
	 */
	public void setTimeout(int timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
	}

	/**
	 * General error reporting, all corraled here just in case I think of
	 * something slightly more intelligent to do.
	 */
	static public void errorMessage(String where, Throwable e) {
		e.printStackTrace();
		throw new RuntimeException("Error inside Serial." + where + "()");
	}

	public void clear() {
		synchronized (readFifo) {
			try {
				while (input.available() > 0) {
					input.read();
				}
			} catch (IOException e) {
				// Error condition
				// e.printStackTrace();
				// An unplugged connection will just flood the console with
				// stack traces, and give us zero useful information.  Until
				// we have a plan for how to respond to the user when the
				// connection drops, we'll just let this silently fail, and set
				// a fail bit.
				disconnected = true;
			}
			readFifo.clear();
			readFifo.notifyAll();
		}
	}
	
	private boolean disconnected = false;
	/**
	 * Indicates if we've received 
	 */
	public boolean isDisconnected() { return disconnected; }
	
	public void serialEvent(SerialPortEvent event) {
		synchronized (readFifo) {
			boolean readAny = false;
			try {
				while (input.available() > 0) {
					int b = input.read();
					if (b >= 0) {
						readFifo.enqueue((byte)b);
						readAny = true;
					}
				}
			} catch (IOException e) {
				// Error condition
				// e.printStackTrace();
				// An unplugged connection will just flood the console with
				// stack traces, and give us zero useful information.  Until
				// we have a plan for how to respond to the user when the
				// connection drops, we'll just let this silently fail, and set
				// a fail bit.
				disconnected = true;
			}
			if (readAny) readFifo.notify();
		}
	}
}
