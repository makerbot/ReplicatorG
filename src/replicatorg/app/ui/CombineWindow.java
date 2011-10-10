package replicatorg.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import replicatorg.model.Build;
import replicatorg.plugin.toolpath.ToolpathGenerator;
import replicatorg.plugin.toolpath.ToolpathGeneratorFactory;
import replicatorg.plugin.toolpath.ToolpathGeneratorThread;

/*
Part of the ReplicatorG project - http://www.replicat.org
Copyright (c) 2008 Zach Smith

Forked from Arduino: http://www.arduino.cc

Based on Processing http://www.processing.org
Copyright (c) 2004-05 Ben Fry and Casey Reas
Copyright (c) 2001-04 Massachusetts Institute of Technology

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

$Id: MainWindow.java 370 2008-01-19 16:37:19Z mellis $
 */

/**
 * This window provides the user with a way to generate a gcode toolpath for four of the same object, using different 
 * skeinforge settings for each one. It offers a box for selecting an STL or gcode file and a place to store the generated gcode.
 * Borrows somewhat from the DualStrusionWindow.
 * @author Ted
 *
 */
public class CombineWindow extends JFrame implements ToolpathGenerator.GeneratorListener
{

	// at some point we may want to have multiple files get combined
	private File combineFile, saveFile;
	private List<File> tmpFiles;
	private boolean replaceStart, replaceEnd;
	
	private int numRepetitions;

	private JLabel fileLabel, saveLabel;
	private JLabel startCBoxLabel, endCBoxLabel;
	
	private JTextField fileInput;
	private JTextField saveInput;

	private JCheckBox repStartCheckBox;
	private JCheckBox repEndCheckBox;
	
	private JButton fileBrowseButton;
	private JButton saveBrowseButton;
	private JButton mergeButton;
	private JButton cancelButton;
	
	public CombineWindow()
	{
		this(null);
	}
	
	public CombineWindow(String path)
	{


		// for now we're hard coding this as four repetitions
		numRepetitions = 4;
		
		fileLabel = new JLabel("File:");
		fileInput = new JTextField();
		if(path != null)
			fileInput.setText(path);
		fileBrowseButton = new JButton("Browse...");
		fileBrowseButton.addActionListener(new ActionListener(){
			// open a file chooser
			public void actionPerformed(ActionEvent arg0) {
				String s = null;
				if(!fileInput.getText().equals(""))
				{
					s = GcodeSelectWindow.goString(new File(fileInput.getText()));	
				}
				else
				{
					s = GcodeSelectWindow.goString();
				}
				if(s != null)
				{
					fileInput.setText(s);
				}

			}
		});
		
		// Set up the save button, field, etc.
		saveLabel = new JLabel("Save as:");
		saveInput = new JTextField();
		saveBrowseButton = new JButton("Browse...");
		saveBrowseButton.addActionListener(new ActionListener(){
			// open a file chooser
			public void actionPerformed(ActionEvent arg0) {
				String s = null;
				if(!saveInput.getText().equals(""))
				{
					s = GcodeSelectWindow.goString(new File(saveInput.getText()));	
				}
				else
				{
					s = GcodeSelectWindow.goString();
				}
				if(s != null)
				{
					saveInput.setText(s);
				}

			}
		});

		startCBoxLabel = new JLabel("Use default start.gcode: ");
		endCBoxLabel = new JLabel("Use default end.gcode: ");
		repStartCheckBox = new JCheckBox();
		repEndCheckBox = new JCheckBox();
		
		mergeButton = new JButton("Merge");
		mergeButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {

				combineFile = new File(fileInput.getText());
				saveFile = new File(saveInput.getText());
				
				replaceStart = repStartCheckBox.isSelected();
				replaceEnd = repEndCheckBox.isSelected();
				
				if(combineFile.getName().endsWith(".stl")) 
				{
					// creates some number of gcode files to be merged
					createGcode();
				}
				else
				{
					final String message = "I know the file browser let you choose gcode, " +
								"but this function only supports stl at the moment. Sorry about that.";
					JOptionPane.showConfirmDialog(CombineWindow.this, message, "Oops!", 
											JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				combineGcode(null);
				
			}
			
		});
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				close();
			}
			
		});
		
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
					// pair the labels, textfeilds, and browse buttons in three groups
					.addGroup(layout.createSequentialGroup()
						.addComponent(fileLabel)
						.addComponent(saveLabel))
					.addGroup(layout.createSequentialGroup()
						.addComponent(fileInput)
						.addComponent(saveInput))
					.addGroup(layout.createSequentialGroup()
						.addComponent(fileBrowseButton)
						.addComponent(saveBrowseButton)))
				// below that, pair the check boxes and their labels, one above the other (seq)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(startCBoxLabel)
						.addComponent(repStartCheckBox))
					.addGroup(layout.createParallelGroup()
						.addComponent(endCBoxLabel)
						.addComponent(repEndCheckBox)))
				// then the cancel and merge next to each other (par)
				.addGroup(layout.createParallelGroup()
					.addComponent(cancelButton)
					.addComponent(mergeButton)));
		layout.setHorizontalGroup(
			layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					// lay out the three pairs in a row
					.addGroup(layout.createParallelGroup()
						.addComponent(fileLabel)
						.addComponent(saveLabel))
					.addGroup(layout.createParallelGroup()
						.addComponent(fileInput)
						.addComponent(saveInput))
					.addGroup(layout.createParallelGroup()
						.addComponent(fileBrowseButton)
						.addComponent(saveBrowseButton)))
				// the check boxes (paired with their labels) are on top of each other (par)
				.addGroup(layout.createParallelGroup()
					.addGroup(layout.createSequentialGroup()
						.addComponent(startCBoxLabel)
						.addComponent(repStartCheckBox))
					.addGroup(layout.createSequentialGroup()
						.addComponent(endCBoxLabel)
						.addComponent(repEndCheckBox)))
				// the merge and cancel buttons are adjacent (seq)
				.addGroup(layout.createSequentialGroup()
					.addComponent(cancelButton)
					.addComponent(mergeButton)));
		
		setResizable(true);
		setLocation(400, 0);
		pack();
		setVisible(true);
	}
	
	// takes our stl and produces some number of temporary gcode files 
	public void createGcode()
	{
		
		try{
			Build readFile = new Build(combineFile.getAbsolutePath());
			ToolpathGenerator generator = ToolpathGeneratorFactory.createSelectedGenerator();
			JFrame progressFrame = new JFrame("File " + numRepetitions);
			
			ToolpathGeneratorThread genThread = new ToolpathGeneratorThread(progressFrame, generator, readFile);

			genThread.setDualStrusionSupportFlag(true, 200, 200, "File " + numRepetitions);
			genThread.addListener(new ToolpathGenerator.GeneratorListener(){

				@Override
				public void updateGenerator(String message) {
					// this is only received when genThread's DualStrusionSupportFlag is set to true
					if(message.equals("Config Done"))
					{
						numRepetitions--;
						createGcode();
					}
					
				}

				@Override
				public void generationComplete(Completion completion, Object details) {
					
				}
				
			});
			genThread.start();
			
//			
//			Build build1 = new Build(combineFile.getAbsolutePath());
//			Build build2 = new Build(combineFile.getAbsolutePath());
//			Build build3 = new Build(combineFile.getAbsolutePath());
//			Build build4 = new Build(combineFile.getAbsolutePath());
//			ToolpathGenerator generator1 = ToolpathGeneratorFactory.createSelectedGenerator();
//			ToolpathGenerator generator2 = ToolpathGeneratorFactory.createSelectedGenerator();
//			ToolpathGenerator generator3 = ToolpathGeneratorFactory.createSelectedGenerator();
//			ToolpathGenerator generator4 = ToolpathGeneratorFactory.createSelectedGenerator();
//
//			JFrame frame1 = new JFrame("Quadrant A");
//			JFrame frame2 = new JFrame("Quadrant B");
//			JFrame frame3 = new JFrame("Quadrant C");
//			JFrame frame4 = new JFrame("Quadrant D");
//			frame1.setLocation(200, 200);
//			frame2.setLocation(500, 200);
//			frame3.setLocation(500, 500);
//			frame4.setLocation(200, 500);
//
//			ToolpathGeneratorThread thread1 = new ToolpathGeneratorThread(frame1, generator1, build1);
//			ToolpathGeneratorThread thread2 = new ToolpathGeneratorThread(frame2, generator2, build2);
//			ToolpathGeneratorThread thread3 = new ToolpathGeneratorThread(frame3, generator3, build3);
//			ToolpathGeneratorThread thread4 = new ToolpathGeneratorThread(frame4, generator4, build4);
//			thread1.addListener(this);
//			thread2.addListener(this);
//			thread3.addListener(this);
//			thread4.addListener(this);
//			thread1.start();
//			thread2.start();
//			thread3.start();
//			thread4.start();
			
		}
		catch(IOException e)
		{
			System.err.println("cannot read stl");
		} 
		
	}
	
	public void combineGcode(Collection<File> tehCodez)
	{
		//either we were given gcode from the start
		//or we started with stl
	}
	
	public void close()
	{
		setVisible(false);
		dispose();
	}

	@Override
	public void updateGenerator(String message) {
		System.out.println(message);
	}

	@Override
	public void generationComplete(Completion completion, Object details) {
		// TODO Auto-generated method stub
		
	}

	
}