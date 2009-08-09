/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 PSerial - class for serial port goodness
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-05 Ben Fry & Casey Reas

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
import gnu.io.UnsupportedCommOperationException;

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

public class Serial {

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
		public boolean isAvailable() {
			return available;
		}
		public int compareTo(Name other) {
			// There should only be one entry per name, so we're going to presume
			// that any two entries with identical names are effectively the same.
			// This also simplifies sorting, etc.
			return name.compareTo(other.name);
		}
	}
	
	private static Set<Serial> portsInUse = new HashSet<Serial>();
	
	/**
	 * Scan the port ids for a list of potential serial ports that we can use.
	 * @return A vector of serial port names.
	 */
	public static Vector<Name> scanSerialNames()
	{
		Vector<Name> v = new Vector<Name>();
		try {
			Enumeration portList = CommPortIdentifier.getPortIdentifiers();
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
		// In-use ports may not end up in the enumeration (thanks, RXTX, you fabulous pile of shit!), so
		// we'll scan for them, insert them if necessary, and return the whole.
		assert portsInUse.size() <= 1;
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
	
	// properties can be passed in for default values
	// otherwise defaults to 9600 N81

	// these could be made static, which might be a solution
	// for the classloading problem.. because if code ran again,
	// the static class would have an object that could be closed

	private SerialPort port;
	private String name;
	private int rate;
	private int parity;
	private int data;
	private int stop;

	public String getName() { return name; }
	
	// read buffer and streams

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
	 * Used by PApplet to shut things down.
	 */
	public void dispose() {
		try {
			// do io streams need to be closed first?
			if (input != null)
				input.close();
			if (output != null)
				output.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		input = null;
		output = null;

		try {
			if (port != null)
				port.close(); // close the port

		} catch (Exception e) {
			e.printStackTrace();
		}
		portsInUse.remove(this);
		port = null;
	}

	/**
	 * Briefly pulse the RTS line low.  On most arduino-based boards, this will hard reset the
	 * device.
	 */
	public void pulseRTSLow() {
		port.setRTS(true);
		port.setRTS(false);
		try {
			Thread.sleep(1);
		} catch (java.lang.InterruptedException ie) {
		}
		port.setRTS(true);
	}

	public int available() {
		try {
			return input.available();
		} catch (IOException ioe) {
			return -1;
		}
	}
	
	public int read() {
		try {
			return input.read();
		} catch (IOException ioe) {
			return -1;
		}
	}
	
	/**
	 * Attempt to fill the given buffer.
	 * @param bytes The buffer to fill with as much data as is available.
	 * @return the number of characters read.
	 */
	public int read(byte bytes[]) {
		try {
			return input.read(bytes);
		} catch (IOException ioe) {
			return -1;
		}
	}
	
	public void write(byte bytes[]) {
		try {
			output.write(bytes);
			output.flush(); // hmm, not sure if a good idea

		} catch (Exception e) { // null pointer or serial port dead
			// errorMessage("write", e);
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

	public void setTimeout(int timeoutMillis) {
		if (!Base.isWindows()) {
			try {
				if (timeoutMillis <= 0) {
					port.disableReceiveTimeout();
				} else {
					port.enableReceiveTimeout(timeoutMillis);
				}
			} catch (UnsupportedCommOperationException unsupEx) {
				System.err.println(unsupEx.getMessage());
			}
		}
	}

	/**
	 * General error reporting, all corraled here just in case I think of
	 * something slightly more intelligent to do.
	 */
	static public void errorMessage(String where, Throwable e) {
		e.printStackTrace();
		throw new RuntimeException("Error inside Serial." + where + "()");
	}
}
