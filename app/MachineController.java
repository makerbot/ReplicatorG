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
import processing.app.exceptions.*;
import processing.app.models.*;
import processing.app.tools.*;

import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.vecmath.*;
import org.xml.sax.*;
import java.lang.Math.*;
import java.util.*;
import javax.swing.JOptionPane;

public class MachineController
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
	
	// our machine model objects
	protected MachineModel model;
	
	//our pause variable
	protected boolean paused = false;
	protected boolean stopped = false;
	
	//estimated build time in millis
	protected long estimatedBuildTime = 0;
	
	//our warmup/cooldown commands
	protected Vector warmupCommands;
	protected Vector cooldownCommands;
	
	/**
	  * Creates the machine object.
	  */
	public MachineController(Node mNode)
	{
		//save our XML
		machineNode = mNode;

		paused = false;
		stopped = false;
		
		parseName();
		System.out.println("Loading machine: " + name);

		//load our various objects
		loadModel();
		loadDriver();
		loadExtraPrefs();
	}
	
	public void setEditor(Editor e)
	{
	  editor = e;
	}
	
	public Editor getEditor()
	{
	  return editor;
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
//		System.out.println("Running...");

		//some sort of race condition.  this hack seems to help.
		try {
			thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//record the time.
		Date started = new Date();

		//start simulator
//		System.out.println("a");
		simulator.createWindow();
//		System.out.println("b");
		editor.setVisible(true);
//		System.out.println("c");
	
		//estimate build time.
		System.out.println("Estimating build time...");
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
		//driver.dispose();
		//driver = null;
		simulator.dispose();
		simulator = null;
		
		//re-enable the gui and shit.
		editor.textarea.enable();
	}
	
	public void estimate()
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
	
	private void runWarmupCommands() throws BuildFailureException
	{
		if (warmupCommands.size() > 0)
		{
			System.out.println("Running warmup commands.");
			Iterator itr = warmupCommands.iterator();
			while (itr.hasNext())
			{
				String command = (String)itr.next();
				
				driver.parse(command);
				
				//execute the command and snag any errors.
				try
				{
					driver.execute();
				}
				catch (GCodeException e)
				{
					//TODO: prompt the user to continue.
					System.out.println("Error: " + e.getMessage());
				}
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
		}
	}

	private void runCooldownCommands() throws BuildFailureException
	{
		if (cooldownCommands.size() > 0)
		{
			System.out.println("Running cooldown commands.");
			Iterator itr = cooldownCommands.iterator();
			while (itr.hasNext())
			{
				String command = (String)itr.next();
				
				driver.parse(command);
				
				//execute the command and snag any errors.
				try
				{
					driver.execute();
				}
				catch (GCodeException e)
				{
					//TODO: prompt the user to continue.
					System.out.println("Error: " + e.getMessage());
				}
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
		}
	}
	
	private boolean build()
	{
		try
		{
			runWarmupCommands();
			
			System.out.println("Running build.");

			int total = editor.textarea.getLineCount();
			for (int i=0; i<total; i++)
			{
				editor.textarea.scrollTo(i, 0);
				editor.highlightLine(i);
				
				String line = editor.textarea.getLineText(i);
				
				//System.out.println("running: " + line);
				
				//use our parser to handle the stuff.
				simulator.parse(line);
				driver.parse(line);
				
				//System.out.println("parsed");
				
				//for handling job flow.
				try {
					driver.getParser().handleStops();
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
				
				//System.out.println("simulated");
				
				//execute the command and snag any errors.
				try
				{
					driver.execute();
				}
				catch (GCodeException e)
				{
					//TODO: prompt the user to continue.
					System.out.println("Error: " + e.getMessage());
				}
				
				//did we get any errors?
				driver.checkErrors();
				
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
			
			//chill out.
			runCooldownCommands();
		}
		catch (BuildFailureException e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Build Failure", JOptionPane.ERROR_MESSAGE);
			
			return false;
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
	
	private void loadModel()
	{
		model = new MachineModel();
		model.loadXML(machineNode);
	}
	
	private void loadDriver()
	{
		//load our utility drivers
		simulator = new SimulationDriver();
		simulator.setMachine(model);
			
		//load our actual driver
		NodeList kids = machineNode.getChildNodes();
		for (int j=0; j<kids.getLength(); j++)
		{
			Node kid = kids.item(j);

			if (kid.getNodeName().equals("driver"))
			{
				driver = DriverFactory.factory(kid);
				driver.setMachine(model);
				driver.initialize();
				return;
			}
		}

		System.out.println("No driver config found.");
		
		driver = DriverFactory.factory();
		driver.setMachine(model);
		driver.initialize();
	}
	
	private void loadExtraPrefs()
	{
		String[] commands = null;
		String command = null;
		
		warmupCommands = new Vector();
		if (XML.hasChildNode(machineNode, "warmup"))
		{
			String warmup = XML.getChildNodeValue(machineNode, "warmup");
			commands = warmup.split("\n");
			
			for (int i=0; i<commands.length; i++)
			{
				command = commands[i].trim();
				warmupCommands.add(new String(command));
				//System.out.println("Added warmup: " + command);
			}
		}
		
		cooldownCommands = new Vector();
		if (XML.hasChildNode(machineNode, "cooldown"))
		{
			String cooldown = XML.getChildNodeValue(machineNode, "cooldown");
			commands = cooldown.split("\n");
			
			for (int i=0; i<commands.length; i++)
			{
				command = commands[i].trim();
				cooldownCommands.add(new String(command));
				//System.out.println("Added cooldown: " + command);
			}
		}
	}
	
	public Driver getDriver()
	{
	  return driver;
	}
	
	public MachineModel getModel()
	{
		return model;
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
