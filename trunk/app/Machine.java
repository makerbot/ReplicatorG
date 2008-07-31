/*
  Part of the ReplicatorG project - http://www.replicat.org

  This class takes a gcode command, parses it and then does something with it.

  @author	Hoeken
  @www http://www.zachhoeken.org

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

package processing.app;

import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import java.lang.Math.*;

public class Machine
{
	// our editor object.
	protected Editor editor;
	
	// this is the xml config for this machine.
	protected Node machineNode;
	
	protected Thread thread;
	
	// the name of our machine.
	protected String name;
	
	// our driver object
	protected Driver driver;
	
	//our pause variable
	protected boolean paused = false;
	protected boolean stopped = false;

/*	
	Color currentLineBackground;
	Color successfulLineBackground;
	Color failedLineBackground;
*/
	
	/**
	  * Creates the machine object.
	  */
	public Machine(Node mNode, Editor edit)
	{
		//save our XML
		editor = edit;
		machineNode = mNode;

		parseName();

		paused = false;
		stopped = false;
		
		System.out.println("Loading machine: " + name);
		
		loadDriver();
		
/*
		currentLineBackground = new Color(0x00, 0x99, 0xFF);
		successfulLineBackground = new Color(0x33, 0xFF, 0x66);
		failedLineBackground = new Color(0xCC, 0x33, 0x33);
*/
	}
	
	public void setThread(Thread iThread)
	{
		thread = iThread;
	}
	
	private void parseName()
	{
		NodeList kids = machineNode.getChildNodes();

		for (int j=0; j<kids.getLength(); j++)
		{
			Node kid = kids.item(j);

			if (kid.getNodeName().equals("name"))
			{
				name = kid.getFirstChild().getNodeValue().trim();
				return;
			}
		}
		
		name = "Unknown";
	}

	public void run()
	{
		editor.textarea.selectNone();
		editor.textarea.disable();
		editor.textarea.scrollTo(0, 0);
		
		int total = editor.textarea.getLineCount();
		for (int i=0; i<total; i++)
		{
			editor.textarea.scrollTo(i, 0);
			editor.highlightLine(i);
			
			String line = editor.textarea.getLineText(i);
			
			driver.parse(line);
			driver.execute();
			driver.commandFinished();
			
			while (this.isPaused())
			{
				try {
					thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			//bail if we got interrupted.
			if (this.isStopped())
			{
				System.out.println("stopped.");
				break;
			}
		}	
		
		editor.textarea.enable();
	}
	
	private void loadDriver()
	{
		NodeList kids = machineNode.getChildNodes();

		for (int j=0; j<kids.getLength(); j++)
		{
			Node kid = kids.item(j);

			if (kid.getNodeName().equals("driver"))
			{
				driver = Driver.factory(kid);
			}
		}
		
		driver = Driver.factory();
	}
	
	synchronized public void stop()
	{
		stopped = true;
	}
	
	synchronized public boolean isStopped()
	{
		return stopped;
	}
	
	synchronized public void pause()
	{
		paused = true;
	}
	
	synchronized public void unpause()
	{
		paused = false;
	}
	
	synchronized public boolean isPaused()
	{
		return paused;
	}	
}
