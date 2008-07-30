/*
  SerialPassthroughDriver

  This is a driver to control a machine that contains a GCode parser and communicates via Serial Port.

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

package processing.app.drivers;

import processing.app.*;
import processing.core.*;

import gnu.io.*;
import java.util.*;
import org.w3c.dom.*;

public class SerialPassthroughDriver extends Driver
{
	/**
	* this is if we need to talk over serial
	*/
	private Serial serial;

	/**
	* our array of gcode commands
	*/
	private Vector commands;
	
	/**
	* our pointer to the currently executing command
	*/
	private int currentCommand = 0;
	
	/**
	* the size of the buffer on the GCode host
	*/
	private int maxBufferSize = 128;
	
	/**
	* the amount of data we've sent and is in the buffer.
	*/
	private int bufferSize = 0;
	
	/**
	* how many commands do we have in the buffer?
	*/
	private int bufferLength = 0;
	
	/**
	* What did we get back from serial?
	*/
	private String result = "";
		
	public SerialPassthroughDriver(Node xml)
	{
		super();
		
		//init our variables.
		commands = new Vector();
		bufferSize = 0;
		bufferLength = 0;
		currentCommand = 0;
		
		//some decent default prefs.
		String name = Serial.list()[0];
		int    rate = Preferences.getInteger("serial.debug_rate");
		char   parity = Preferences.get("serial.parity").charAt(0);
		int    databits = Preferences.getInteger("serial.databits");
		float  stopbits = new Float(Preferences.get("serial.stopbits")).floatValue();
		
		//load from our XML config, if we have it.
		//TODO: actually load up from XML.
		
		//declare our serial guy.
		try {
			serial = new Serial(name, rate, parity, databits, stopbits);
		} catch (SerialException e) {
			//TODO: report the error here.
		}
	}
	
	/**
	 * Actually execute the GCode we just parsed.
	 */
	public void execute()
	{
		String next = clean(command);

		//skip empty commands.
		if (next.length() == 0)
			return;

		//check to see if we got a response.
		do {
			readResponse();
		} while (bufferSize + next.length() > maxBufferSize);
		
		//will it fit into our buffer?
		if (bufferSize + next.length() < maxBufferSize)
		{
			serial.write(next + "\n");
			
			//wait between commands.
			try{ Thread.sleep(5); } catch (Exception e){}
			
			//record it in our buffer tracker.
			commands.add(next);
			bufferSize += next.length() + 1;
			bufferLength++;
			
			//debug... let us know whts up!
			//Debug.c("Sent: " + next);
			//Debug.d("Buffer: " + bufferSize + " (" + bufferLength + " commands)");
		}
	}

	public String clean(String command)
	{
		String clean = command;
		
		//strip comments
		int commentIndex = clean.indexOf(';');
		if (commentIndex >= -1)
			clean = clean.substring(0, commentIndex);
		
		//trim whitespace
		clean = clean.trim();	
		
		//remove spaces
		clean = clean.replaceAll(" ", "");
		
		return clean;
	}
	
	public void readResponse()
	{
		String cmd = "";
		
		//read for any results.
		for (;;)
		{
			try
			{
				//no serial? bail!
				if (serial.available() > 0)
				{
					//get it as ascii.
					char c = serial.readChar();
					result += c;
				
					//is it a done command?
					if (c == '\n')
					{
						if (result.startsWith("ok"))
						{
							cmd = (String)commands.get(currentCommand);

							//if (result.length() > 2)
							//	Debug.c("got: " + result.substring(0, result.length()-2) + "(" + bufferSize + " - " + (cmd.length() + 1) + " = " + (bufferSize - (cmd.length() + 1)) + ")");

							bufferSize -= cmd.length() + 1;
							bufferLength--;
							
							currentCommand++;
							result = "";
							
							//Debug.d("Buffer: " + bufferSize + " (" + bufferLength + " commands)");

							//bail, buffer is almost empty.  fill it!
							if (bufferLength < 2)
								break;
							
							//we'll never get here.. for testing.
							//if (bufferLength == 0)
							//	Debug.d("Empty buffer!! :(");
						}
						//else if (result.startsWith("T:"))
						//	Debug.d(result.substring(0, result.length()-2));
						//else
						//	Debug.c(result.substring(0, result.length()-2));
							
						result = "";
					}
				}
				else
					break;
			} catch (Exception e) {
				break;
			}
		}	
	}
}