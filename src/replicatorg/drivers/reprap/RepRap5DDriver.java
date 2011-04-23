/*
 RepRap5DDriver.java

 This is a driver to control a machine that contains a GCode parser and communicates via Serial Port.

 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/* TODOs:
 * (M6 T0 (Wait for tool to heat up)) - not supported? Rewrite to other code?
 * 
 * 
 */
package replicatorg.drivers.reprap;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.app.tools.XML;
import replicatorg.app.util.serial.ByteFifo;
import replicatorg.app.util.serial.SerialFifoEventListener;
import replicatorg.drivers.RealtimeControl;
import replicatorg.drivers.RetryException;
import replicatorg.drivers.SerialDriver;
import replicatorg.drivers.reprap.ExtrusionUpdater.Direction;
import replicatorg.machine.model.AxisId;
import replicatorg.machine.model.ToolModel;
import replicatorg.util.Point5d;

public class RepRap5DDriver extends SerialDriver implements SerialFifoEventListener, RealtimeControl 
{
	private static Pattern gcodeCommentPattern = Pattern.compile("\\([^)]*\\)|;.*");
	private static Pattern resendLinePattern = Pattern.compile("([0-9]+)");
	private static Pattern gcodeLineNumberPattern = Pattern.compile("n\\s*([0-9]+)");
	
	public final AtomicReference<Double> feedrate = new AtomicReference<Double>(0.0);
	public final AtomicReference<Double> ePosition = new AtomicReference<Double>(0.0);
	
	private final ReentrantLock sendCommandLock = new ReentrantLock();
	
	/** true if a line containing the start keyword has been received from the firmware*/
	private final AtomicBoolean startReceived = new AtomicBoolean(false);

	/** true if a line containing the ok keyword has been received from the firmware*/
	private final AtomicBoolean okReceived = new AtomicBoolean(false);
	

	/**
	 * An above zero level shows more info
	 */
	private int debugLevel = 0;
	
	/**
	 * Temporary. This allows for purposefully injection of noise to speed up debugging of retransmits and recovery.
	 */
	private int introduceNoiseEveryN = -1;
	private int lineIterator = 0;
	private int numResends = 0;
	
	/**
	 * Enables five D GCodes if true. If false reverts to traditional 3D Gcodes
	 */
	private boolean fiveD = true;
	
	/**
	 * Adds check-sums on to each gcode line sent to the RepRap.
	 */
	private boolean hasChecksums = true;
	
	/**
	 * Enables pulsing RTS to restart the RepRap on connect.
	 */
	private boolean pulseRTS = true;

	/**
	 * if true the firmware sends "resend: 1234" and then "ok" for
	 * the same line and we need to eat that extra "ok".  Teacup does
	 * not do this but Tonokip, Klimentkip and FiveD do
	 */
	private boolean okAfterResend = true;

	/**
	 * if true the firmware sends "start" followed by "ok" on reset
	 * and we need to eat that extra "ok".  Teacup is the only known
	 * firmware with this behavior.
	 */
	private boolean okAfterStart = false;

	/**
	 * if true all E moves are relative, so there's no need to snoop
	 * E moves and ePosition stays at 0.0 all the time.  Teacup has
	 * always relative E.
	 */
	private boolean alwaysRelativeE = false;
	
	/**
	 * if true the driver will wait for a "start" signal when pulsing rts low 
	 * before initialising the printer.
	 */
	private boolean waitForStart = false;
	
	/**
	 * configures the time to wait for the first response from the RepRap in ms.
	 */
	private long waitForStartTimeout = 1000;
	
	private int waitForStartRetries = 3;

	/**
	 * This allows for real time adjustment of certain printing variables!
	 */
	private boolean realtimeControl = false;
	private double rcFeedrateMultiply = 1;
	private double rcTravelFeedrateMultiply = 1;
	private double rcExtrusionMultiply = 1;
	private double rcFeedrateLimit = 60*300; // 300mm/s still works on Ultimakers!
	
	private final ExtrusionUpdater extrusionUpdater = new ExtrusionUpdater(this);

	/**
	 * the size of the buffer on the GCode host
	 */
	private int maxBufferSize = 128;

	/**
	 * The commands sent but not yet acknowledged by the firmware. Stored so they can be resent 
	 * if there is a checksum problem.
	 */
	private LinkedList<String> buffer = new LinkedList<String>();
	private ReentrantLock bufferLock = new ReentrantLock();
	
	/** locks the readResponse method to prevent multiple concurrent reads */
	private ReentrantLock readResponseLock = new ReentrantLock();

	protected DecimalFormat df;

	private AtomicInteger lineNumber = new AtomicInteger(-1);

	public RepRap5DDriver() {
		super();
		
		// init our variables.
		setInitialized(false);

		df = new DecimalFormat("#.######");
	}

        public String getDriverName() {
                return "RepRap5D";
        }

	public synchronized void loadXML(Node xml) {
		super.loadXML(xml);
        // load from our XML config, if we have it.
        if (XML.hasChildNode(xml, "waitforstart")) {
        	Node startNode = XML.getChildNodeByName(xml, "waitforstart");

            String enabled = XML.getAttributeValue(startNode, "enabled");
            if (enabled !=null) waitForStart = Boolean.parseBoolean(enabled);
            
            String timeout = XML.getAttributeValue(startNode, "timeout");
            if (timeout !=null) waitForStartTimeout = Long.parseLong(timeout);
            
            String retries = XML.getAttributeValue(startNode, "retries");
            if (retries !=null) waitForStartRetries = Integer.parseInt(retries);
            
        }

        if (XML.hasChildNode(xml, "pulserts")) {
            pulseRTS = Boolean.parseBoolean(XML.getChildNodeValue(xml, "pulserts"));
        }

        if (XML.hasChildNode(xml, "checksums")) {
            hasChecksums = Boolean.parseBoolean(XML.getChildNodeValue(xml, "checksums"));
        }

        if (XML.hasChildNode(xml, "fived")) {
            fiveD = Boolean.parseBoolean(XML.getChildNodeValue(xml, "fived"));
        }
        if (XML.hasChildNode(xml, "debugLevel")) {
        	debugLevel = Integer.parseInt(XML.getChildNodeValue(xml, "debugLevel"));
        }
        if (XML.hasChildNode(xml, "limitFeedrate")) {
        	rcFeedrateLimit = Double.parseDouble(XML.getChildNodeValue(xml, "limitFeedrate"));
        }
	if (XML.hasChildNode(xml, "okAfterResend")) {
		okAfterResend = Boolean.parseBoolean(XML.getChildNodeValue(xml, "okAfterResend"));
	}
	if (XML.hasChildNode(xml, "okAfterStart")) {
		okAfterStart = Boolean.parseBoolean(XML.getChildNodeValue(xml, "okAfterStart"));
	}
	if (XML.hasChildNode(xml, "alwaysRelativeE")) {
		alwaysRelativeE = Boolean.parseBoolean(XML.getChildNodeValue(xml, "alwaysRelativeE"));
	}
        
        if (XML.hasChildNode(xml, "introduceNoise")) {
        	double introduceNoise = Double.parseDouble(XML.getChildNodeValue(xml, "introduceNoise"));
        	if(introduceNoise != 0) {
            	Base.logger.warning("Purposefully injecting noise into communications. This is NOT for production.");
            	Base.logger.warning("Turn this off by removing introduceNoise from the machines XML file of your machine.");
            	introduceNoiseEveryN = (int) (1/introduceNoise);
        	}
        }
    }

	public void updateManualControl() throws InterruptedException
	{
		extrusionUpdater.update();
	}

	/**
	 * Send any gcode needed to synchronize the driver state and
	 * the firmware state.  Sent on startup and if we see "start"
	 * indicating an uncommanded firmware reset.
	 */
	public void sendInitializationGcode(boolean synchronous) {
		sendCommand("M110", synchronous);  // may be duplicate, that's ok
		// default us to absolute positioning
		sendCommand("G90", synchronous);
		sendCommand("G92 X0 Y0 Z0 E0", synchronous);
	}

	public synchronized void initialize() {
		// declare our serial guy.
		if (serial == null) {
			Base.logger.severe("No Serial Port found.\n");
			return;
		}
		// wait till we're initialised
		if (!isInitialized()) {
			Base.logger.info("Initializing Serial.");

			flushBuffer();

			if (pulseRTS)
			{
				Base.logger.fine("Resetting RepRap: Pulsing RTS..");
				int retriesRemaining = waitForStartRetries+1;
				retryPulse: do
				{
					try {
						pulseRTS();
						Base.logger.finer("start received");
						break retryPulse;
					} catch (TimeoutException e) {
						retriesRemaining--;
					}
					if (retriesRemaining == 0)
					{
						this.disconnect();
						Base.logger.warning("RepRap not responding to RTS reset. Failed to connect.");
						return;
					}
					else
					{
						Base.logger.warning("RepRap not responding to RTS reset. Trying again..");
					}
				} while(true);
				Base.logger.fine("RepRap Reset. RTS pulsing complete.");
			}



			// Send a line # reset command, this allows us to catch the "ok" response.
			// If we get a "start" while trying this we'll go again.
			synchronized(okReceived)
			{
				okReceived.set(false);
				while (!okReceived.get())
				{
					sendCommand("M110", false);
					Base.logger.info("GCode sent. waiting for response..");
					
					try
					{
						waitForNotification(okReceived, 10000);
					}
					catch (RetryException e)
					{ }
					catch (TimeoutException e)
					{
						this.disconnect();
						Base.logger.warning("Firmware not responding to gcode. Failed to connect.");
						return;
					}
				}
			}

			Base.logger.fine("GCode response received. RepRap connected.");

			sendInitializationGcode(true);
			Base.logger.info("Ready.");
			this.setInitialized(true);
		}
	}
	
	private void waitForNotification(AtomicBoolean notifier, long timeout) throws TimeoutException, RetryException
	{
		//Wait for the RepRap to respond
		try {
			notifier.wait(timeout);
		} catch (InterruptedException e) {
			//Presumably we're shutting down
			Thread.currentThread().interrupt();
			return;
		}
		
		if (notifier.get() == true)
		{
			return;
		}
		else
		{
			throw new RetryException();
		}
	}
	
	private void pulseRTS() throws TimeoutException
	{
		// attempt to reset the device, this may slow down the connection time, but it 
		// increases our chances of successfully connecting dramatically.

		Base.logger.info("Attempting to reset RepRap (pulsing RTS)");
		
		synchronized (startReceived) {
			startReceived.set(false);
			serial.pulseRTSLow();
			
			if (waitForStart == false) return;
	
			// Wait for the RepRap to respond to the RTS pulse attempt 
			// or for this to timeout.
			while (!startReceived.get()) {
				try {
					waitForNotification(startReceived, waitForStartTimeout);
				} catch (RetryException e) {
					continue;
				}
			}
		}
	}

	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute() {
		//If we're not initialized (ie. disconnected) do not execute commands on the disconnected machine.
		if(!isInitialized()) return;

		// we *DONT* want to use the parents one,
		// as that will call all sorts of misc functions.
		// we'll simply pass it along.
		// super.execute();
		sendCommand(getParser().getCommand());
	}
	
	/**
	 * Returns null or the first instance of the matching group
	 */
	private String getRegexMatch(String regex, String input, int group)
	{
		return getRegexMatch(Pattern.compile(regex), input, group);
	}

	private String getRegexMatch(Pattern regex, String input, int group)
	{
		Matcher matcher = regex.matcher(input);
		if (!matcher.find() || matcher.groupCount() < group) return null;
		return matcher.group(group);
	}

	/**
	 * Actually sends command over serial.
	 * 
	 * Commands sent here are acknowledged asynchronously in another 
	 * thread so as to increase our serial GCode throughput.
	 * 
	 * Only one command can be sent at a time. If another command is 
	 * being sent this method will block until the previous command 
	 * is finished sending.
	 */
	protected void sendCommand(String next) {
		_sendCommand(next, true, false);
	}

	protected void sendCommand(String next, boolean synchronous) {
		_sendCommand(next, synchronous, false);
	}

	
	protected void resendCommand(String command) {
		synchronized (sendCommandLock)
		{
			numResends++;
			if(debugLevel > 0)
				Base.logger.warning("Resending: \"" + command + "\". Resends in "+ numResends + " of "+lineIterator+" lines.");
			_sendCommand(command, false, true);
		}
	}

	/**
	 * inner method. not for use outside sendCommand and resendCommand
	 */
	protected void _sendCommand(String next, boolean synchronous, boolean resending) {
				
		// If this line is uncommented, it simply sends the next line instead of doing a retransmit!
		if (!resending) 
		{
			sendCommandLock.lock();

			//assert (isInitialized());
			// System.out.println("sending: " + next);
	
			next = clean(next);
			next = fix(next); // make it compatible with older versions of the GCode interpeter
			
			// skip empty commands.
			if (next.length() == 0)
			{
				sendCommandLock.unlock();
				return;
			}
	
			//update the current feedrate
			String feedrate = getRegexMatch("F(-[0-9\\.]+)", next, 1);
			if (feedrate!=null) this.feedrate.set(Double.parseDouble(feedrate));

			if (!alwaysRelativeE) {
				//update the current extruder position
				String e = getRegexMatch("E([-0-9\\.]+)", next, 1);
				if (e!=null) this.ePosition.set(Double.parseDouble(e));
			} else {
				ePosition.set(0.0);
			}

			// applychecksum replaces the line that was to be retransmitted, into the next line.
			if (hasChecksums) next = applyNandChecksum(next);
			
			Base.logger.finest("sending: "+next);
		}
		else
		{
			Base.logger.finest("resending: "+next);
		}
		// Block until we can fit the command on the Arduino
/*		synchronized(bufferLock)
		{
			//wait for the number of commands queued in the buffer to shrink before 
			//adding the next command to it.
                        // NOTE: must compute bufferSize from buffer elements now
			while(bufferSize + next.length() + 1 > maxBufferSize)
			{
				Base.logger.warning("reprap driver buffer full. gcode write slowed.");
				try {
					bufferLock.wait();
				}
				catch (InterruptedException e1) {
					//Presumably we're shutting down
					Thread.currentThread().interrupt();
					return;
				}
			}
		}*/
		

		// debug... let us know whats up!
		if(debugLevel > 1)
			Base.logger.info("Sending: " + next);


		try {
			synchronized(next)
			{
				// do the actual send.
				serialInUse.lock();
				bufferLock.lock();

				// record it in our buffer tracker.
				buffer.addFirst(next);
				
				if((introduceNoiseEveryN != -1) && (lineIterator++) >= introduceNoiseEveryN) {
					Base.logger.info("Introducing noise (lineIterator=="
							+ lineIterator + ",introduceNoiseEveryN=" + introduceNoiseEveryN + ")");
					lineIterator = 0;
					String noisyNext = next.replace('6','7').replace('7','1') + "\n";
					serial.write(noisyNext);
				} else {
					serial.write(next + "\n");
				}
				bufferLock.unlock();
				serialInUse.unlock();

				// Synchronous gcode transfer. Waits for the 'ok' ack to be received.
				if (synchronous) next.wait();
			}
		} catch (InterruptedException e1) {
			//Presumably we're shutting down
			Thread.currentThread().interrupt();
			if (!resending) 
				sendCommandLock.unlock();
			return;
		}

		// Wait for the response (synchronous gcode transmission)
		//while(!isFinished()) {}
		
		if (!resending) 
			sendCommandLock.unlock();
	}

	public String clean(String str) {
		String clean = str;

		// trim whitespace
		clean = clean.trim();

		// remove spaces
		//clean = clean.replaceAll(" ", "");
		
		// remove all comments
        clean = RepRap5DDriver.gcodeCommentPattern.matcher(clean).replaceAll("");

		return clean;
	}
	public String fix(String str) {
		String fixed = str;
		// The 5D firmware expects E codes for extrusion control instead of M101, M102, M103

		Pattern r = Pattern.compile("M01[^0-9]");
		Matcher m = r.matcher(fixed);
		if (m.find())
		{
			return "";
		}

	    // Remove M10[123] codes
	    // This piece of code causes problems?!? Restarts?
		r = Pattern.compile("M10[123](.*)");
		m = r.matcher(fixed);
	    if (m.find( )) {
//	    	System.out.println("Didn't find pattern in: " + str );
//	    	fixed = m.group(1)+m.group(3)+";";
	    	return "";
	    }
		
		// Reorder E and F codes? F-codes need to go LAST!
		//requires: import java.util.regex.Matcher; and import java.util.regex.Pattern;
		r = Pattern.compile("^(.*)(F[0-9\\.]*)\\s?E([0-9\\.]*)$");
	    m = r.matcher(fixed);
	    if (m.find( )) {
			fixed = m.group(1)+" E"+m.group(3)+" "+m.group(2);
	    }

	    if(realtimeControl) {
		    // Rescale F value
		    r = Pattern.compile("(.*)F([0-9\\.]*)(.*)");
		    m = r.matcher(fixed);
		    if (m.find( )) {
		    	double newvalue = Double.valueOf(m.group(2).trim()).doubleValue();
		    	// FIXME: kind of an ugly way to test for extrusionless "travel" versus extrusion.
		    	if (!fixed.contains("E"))
		    	{
		    		newvalue *= rcTravelFeedrateMultiply;
		    	} else {
		    		newvalue *= rcFeedrateMultiply;
		    	}
		    	if(newvalue > rcFeedrateLimit)
		    		newvalue = rcFeedrateLimit;
		    	
		    	NumberFormat formatter = new DecimalFormat("#0.0");
		    	fixed = m.group(1)+" F"+formatter.format(newvalue)+" "+m.group(3);
		    }
		    
	/*	    // Rescale E value
			r = Pattern.compile("(.*)E([0-9\\.]*)(.*)");//E317.52// Z-moves slower! Extrude 10% less? More and more rapid reversing
		    m = r.matcher(fixed);
		    if (m.find( )) {
		    	double newEvalue = Double.valueOf(m.group(2).trim()).doubleValue();
		    	newEvalue = newEvalue * 0.040;
		    	NumberFormat formatter = new DecimalFormat("#0.0");
		    	fixed = m.group(1)+" E"+formatter.format(newEvalue)+" "+m.group(3);
		    }
	*/
	    }
	    
	    return fixed; // no change!
	}

	/**
	 * takes a line of gcode and returns that gcode with a line number and checksum
	 */
	public String applyNandChecksum(String gcode) {
		// RepRap Syntax: N<linenumber> <cmd> *<chksum>\n

		if (gcode.contains("M110"))
			lineNumber.set(-1);
		
		Matcher lineNumberMatcher = gcodeLineNumberPattern.matcher(gcode);
		if (lineNumberMatcher.matches())
		{ // reset our line number to the specified one. this is usually a m110 line # reset
			lineNumber.set( Integer.parseInt( lineNumberMatcher.group(1) ) );
		}
		else
		{ // only add a line number if it is not already specified
			gcode = "N"+lineNumber.incrementAndGet()+' '+gcode+' ';
		}
		return applyChecksum(gcode);
	}

	public String applyChecksum(String gcode) {
		// chksum = 0 xor each byte of the gcode (including the line number and trailing space)
		byte checksum = 0;
		byte[] gcodeBytes = gcode.getBytes();
		for (int i = 0; i<gcodeBytes.length; i++)
		{
			checksum = (byte) (checksum ^ gcodeBytes[i]);
		}
		return gcode+'*'+checksum;
	}
	
	public void serialByteReceivedEvent(ByteFifo fifo) {
		readResponseLock.lock();

		serialInUse.lock();
		byte[] response = fifo.dequeueLine();
		int responseLength = response.length;
		serialInUse.unlock();

		// 0 is now an acceptable value; it merely means that we timed out
		// waiting for input
		if (responseLength < 0) {
			// This signifies EOF. FIXME: How do we handle this?
			Base.logger.severe("SerialPassthroughDriver.readResponse(): EOF occured");
			readResponseLock.unlock();
			return;
		} else if(responseLength!=0) {
			String line;
			try
			{
				//convert to string and remove any trailing \r or \n's
				line = new String(response, 0, responseLength, "US-ASCII")
							.trim().toLowerCase();
			} catch (UnsupportedEncodingException e) {
				Base.logger.severe("US-ASCII required. Terminating.");
				readResponseLock.unlock();
				throw new RuntimeException(e);
			}

			//System.out.println("received: " + line);
			if(debugLevel > 1)
				Base.logger.info("<< " + line);

			if (line.length() == 0)
				Base.logger.fine("empty line received");
			else if (line.startsWith("echo:")) {
					//if echo is turned on relay it to the user for debugging
					Base.logger.info(line);
			}
			else if (line.startsWith("ok t:")||line.startsWith("t:")) {
				Pattern r = Pattern.compile("t:([0-9\\.]+)");
			    Matcher m = r.matcher(line);
			    if (m.find( )) {
			    	String temp = m.group(1);
					
					machine.currentTool().setCurrentTemperature(
							Double.parseDouble(temp));
			    }
				r = Pattern.compile("^ok.*b:([0-9\\.]+)$");
			    m = r.matcher(line);
			    if (m.find( )) {
			    	String bedTemp = m.group(1);
					machine.currentTool().setPlatformCurrentTemperature(
							Double.parseDouble(bedTemp));
			    }
			}
			else if (line.startsWith("ok c:")||line.startsWith("c:")) {
				Pattern r = Pattern.compile("c: *x:?([-0-9\\.]+) *y:?([-0-9\\.]+) *z:?([-0-9\\.]+)");
				Matcher m = r.matcher(line);
				if (m.find()) {
					double x = Double.parseDouble(m.group(1));
					double y = Double.parseDouble(m.group(2));
					double z = Double.parseDouble(m.group(3));
					// super to avoid parroting back a G92
					try {
						super.setCurrentPosition(new Point5d(x, y, z));
						//Base.logger.fine("setting currentposition to:"+x+","+y+","+z+".");
					} catch (RetryException e) {
						// do or do not, there is no retry
					}
				}
			}
			if (line.startsWith("ok")) {
				
				synchronized(okReceived)
				{
					okReceived.set(true);
					okReceived.notifyAll();
				}

				bufferLock.lock();
				//Notify the thread waitining in this gcode's sendCommand method that the gcode has been received.
				if (buffer.isEmpty()) {
					Base.logger.severe("Received OK with nothing queued!");
				} else {
					String notifier = buffer.removeLast();
					if(debugLevel > 1)
						Base.logger.info("FW Accepted: " + notifier);
					synchronized(notifier) { notifier.notifyAll(); }
				}
				bufferLock.unlock();
				
				synchronized(bufferLock)
				{ /*let any sendCommand method waiting to send know that the buffer is 
					now smaller and may be able to fit their command.*/
					bufferLock.notifyAll();
				}
			}

			// old arduino firmware sends "start"
			else if (line.contains("start")) {
				// Reset line number first in case gcode is sent below
				lineNumber.set(-1);

				boolean active = !buffer.isEmpty();
				flushBuffer();

				if (isInitialized()) {
					sendInitializationGcode(false);

					// If there were outstanding commands try to abort any print in progress.
					// This is a poor test:  but do we know if we're printing at this level?
					// tried setInitialized(false); but that didn't work well
					if (active) {
						Base.logger.severe("Firmware reset with active commands!");
						setError("Firmware reset with active commands!");
					}
				}
				if (okAfterStart) {
					// firmware sends "ok" after start, put something here to consume it:
					bufferLock.lock();
					buffer.addLast(";start-ok");
					bufferLock.unlock();
				}

				// todo: set version
				synchronized (startReceived) {
					startReceived.set(true);
					startReceived.notifyAll();
				}

				// Wake up connect task to try again
				synchronized (okReceived) {
					okReceived.set(false);
					okReceived.notifyAll();
				}

			} else if (line.startsWith("extruder fail")) {
				setError("Extruder failed:  cannot extrude as this rate.");

			} else if (line.startsWith("resend:")||line.startsWith("rs ")) {
				// Bad checksum, resend requested
				Matcher badLineMatch = resendLinePattern.matcher(line);

				// Is it a Dud M or G code?
				String dudLetter = getRegexMatch("dud ([a-z]) code", line, 1);

				if (badLineMatch.find())
				{
					int badLineNumber = Integer.parseInt(
							badLineMatch.group(1) );
					if(debugLevel > 1)
						Base.logger.warning("Received resend request for line " + badLineNumber);

					Queue<String> resend = new LinkedList<String>();
					boolean found = false;
					// Search backwards for the bad line in our buffer.
					// Firmware flushed everything after this line, so
					// build a queue of lines to resend.
					bufferLock.lock();
					lineSearch: while (!buffer.isEmpty())
					{
						String bufferedLine = buffer.removeLast();
						if(debugLevel > 1)
							Base.logger.info("Searching: " + bufferedLine);
						int bufferedLineNumber = Integer.parseInt( getRegexMatch(
								gcodeLineNumberPattern, bufferedLine.toLowerCase(), 1) );
						if (dudLetter != null && bufferedLineNumber == badLineNumber) {
							Base.logger.info("Dud "+dudLetter+" code: Dropping " + bufferedLine);
							synchronized (bufferedLine) {
								bufferedLine.notifyAll();
							}
							found = true;
							break lineSearch;
						}
						resend.add(bufferedLine);
						if (bufferedLineNumber == badLineNumber) {
							found = true;
							break lineSearch;
						}
					}
					if (okAfterResend) {
						// firmware sends "ok" after resend, put something here to consume it:
						buffer.addLast(";resend-ok");
					}
					bufferLock.unlock();

					if (!found) {
						int restartLineNumber = Integer.parseInt( getRegexMatch(
								gcodeLineNumberPattern, resend.element().toLowerCase(), 1) );
						Base.logger.severe("resend for line " + badLineNumber + " not in our buffer.  Resuming from " + restartLineNumber);
						this.resendCommand(applyChecksum("N"+(restartLineNumber-1)+" M110"));
					}
					// resend the lines
					while (!resend.isEmpty())
					{
						String bufferedLine = resend.remove();
						this.resendCommand(bufferedLine);
					}
				}
				else
				{
					// Malformed resend line request received. Resetting the line number
					Base.logger.warning("malformed line resend request, "
							+"resetting line number. Malformed Data: \n"+line);
					this.resendCommand(applyChecksum("N"+(lineNumber.get()-1)+" M110"));
				}

			} else if (line.startsWith("t:") || line.startsWith("c:")) {
				// temperature, position handled above
			} else {
				Base.logger.severe("Unknown: " + line);
			}
		}

		readResponseLock.unlock();
	}

	public boolean isFinished() {
		return isBufferEmpty();
	}

	/**
	 * Clear the command buffer and send notifications to everyone
	 * waiting for their completion.
	 */
	private void flushBuffer() {
		bufferLock.lock();
		while (!buffer.isEmpty())
		{
			String notifier = buffer.removeLast();
			if(debugLevel > 1)
				Base.logger.fine("Flushing dead command: " + notifier);
			synchronized(notifier) { notifier.notifyAll(); }
		}
		bufferLock.unlock();
	}

	/**
	 * Is our buffer empty? If don't have a buffer, its always true.
	 */
	public boolean isBufferEmpty() {
		bufferLock.lock();
		boolean isEmpty = buffer.isEmpty();
		bufferLock.unlock();
		return isEmpty;
	}

	/**
	 * What is our queue size?  Used by extrusion driver
	 */
	public int queueSize() {
		bufferLock.lock();
		int queueSize = buffer.size();
		bufferLock.unlock();
		return queueSize;
	}

	private synchronized void disconnect() {
		bufferLock.lock();
		flushBuffer();
		setSerial(null);
		bufferLock.unlock();
	}

	public synchronized void dispose() {
		bufferLock.lock();
		flushBuffer();
		super.dispose();
		bufferLock.unlock();
	}

	/***************************************************************************
	 * commands for interfacing with the driver directly
	 * @throws RetryException 
	 **************************************************************************/

	public void queuePoint(Point5d p) throws RetryException {
		String cmd = "G1 F" + df.format(getCurrentFeedrate());
		
		sendCommand(cmd);

		cmd = "G1 X" + df.format(p.x()) + " Y" + df.format(p.y()) + " Z"
				+ df.format(p.z()) + " F" + df.format(getCurrentFeedrate());

		sendCommand(cmd);

		super.queuePoint(p);
	}

	public void setCurrentPosition(Point5d p) throws RetryException {
		sendCommand("G92 X" + df.format(p.x()) + " Y" + df.format(p.y()) + " Z"
				+ df.format(p.z()));

		super.setCurrentPosition(p);
	}

	@Override
	public void homeAxes(EnumSet<AxisId> axes, boolean positive, double feedrate) throws RetryException {
		Base.logger.info("homing "+axes.toString());
		StringBuffer buf = new StringBuffer("G28");
		for (AxisId axis : axes)
		{
			buf.append(" "+axis+"0");
		}
		sendCommand(buf.toString());

		invalidatePosition();
		super.homeAxes(axes,false,0);
	}

	public void delay(long millis) {
		int seconds = Math.round(millis / 1000);

		sendCommand("G4 P" + seconds);

		// no super call requried.
	}

	public void openClamp(int clampIndex) {
		sendCommand("M11 Q" + clampIndex);

		super.openClamp(clampIndex);
	}

	public void closeClamp(int clampIndex) {
		sendCommand("M10 Q" + clampIndex);

		super.closeClamp(clampIndex);
	}

	public void enableDrives() throws RetryException {
		sendCommand("M17");

		super.enableDrives();
	}

	public void disableDrives() throws RetryException {
		sendCommand("M18");
		sendCommand("M84");     // Klimentkip

		super.disableDrives();
	}

	public void changeGearRatio(int ratioIndex) {
		// gear ratio codes are M40-M46
		int code = 40 + ratioIndex;
		code = Math.max(40, code);
		code = Math.min(46, code);

		sendCommand("M" + code);

		super.changeGearRatio(ratioIndex);
	}

	protected String _getToolCode() {
		return "T" + machine.currentTool().getIndex() + " ";
	}

	/***************************************************************************
	 * Motor interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setMotorRPM(double rpm) throws RetryException {
		if (fiveD == false)
		{
			sendCommand(_getToolCode() + "M108 R" + df.format(rpm));
		}
		else
		{
			extrusionUpdater.setFeedrate(rpm);
		}
		
		super.setMotorRPM(rpm);
	}

	public void setMotorSpeedPWM(int pwm) throws RetryException {
		if (fiveD == false)
		{
			sendCommand(_getToolCode() + "M108 S" + df.format(pwm));
		}

		super.setMotorSpeedPWM(pwm);
	}
	
	public synchronized void enableMotor() throws RetryException {
		String command = _getToolCode();

		if (fiveD == false)
		{
			if (machine.currentTool().getMotorDirection() == ToolModel.MOTOR_CLOCKWISE)
				command += "M101";
			else
				command += "M102";
	
			sendCommand(command);
		}
		else
		{
			extrusionUpdater.setDirection( machine.currentTool().getMotorDirection()==1?
					Direction.forward : Direction.reverse );
			extrusionUpdater.startExtruding();
		}

		super.enableMotor();
	}

	/**
	 * Unified enable+delay+disable to allow us to use G1 E
	 */
	public synchronized void enableMotor(long millis) throws RetryException {
		if (fiveD == false)
		{
			super.enableMotor(millis);
		}
		else
		{
			super.enableMotor();
			double feedrate = machine.currentTool().getMotorSpeedRPM();
			double distance = millis * feedrate / 60 / 1000;
			if (machine.currentTool().getMotorDirection() != 1) {
				distance *= -1;
			}
			sendCommand(_getToolCode() + "G1 E" + (ePosition.get() + distance) + " F" + feedrate);
			super.disableMotor();
		}
	}

	public void disableMotor() throws RetryException {
		if (fiveD == false)
		{
			sendCommand(_getToolCode() + "M103");
		}
		else
		{
			extrusionUpdater.stopExtruding();
		}

		super.disableMotor();
	}

	/***************************************************************************
	 * Spindle interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setSpindleRPM(double rpm) throws RetryException {
		sendCommand(_getToolCode() + "S" + df.format(rpm));

		super.setSpindleRPM(rpm);
	}

	public void enableSpindle() throws RetryException {
		String command = _getToolCode();

		if (machine.currentTool().getSpindleDirection() == ToolModel.MOTOR_CLOCKWISE)
			command += "M3";
		else
			command += "M4";

		sendCommand(command);

		super.enableSpindle();
	}

	public void disableSpindle() throws RetryException {
		sendCommand(_getToolCode() + "M5");

		super.disableSpindle();
	}

	/***************************************************************************
	 * Temperature interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void setTemperature(double temperature) throws RetryException {
		sendCommand(_getToolCode() + "M104 S" + df.format(temperature));

		super.setTemperature(temperature);
	}

	public void readTemperature() {
		sendCommand(_getToolCode() + "M105");

		super.readTemperature();
	}

	public double getPlatformTemperature(){
		return machine.currentTool().getPlatformCurrentTemperature();
	}

	public void setPlatformTemperature(double temperature) throws RetryException {
		sendCommand(_getToolCode() + "M140 S" + df.format(temperature));
		
		super.setPlatformTemperature(temperature);
	}
	/***************************************************************************
	 * Flood Coolant interface functions
	 **************************************************************************/
	public void enableFloodCoolant() {
		sendCommand(_getToolCode() + "M7");

		super.enableFloodCoolant();
	}

	public void disableFloodCoolant() {
		sendCommand(_getToolCode() + "M9");

		super.disableFloodCoolant();
	}

	/***************************************************************************
	 * Mist Coolant interface functions
	 **************************************************************************/
	public void enableMistCoolant() {
		sendCommand(_getToolCode() + "M8");

		super.enableMistCoolant();
	}

	public void disableMistCoolant() {
		sendCommand(_getToolCode() + "M9");

		super.disableMistCoolant();
	}

	/***************************************************************************
	 * Fan interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void enableFan() throws RetryException {
		sendCommand(_getToolCode() + "M106");

		super.enableFan();
	}

	public void disableFan() throws RetryException {
		sendCommand(_getToolCode() + "M107");

		super.disableFan();
	}

	/***************************************************************************
	 * Valve interface functions
	 * @throws RetryException 
	 **************************************************************************/
	public void openValve() throws RetryException {
		sendCommand(_getToolCode() + "M126");

		super.openValve();
	}

	public void closeValve() throws RetryException {
		sendCommand(_getToolCode() + "M127");

		super.closeValve();
	}

	/***************************************************************************
	 * Collet interface functions
	 **************************************************************************/
	public void openCollet() {
		sendCommand(_getToolCode() + "M21");

		super.openCollet();
	}

	public void closeCollet() {
		sendCommand(_getToolCode() + "M22");

		super.closeCollet();
	}

	public void reset() {
		Base.logger.info("Reset.");
		setInitialized(false);
		initialize();
	}

	public void stop(boolean abort) {
		// No implementation needed for synchronous machines.
		//sendCommand("M0");
		// M0 is the same as emergency stop: will hang all communications. You don't really want that...
		invalidatePosition();
		Base.logger.info("RepRap/Ultimaker Machine stop called.");
	}

	protected Point5d reconcilePosition() {
		sendCommand("M114");
		// If the firmware returned a position then the reply parser
		// already set the current position and our caller is actually
		// going to end up ignoring what we return.
		//
		//return new Point5d();
		return super.getCurrentPosition(false);
	}

	/* ===============
	 * This driver has real time control over feedrate and extrusion parameters, allowing real-time tuning!
	 */
	public boolean hasFeatureRealtimeControl() {
		Base.logger.info("Yes, I have a Realtime Control feature." );
		return true;
	}

	public void enableRealtimeControl(boolean enable) {
		realtimeControl = enable;
		Base.logger.info("Realtime Control (RC) is: "+ realtimeControl );
	}
	
	public double getExtrusionMultiplier() {
		return rcExtrusionMultiply;
	}

	public double getFeedrateMultiplier() {
		return rcFeedrateMultiply;
	}
	
	public double getTravelFeedrateMultiplier() {
		return rcTravelFeedrateMultiply;
	}

	public boolean setExtrusionMultiplier(double multiplier) {
		rcExtrusionMultiply = multiplier;
		if(debugLevel == 2)
			Base.logger.info("RC muplipliers: extrusion="+rcExtrusionMultiply+"x, feedrate="+rcFeedrateMultiply+"x" );
		return true;
	}

	public boolean setFeedrateMultiplier(double multiplier) {
		rcFeedrateMultiply = multiplier;
		if(debugLevel == 2)
			Base.logger.info("RC muplipliers: extrusion="+rcExtrusionMultiply+"x, feedrate="+rcFeedrateMultiply+"x" );
		return true;
	}
	public boolean setTravelFeedrateMultiplier(double multiplier) {
		rcTravelFeedrateMultiply = multiplier;
		if(debugLevel == 2)
			Base.logger.info("RC muplipliers: extrusion="+rcExtrusionMultiply+"x, feedrate="+rcFeedrateMultiply+"x, travel feedrate="+rcTravelFeedrateMultiply+"x" );
		return true;
	}

	public void setDebugLevel(int level) {
		debugLevel = level;
	}
	public int getDebugLevel() {
		return debugLevel;
	}

	public double getFeedrateLimit() {
		return rcFeedrateLimit;
	}

	public boolean setFeedrateLimit(double limit) {
		rcFeedrateLimit = limit;
		return true;
	}
}
