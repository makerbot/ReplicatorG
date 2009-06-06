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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import replicatorg.app.exceptions.SerialException;

public class Serial {

	// properties can be passed in for default values
	// otherwise defaults to 9600 N81

	// these could be made static, which might be a solution
	// for the classloading problem.. because if code ran again,
	// the static class would have an object that could be closed

	SerialPort port;

	int rate;

	int parity;

	int databits;

	int stopbits;

	// read buffer and streams

	public InputStream input;

	public OutputStream output;

	// defaults

	static String dname = "COM1";

	static int drate = 9600;

	static char dparity = 'N';

	static int ddatabits = 8;

	static float dstopbits = 1;

	public Serial(String iname, int irate, char iparity, int idatabits,
			float istopbits) throws SerialException {

		this.rate = irate;

		parity = SerialPort.PARITY_NONE;
		if (iparity == 'E')
			parity = SerialPort.PARITY_EVEN;
		if (iparity == 'O')
			parity = SerialPort.PARITY_ODD;

		this.databits = idatabits;

		stopbits = SerialPort.STOPBITS_1;
		if (istopbits == 1.5f)
			stopbits = SerialPort.STOPBITS_1_5;
		if (istopbits == 2)
			stopbits = SerialPort.STOPBITS_2;

		try {
			Enumeration portList = CommPortIdentifier.getPortIdentifiers();
			while (portList.hasMoreElements()) {
				CommPortIdentifier portId = (CommPortIdentifier) portList
						.nextElement();

				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					if (portId.getName().equals(iname)) {
						port = (SerialPort) portId.open("serial madness", 2000);
						input = port.getInputStream();
						output = port.getOutputStream();
						port.setSerialPortParams(rate, databits, stopbits,
								parity);
					}
				}
			}
		} catch (PortInUseException e) {
			port = null;
			input = null;
			output = null;
			throw new SerialException(
					"Serial port '"
							+ iname
							+ "' already in use.  Try quiting any programs that may be using it.");
		} catch (Exception e) {
			port = null;
			input = null;
			output = null;
			throw new SerialException("Error opening serial port '" + iname
					+ "'.", e);
		}
		if (port == null) {
			throw new SerialException(
					"Serial port '"
							+ iname
							+ "' not found.  Did you select the right one from the Tools > Serial Port menu?");
		}
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
		port = null;
	}

	public void pulseRTSLow() {
		port.setRTS(true);
		port.setRTS(false);
		try {
			Thread.sleep(1);
		} catch (java.lang.InterruptedException ie) {
		}
		port.setRTS(true);
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

	/**
	 * If this just hangs and never completes on Windows, it may be because the
	 * DLL doesn't have its exec bit set. Why the hell that'd be the case, who
	 * knows.
	 */
	static public String[] list() {
		Vector<String> list = new Vector<String>();
		try {
			// System.err.println("trying");
			Enumeration portList = CommPortIdentifier.getPortIdentifiers();
			// System.err.println("got port list");
			while (portList.hasMoreElements()) {
				CommPortIdentifier portId = (CommPortIdentifier) portList
						.nextElement();
				// System.out.println(portId);

				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					String name = portId.getName();
					list.addElement(name);
				}
			}
		} catch (UnsatisfiedLinkError e) {
			// System.err.println("1");
			errorMessage("ports", e);
		} catch (Exception e) {
			// System.err.println("2");
			errorMessage("ports", e);
		}
		// System.err.println("move out");
		String outgoing[] = new String[list.size()];
		list.copyInto(outgoing);
		return outgoing;
	}

	public void setTimeout(int timeoutMillis) {
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

	/**
	 * General error reporting, all corraled here just in case I think of
	 * something slightly more intelligent to do.
	 */
	static public void errorMessage(String where, Throwable e) {
		e.printStackTrace();
		throw new RuntimeException("Error inside Serial." + where + "()");
	}
}
