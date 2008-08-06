/*
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

package processing.app;

import processing.app.drivers.*;

import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.vecmath.*;
import org.xml.sax.*;
import java.lang.Math.*;
import java.util.*;
import javax.swing.JOptionPane;

public class Machine
{
	// our editor object.
	protected Editor editor;
	
	// this is the xml config for this machine.
	protected Node machineNode;
	
	// our current thread.
	protected Thread thread;
	
	// the name of our machine.
	protected String name;
	
	// our driver object
	protected Driver driver;
	protected SimulationDriver simulator;
		
	//our pause variable
	protected boolean paused = false;
	protected boolean stopped = false;
	
	//estimated build time in millis
	protected long estimatedBuildTime = 0;

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
		
		//load our drivers
		loadDriver();
		loadToolDrivers();
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
		//record the time.
		Date started = new Date();
		
		simulator.createWindow();
		
		editor.setVisible(true);
		editor.textarea.selectNone();
		editor.textarea.disable();
		editor.textarea.scrollTo(0, 0);

		//estimate build time.
		estimate();
		
		//do that build!
		System.out.println("Running GCode...");
		if (build())
		{
			//record the time.
			Date finished = new Date();
		
			//let them know.
			notifyBuildComplete(started, finished);
		}
		
		//clean things up.
		driver.dispose();
		simulator.dispose();
		simulator = null;
		driver = null;

		//re-enable the gui and shit.
		editor.textarea.enable();
	}
	
	private void estimate()
	{
		EstimationDriver estimator = new EstimationDriver();
		
		//run each line through the estimator
		int total = editor.textarea.getLineCount();
		for (int i=0; i<total; i++)
		{
			String line = editor.textarea.getLineText(i);
			
			//use our parser to handle the stuff.
			estimator.parse(line);
			estimator.execute();
		}

		System.out.println("Estimated build time is: " + EstimationDriver.getBuildTimeString(estimator.getBuildTime()));
	}
	
	
	private boolean build()
	{
		int total = editor.textarea.getLineCount();
		for (int i=0; i<total; i++)
		{
			editor.textarea.scrollTo(i, 0);
			editor.highlightLine(i);
			
			String line = editor.textarea.getLineText(i);
			
			//use our parser to handle the stuff.
			simulator.parse(line);
			driver.parse(line);
			
			//for handling job flow.
			try {
				driver.handleStops();
			} catch (JobEndException e) {
				return false;
			} catch (JobCancelledException e) {
				return false;
			} catch (JobRewindException e) {
				i = -1;
				continue;
			}
			
			//simulate the command.
			simulator.execute();
			
			//execute the command and snag any errors.
			try {
				driver.execute();
			} catch (GCodeException e) {
				//TODO: prompt the user to continue.
				System.out.println("Error: " + e.getMessage());
			}
			
			//are we paused?
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
				return false;
		}
		
		//wait for driver to finish up.
		while (!driver.isFinished())
		{
			try {
				thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	/**
	* give a prompt and stuff about the build being done with elapsed time, etc.
	*/
	private void notifyBuildComplete(Date started, Date finished)
	{
		long elapsed = finished.getTime() - started.getTime();
		
		String message = "Build finished.\n\n";
		message += "Completed in " + EstimationDriver.getBuildTimeString(elapsed);
		
		JOptionPane.showMessageDialog(null, message);
	}
	
	private void loadDriver()
	{
		//load our utility drivers
		simulator = new SimulationDriver(0);
		
		//load our actual driver
		NodeList kids = machineNode.getChildNodes();
		for (int j=0; j<kids.getLength(); j++)
		{
			Node kid = kids.item(j);

			if (kid.getNodeName().equals("driver"))
			{
				driver = Driver.factory(kid);
				return;
			}
		}

		System.out.println("No driver config found.");
		
		driver = Driver.factory();
	}
	
	private void loadToolDrivers()
	{
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
