package replicatorg.app.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.vecmath.Point3d;

import replicatorg.app.ui.modeling.EditingModel;
import replicatorg.dualstrusion.CodeCombination;
import replicatorg.model.Build;
import replicatorg.model.BuildModel;
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
 * This window provides the user with a way to generate a gcode toolpath for a few of the same object, using different 
 * skeinforge settings for each one. It offers a box for selecting an STL file and a place to store the generated gcode.
 * Borrows somewhat from the DualStrusionWindow.
 * 
 * This is a tool intended for Makerbot's R&D team to do materials testing, it may have uses or hazards 
 * that are documented only verbally. Use at your own risk.
 * @author Ted
 *
 */
public class CombineWindow extends JFrame
{
	private File sourceFile;
	private File destFile;
	
	// Setting these here is a secondary layer of protection, not strictly necessary
	private int numRepetitions = 2;
	private double availableSpace = 100;
	
	private List<File> combineFiles;
	
	private JLabel fileLabel;
	private JLabel saveLabel;
	private JLabel numLabel;
	private JLabel spaceLabel;

	private JTextField fileInput;
	private JTextField saveInput;
	private JTextField numInput;
	private JTextField spaceInput;
	
	private JButton fileBrowseButton;
	private JButton saveBrowseButton;
	private JButton mergeButton;
	private JButton cancelButton;
	
	// With this we can manipulate the model!
	private final MainWindow theMainWindow;
	
	public CombineWindow(final MainWindow mw)
	{
		this(null, mw);
	}
	
	public CombineWindow(String path, final MainWindow mw)
	{
		super("Row Combination (EXPERIMENTAL functionality)");
		theMainWindow = mw;

		fileLabel = new JLabel("File:");
		fileInput = new JTextField();
		
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

		if(path != null)
		{
			fileInput.setText(path);
			saveInput.setText(path.replace(".stl", ".gcode"));
		}

		numLabel = new JLabel("Rows of copies:");
		numInput = new JTextField();
		numInput.setText("2");
		numLabel.setToolTipText("Number of rows of copies, 1 per row");
		
		spaceLabel = new JLabel("Build Depth(mm):");
		spaceInput = new JTextField();
		spaceInput.setText("100"); // -- default depth mm for TOM 
		spaceLabel.setToolTipText("Depth of your build platform");
		
		mergeButton = new JButton("Merge");
		mergeButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {

				if(fileInput.getText().equals("") || saveInput.getText().equals("") ||
					numInput.getText().equals("") || spaceInput.getText().equals(""))
				{
					JOptionPane.showConfirmDialog(CombineWindow.this, "Please fill in all fields.", 
							"Oops!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
					return;
				}
				sourceFile = new File(fileInput.getText());
				destFile = new File(saveInput.getText());
				
				numRepetitions = Integer.parseInt(numInput.getText());
				availableSpace = Double.parseDouble(spaceInput.getText());
				
				if(numRepetitions < 2)
				{
					JOptionPane.showConfirmDialog(CombineWindow.this, "Number of copies must be greater than 2.", 
							"Oops!", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
					return;
				}
				theMainWindow.handleOpenUnchecked(sourceFile.getAbsolutePath(), 0, 0, 0, 0);
				// make sure we load up the file before doing all this, otherwise
				//   we're always just operating on the open file. maybe that would be a good thing?
				
				if(sourceFile.getName().endsWith(".stl")) 
				{
					// creates some number of gcode files to be merged
					createGcode();
				}
				else
				{
					final String message = "I know the file browser let you choose gcode, " +
								"but this function only supports .stl at the moment. Sorry about that.";
					JOptionPane.showConfirmDialog(CombineWindow.this, message, "Oops!", 
											JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
					return;
				}
				
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
		
		int fsLabelWidth = 45;
		int fsInputMinW = 10, fsInputPrefW = 200;
		
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(
			layout.createParallelGroup()
				// add the two rows of label, box, browse button
				.addGroup(layout.createParallelGroup()
					.addGroup(layout.createSequentialGroup()
						.addComponent(fileLabel, fsLabelWidth, fsLabelWidth, fsLabelWidth)
						.addComponent(fileInput, fsInputMinW, fsInputPrefW, Short.MAX_VALUE)
						.addComponent(fileBrowseButton))
					.addGroup(layout.createSequentialGroup()
						.addComponent(saveLabel, fsLabelWidth, fsLabelWidth, fsLabelWidth)
						.addComponent(saveInput, fsInputMinW, fsInputPrefW, Short.MAX_VALUE)
						.addComponent(saveBrowseButton)))
				// add the number settings
				.addGroup(layout.createParallelGroup()
					.addGroup(layout.createSequentialGroup()
						.addComponent(numLabel)
						.addComponent(numInput))
					.addGroup(layout.createSequentialGroup()
						.addComponent(spaceLabel)
						.addComponent(spaceInput)))
				// the merge and cancel buttons are adjacent (seq)
				.addGroup(layout.createSequentialGroup()
					.addComponent(cancelButton)
					.addComponent(mergeButton)));
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				// add the two rows of label, box, browse button
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(fileLabel)
						.addComponent(fileInput)
						.addComponent(fileBrowseButton))
					.addGroup(layout.createParallelGroup()
						.addComponent(saveLabel)
						.addComponent(saveInput)
						.addComponent(saveBrowseButton)))
				// add the number settings
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(numLabel)
						.addComponent(numInput))
					.addGroup(layout.createParallelGroup()
						.addComponent(spaceLabel)
						.addComponent(spaceInput)))
				// then the cancel and merge next to each other (par)
				.addGroup(layout.createParallelGroup()
					.addComponent(cancelButton)
					.addComponent(mergeButton)));
		
		setResizable(true);
		setLocation(400, 0);
		pack();
		setVisible(true);
	}
	
	// takes our stl and produces some number of temporary gcode files
	private void createGcode()
	{
		// Here we can do some editing to the stl model, pushing it to the front center
		EditingModel em = theMainWindow.previewPanel.getModel();
		em.center();

		Point3d bbUp = new Point3d();
		em.getBoundingBox().getUpper(bbUp);

		// this assumes that the center function leaves half of the object on the 
		// far side of the y origin, and half on the near side. That's how it works, yeah?
		double halfDepth = bbUp.y;
		double halfSpace = availableSpace/2;
		// move to the front (Y - availableSpace/2 + boundingbox.high)
		em.translateObject(0, halfDepth - halfSpace , 0);

		// calculate the offset from numReps, available space, and object size
		double objDepth = halfDepth*2;
		double spaceNeeded = objDepth*numRepetitions;
		if(spaceNeeded > availableSpace)
		{
			JOptionPane.showConfirmDialog(CombineWindow.this, "Not enough space for that many objects.", "Oops!", 
									JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
			return;
		}
			
		double offset = availableSpace - spaceNeeded;
		offset /= (numRepetitions-1);
		offset += objDepth;
		
		BuildModel bm = em.getBuildModel();
		String fileNameBase = sourceFile.getAbsolutePath();
		fileNameBase = fileNameBase.substring(0, fileNameBase.lastIndexOf("."));
		
		// these allow us to call up each gcode generator in turn
		final Queue<ToolpathGeneratorThread> generatorQueue = new LinkedList<ToolpathGeneratorThread>();
		final CountDownLatch numLeft = new CountDownLatch(numRepetitions);
		
		combineFiles = new ArrayList<File>();
		
		for(int i = 0; i < numRepetitions; i++)
		{
			try
			{
				// save our shape in a new file
				File tmpFile = new File(fileNameBase + "_tmp_" + i + ".stl");
				bm.saveAs(tmpFile);
				
				combineFiles.add(new File(fileNameBase + "_tmp_" + i + ".gcode"));
				
				// generate gcode from our new file
				final Build readFile = new Build(tmpFile.getAbsolutePath());
				ToolpathGenerator generator = ToolpathGeneratorFactory.createSelectedGenerator();
				JFrame progressFrame = new JFrame("File " + i);
				
				ToolpathGeneratorThread genThread = new ToolpathGeneratorThread(progressFrame, generator, readFile);
	
				// this makes sure we can be alerted when the config finishes
				genThread.setDualStrusionSupportFlag(true, 200, 200, "File " + i);
				
				// this listener starts each remaining generator in turn, and grabs each gcode as it becomes available
				genThread.addListener(new ToolpathGenerator.GeneratorListener(){
	
					private final Queue<ToolpathGeneratorThread> genQueue = generatorQueue;
					
					@Override
					public void updateGenerator(String message) {
						/* This is only received when genThread's DualStrusionSupportFlag is set to true.
						 * We wait until Config is finished each time, because config boxes are modal.
						 */
						if(message.equals("Config Done"))
						{
							if(!genQueue.isEmpty())
								genQueue.poll().start();
						}
						
					}
	
					@Override
					public void generationComplete(Completion completion, Object details) {
						
						if(completion == Completion.SUCCESS)
						{
							numLeft.countDown();
							if(numLeft.getCount() == 0)
							{
								combineGcode();
							}
						}
					}
					
				});
				
				generatorQueue.add(genThread);
				
				// Now we can move the shape for the next file
				em.translateObject(0, offset, 0);
			}
			catch(IOException e)
			{
				System.err.println("cannot read stl");
				close();
			}
		}
		
		// start the first generator in the queue
		generatorQueue.poll().start();
		
	}
	
	private void combineGcode()
	{
		CodeCombination.mergeGCodes(destFile, combineFiles);
		theMainWindow.handleOpenUnchecked(destFile.getAbsolutePath(), 0, 0, 0, 0);
		close();
	}
	
	private void close()
	{
		setVisible(false);
		dispose();
	}

	
}
