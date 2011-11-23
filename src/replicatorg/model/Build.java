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
 */

package replicatorg.model;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JOptionPane;

import replicatorg.app.Base;
import replicatorg.app.ui.MainWindow;

/**
 * Stores information about files in the current build
 */
public class Build {

	/** The editor window associated with this build.  We should remove this dependency or replace it with a
	 * buildupdatelistener or the like. */
	MainWindow editor;

	private String name;
	private boolean hasMainWindow;
	/** Name of the build, which is the name of main file, sans extension. */
	public String getName() { return name; }

	/** Name of source file, used by load().  Recognized types so far are:
	 * .stl - model file
	 * .obj - model file
	 * .dae - model file
	 * .gcode - gcode file
	 * .ngc - gcode file
	 * .zip - composite build file
	 */
	String mainFilename;

	/**
	 * The folder which the base file is located in.
	 */
	public File folder;

	/**
	 * The elements of this build.
	 */
	private Vector<BuildElement> elements = new Vector<BuildElement>();

	/**
	 * The element that this build was opened as.
	 */
	private BuildElement openedElement;
	/**
	 * Retrieve the element that this build was opened as.  If a user opens a gcode file, it should 
	 * initially display that gcode rather than the model, etc.
	 */
	public BuildElement getOpenedElement() { return openedElement; }

	
	public Build(String path) throws IOException {
		hasMainWindow = false;
		if (path == null) {
			mainFilename = null;
			name = "Untitled";
			folder = new File("~");
			BuildElement code = new BuildCode(null,null);
			openedElement = code;
			code.setModified(true);
			elements.add(code);
		} else {
			File mainFile = new File(path);
			mainFilename = mainFile.getName();
			String suffix = "";
			int lastIdx = mainFilename.lastIndexOf('.');
			if (lastIdx > 0) {
				suffix = mainFilename.substring(lastIdx+1);
				name = mainFilename.substring(0,lastIdx);
			} else {
				name = mainFilename;
			}
			String parentPath = new File(path).getParent(); 
			if (parentPath == null) {
				parentPath = ".";
			}
			folder = new File(parentPath);
			if ("stl".equalsIgnoreCase(suffix) || "obj".equalsIgnoreCase(suffix) || "dae".equalsIgnoreCase(suffix)) {
				modelFile = mainFile;
			}
			loadCode();
			loadModel();
			if (("gcode".equalsIgnoreCase(suffix) || "ngc".equalsIgnoreCase(suffix)) && getCode() != null) {
				openedElement = getCode();
			} else {
				openedElement = getModel();
			}
		}
	}
	/**
	 * path is location of the main .gcode file, because this is also simplest
	 * to use when opening the file from the finder/explorer.
	 */
	public Build(MainWindow editor, String path) throws IOException {
		hasMainWindow = true;
		this.editor = editor;
		if (path == null) {
			mainFilename = null;
			name = "Untitled";
			folder = new File("~");
			BuildElement code = new BuildCode(null,null);
			openedElement = code;
			code.setModified(true);
			elements.add(code);
		} else {

			File mainFile = new File(path);
			mainFilename = mainFile.getName();
			String suffix = "";
			int lastIdx = mainFilename.lastIndexOf('.');
			if (lastIdx > 0) {
				suffix = mainFilename.substring(lastIdx+1);
				name = mainFilename.substring(0,lastIdx);
			} else {
				name = mainFilename;
			}

			//protect against loading files that may have caused a crash last time
			File crashCheck = new File(name + ".crash");
			if(crashCheck.exists())
			{
				crashCheck.delete();
				int op = JOptionPane.showConfirmDialog(null, "It looks as though ReplicatorG may have crashed\n" +
						"last time it tried to load this file.\nRe-loading the same file could cause another crash,\n" +
						"would you like to load this file anyway?", "Remnants of a crash detected!", 
						JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE);
				if(op != 0)
					return; 
			}
			crashCheck.createNewFile();
			
			String parentPath = new File(path).getParent(); 
			if (parentPath == null) {
				parentPath = ".";
			}
			folder = new File(parentPath);
			if ("stl".equalsIgnoreCase(suffix) || "obj".equalsIgnoreCase(suffix) || "dae".equalsIgnoreCase(suffix)) {
				modelFile = mainFile;
			}
			loadCode();
			loadModel();
			if (("gcode".equalsIgnoreCase(suffix) || "ngc".equalsIgnoreCase(suffix)) && getCode() != null) {
				openedElement = getCode();
			} else {
				openedElement = getModel();
			}
			
			crashCheck.delete();
		}
	}

	public void reloadCode() {
		Iterator<BuildElement> iterator = elements.iterator();
		while(iterator.hasNext()) {
			BuildElement e = iterator.next();
			if (e instanceof BuildCode) {
				elements.removeElement(e);
				break;
			}
		}
		loadCode();
	}

	public void loadCode() {
		File codeFile = new File(folder, name + ".gcode");
		if (codeFile.exists()) {
			elements.add(new BuildCode(name, codeFile));
		} else {
			codeFile = new File(folder, name + ".ngc");
			if (codeFile.exists()) {
				elements.add(new BuildCode(name, codeFile));
			}
		}
	}

	File modelFile = null;

	public void loadModel() {
		if (modelFile == null || !modelFile.exists()) {
			modelFile = new File(folder, name + ".stl");
		}
		if (modelFile.exists()) {
			elements.add(new BuildModel(this, modelFile));
		}		
	}


	/**
	 * Save all code in the current sketch.
	 */
	public boolean save() throws IOException {
		if (mainFilename == null) {
			return saveAs();
		}

		if (isReadOnly()) {
			// if the files are read-only, need to first do a "save as".
			Base.showMessage(
					"File is read-only",
					"This file is marked \"read-only\", so you'll\n"
					+ "need to re-save this file to another location.");
			// if the user cancels, give up on the save()
			if (!saveAs())
				return false;
			return true;
		}

		BuildCode code = getCode();
		if (code != null) {
			if(hasMainWindow )
			{
				if (code.isModified()) { 
					code.program = editor.getText();
					code.save();
				}
			}
		}
		BuildModel model = getModel();
		if (model != null) {
			if (model.isModified()) {
				model.save();
			}
		}
		return true;
	}

	/**
	 * Handles 'Save As' for a build.
	 * <P>
	 * This basically just duplicates the current build to a new
	 * location, and then calls 'Save'. (needs to take the current state of the
	 * open files and save them to the new folder.. but not save over the old
	 * versions for the old sketch..)
	 * <P>
	 * Also removes the previously-generated .class and .jar files, because they
	 * can cause trouble.
	 */
	public boolean saveAs() throws IOException {
		// get new name for folder
		FileDialog fd = new FileDialog(new Frame(), "Save file as...",
				FileDialog.SAVE);
		// default to the folder that this file is in
		fd.setDirectory(folder.getCanonicalPath());
		fd.setFile(mainFilename);

		fd.setVisible(true);
		String parentDir = fd.getDirectory();
		String newName = fd.getFile();
		// user cancelled selection
		if (newName == null)
			return false;

		File folder = new File(parentDir);

		// Find base name
		if (newName.toLowerCase().endsWith(".gcode")) newName = newName.substring(0, newName.length()-6);
		if (newName.toLowerCase().endsWith(".ngc")) newName = newName.substring(0, newName.length()-4);
		if (newName.toLowerCase().endsWith(".stl")) newName = newName.substring(0, newName.length()-4);
		if (newName.toLowerCase().endsWith(".obj")) newName = newName.substring(0, newName.length()-4);
		if (newName.toLowerCase().endsWith(".dae")) newName = newName.substring(0, newName.length()-4);


		BuildCode code = getCode();
		if (code != null) {
			// grab the contents of the current tab before saving
			// first get the contents of the editor text area
			if(hasMainWindow)
			{
				if (code.isModified()) {
					code.program = editor.getText();
				}
			}
			File newFile = new File(folder, newName+".gcode");
			code.saveAs(newFile);
		}

		BuildModel model = getModel();
		if (model != null) {
			File newFile = new File(folder, newName+".stl");
			model.saveAs(newFile);
		}

		this.name = newName;
		this.mainFilename = fd.getFile();
		this.folder = folder;
		return true;
	}

	/**
	 * Return the gcode object.
	 */
	public BuildCode getCode() {
		for (BuildElement e : elements) {
			if (e instanceof BuildCode) { return (BuildCode)e; }
		}
		return null;
	}

	/**
	 * Return the model object.
	 */
	public BuildModel getModel() {
		for (BuildElement e : elements) {
			if (e instanceof BuildModel) { return (BuildModel)e; }
		}
		return null;
	}

	protected int countLines(String what) {
		char c[] = what.toCharArray();
		int count = 0;
		for (int i = 0; i < c.length; i++) {
			if (c[i] == '\n')
				count++;
		}
		return count;
	}

	static public String scrubComments(String what) {
		char p[] = what.toCharArray();

		int index = 0;
		while (index < p.length) {
			// for any double slash comments, ignore until the end of the line
			if ((p[index] == '/') && (index < p.length - 1)
					&& (p[index + 1] == '/')) {
				p[index++] = ' ';
				p[index++] = ' ';
				while ((index < p.length) && (p[index] != '\n')) {
					p[index++] = ' ';
				}

				// check to see if this is the start of a new multiline comment.
				// if it is, then make sure it's actually terminated somewhere.
			} else if ((p[index] == '/') && (index < p.length - 1)
					&& (p[index + 1] == '*')) {
				p[index++] = ' ';
				p[index++] = ' ';
				boolean endOfRainbow = false;
				while (index < p.length - 1) {
					if ((p[index] == '*') && (p[index + 1] == '/')) {
						p[index++] = ' ';
						p[index++] = ' ';
						endOfRainbow = true;
						break;

					} else {
						index++;
					}
				}
				if (!endOfRainbow) {
					throw new RuntimeException(
							"Missing the */ from the end of a "
							+ "/* comment */");
				}
			} else { // any old character, move along
				index++;
			}
		}
		return new String(p);
	}



	/**
	 * Returns true if this is a read-only sketch. Used for the examples
	 * directory, or when sketches are loaded from read-only volumes or folders
	 * without appropriate permissions.
	 */
	public boolean isReadOnly() {
		BuildCode code = getCode();
		if (code == null) return false;
		// check to see if each modified code file can be written to
		return (code.isModified() && 
				!code.file.canWrite() &&
				code.file.exists());
	}

	/**
	 * Returns path to the main .gcode file for this sketch.
	 */
	public String getMainFilePath() {
		BuildCode code = getCode();
		if (code != null && code.file != null) {
			return code.file.getAbsolutePath();
		}
		return null;
	}

	/**
	 * @return True if any of the elements of the build are modified; false otherwise
	 */
	public boolean hasModifiedElements() {
		boolean rv = false;
		for (BuildElement e : elements) {
			if (e.isModified()) { rv = true; }
		}
		return rv;
	}

}
