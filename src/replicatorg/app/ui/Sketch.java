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

package replicatorg.app.ui;

import java.awt.FileDialog;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.JOptionPane;

import replicatorg.app.Base;
import replicatorg.app.Sketchbook;

/**
 * Stores information about files in the current sketch
 */
public class Sketch {
	static File tempBuildFolder;

	MainWindow editor;

	/**
	 * Name of sketch, which is the name of main file (without .gcode extension)
	 */
	public String name;

	/**
	 * Name of 'main' file, used by load(), such as sketch_04040.gcode
	 */
	String mainFilename;

	/**
	 * true if any of the files have been modified.
	 */
	public boolean modified;

	public File folder;

	public File dataFolder;

	public File codeFolder;

	static public final int GCODE = 0;

	static public final String flavorExtensionsReal[] = new String[] { ".gcode" };

	static public final String flavorExtensionsShown[] = new String[] { "", ".gcode" };

	public SketchCode current;

	int currentIndex;

	public int codeCount;

	public SketchCode code[];

	public int hiddenCount;

	public SketchCode hidden[];

	Hashtable zipFileContents;

	// all these set each time build() is called
	String mainClassName;

	String classPath;

	boolean externalRuntime;

	/**
	 * path is location of the main .gcode file, because this is also simplest
	 * to use when opening the file from the finder/explorer.
	 */
	public Sketch(MainWindow editor, String path) throws IOException {
		this.editor = editor;

		File mainFile = new File(path);
		// System.out.println("main file is " + mainFile);

		mainFilename = mainFile.getName();
		// System.out.println("main file is " + mainFilename);

		// get the name of the sketch by chopping .gcode
		// off of the main file name
		if (mainFilename.endsWith(".gcode")) {
			name = mainFilename.substring(0, mainFilename.length() - 6);
		}

		tempBuildFolder = Base.getBuildFolder();
		// Base.addBuildFolderToClassPath();

		folder = new File(new File(path).getParent());
		// System.out.println("sketch dir is " + folder);

		load();
	}

	/**
	 * Build the list of files.
	 * 
	 * Generally this is only done once, rather than each time a change is made,
	 * because otherwise it gets to be a nightmare to keep track of what files
	 * went where, because not all the data will be saved to disk.
	 * 
	 * This also gets called when the main sketch file is renamed, because the
	 * sketch has to be reloaded from a different folder.
	 * 
	 * Another exception is when an external editor is in use, in which case the
	 * load happens each time "run" is hit.
	 */
	public void load() {
		codeFolder = new File(folder, "code");
		dataFolder = new File(folder, "data");

		// get list of files in the sketch folder
		String list[] = folder.list();

		for (int i = 0; i < list.length; i++) {
			if (list[i].endsWith(".gcode"))
				codeCount++;
		}

		code = new SketchCode[codeCount];
		hidden = new SketchCode[hiddenCount];

		int codeCounter = 0;
		int hiddenCounter = 0;

		for (int i = 0; i < list.length; i++) {
			if (list[i].endsWith(".gcode")) {
				code[codeCounter++] = new SketchCode(list[i].substring(0,
						list[i].length() - 6), new File(folder, list[i]), GCODE);

			}
		}

		// some of the hidden files may be bad too, so use hiddenCounter
		// added for rev 0121, fixes bug found by axel
		hiddenCount = hiddenCounter;

		// remove any entries that didn't load properly from codeCount
		int index = 0;
		while (index < codeCount) {
			if ((code[index] == null) || (code[index].program == null)) {
				for (int i = index + 1; i < codeCount; i++) {
					code[i - 1] = code[i];
				}
				codeCount--;

			} else {
				index++;
			}
		}

		// move the main class to the first tab
		// start at 1, if it's at zero, don't bother
		for (int i = 1; i < codeCount; i++) {
			if (code[i].file.getName().equals(mainFilename)) {
				SketchCode temp = code[0];
				code[0] = code[i];
				code[i] = temp;
				break;
			}
		}

		// sort the entries at the top
		sortCode();

		// set the main file to be the current tab
		setCurrent(0);
	}

	protected void insertCode(SketchCode newCode) {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// add file to the code/codeCount list, resort the list
		if (codeCount == code.length) {
			SketchCode temp[] = new SketchCode[codeCount + 1];
			System.arraycopy(code, 0, temp, 0, codeCount);
			code = temp;
		}
		code[codeCount++] = newCode;
	}

	protected void sortCode() {
		// cheap-ass sort of the rest of the files
		// it's a dumb, slow sort, but there shouldn't be more than ~5 files
		for (int i = 1; i < codeCount; i++) {
			int who = i;
			for (int j = i + 1; j < codeCount; j++) {
				if (code[j].name.compareTo(code[who].name) < 0) {
					who = j; // this guy is earlier in the alphabet
				}
			}
			if (who != i) { // swap with someone if changes made
				SketchCode temp = code[who];
				code[who] = code[i];
				code[i] = temp;
			}
		}
	}

	boolean renamingCode;

	public void newCode() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// if read-only, give an error
		if (isReadOnly()) {
			// if the files are read-only, need to first do a "save as".
			Base
					.showMessage(
							"Sketch is Read-Only",
							"Some files are marked \"read-only\", so you'll\n"
									+ "need to re-save the sketch in another location,\n"
									+ "and try again.");
			return;
		}

		renamingCode = false;
		//editor.status.edit("Name for new file:", "");
	}

	public void renameCode() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// if read-only, give an error
		if (isReadOnly()) {
			// if the files are read-only, need to first do a "save as".
			Base
					.showMessage(
							"Sketch is Read-Only",
							"Some files are marked \"read-only\", so you'll\n"
									+ "need to re-save the sketch in another location,\n"
									+ "and try again.");
			return;
		}

		// ask for new name of file (internal to window)
		// TODO maybe just popup a text area?
		renamingCode = true;
		String prompt = (currentIndex == 0) ? "New name for sketch:"
				: "New name for file:";
		String oldName = current.name + flavorExtensionsShown[current.flavor];
		//editor.status.edit(prompt, oldName);
	}

	/**
	 * This is called upon return from entering a new file name. (that is, from
	 * either newCode or renameCode after the prompt) This code is almost
	 * identical for both the newCode and renameCode cases, so they're kept
	 * merged except for right in the middle where they diverge.
	 */
	public void nameCode(String newName) {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// if renaming to the same thing as before, just ignore.
		// also ignoring case here, because i don't want to write
		// a bunch of special stuff for each platform
		// (osx is case insensitive but preserving, windows insensitive,
		// *nix is sensitive and preserving.. argh)
		if (renamingCode && newName.equalsIgnoreCase(current.name)) {
			// exit quietly for the 'rename' case.
			// if it's a 'new' then an error will occur down below
			return;
		}

		// don't allow blank names
		if (newName.trim().equals("")) {
			return;
		}

		if (newName.trim().equals(".gcode")) {
			return;
		}

		String newFilename = null;
		int newFlavor = 0;

		// separate into newName (no extension) and newFilename (with ext)
		// add .gcode to file if it has no extension
		if (newName.endsWith(".gcode")) {
			newFilename = newName;
			newName = newName.substring(0, newName.length() - 6);
			newFlavor = GCODE;

		} else {
			newFilename = newName + ".gcode";
			newFlavor = GCODE;
		}

		// dots are allowed for the .gcode and .java, but not in the name
		// make sure the user didn't name things poo.time.gcode
		// or something like that (nothing against poo time)
		if (newName.indexOf('.') != -1) {
			newName = Sketchbook.sanitizedName(newName);
			newFilename = newName + ".gcode";
		}

		// create the new file, new SketchCode object and load it
		File newFile = new File(folder, newFilename);
		if (newFile.exists()) { // yay! users will try anything
			Base.showMessage("Nope", "A file named \"" + newFile
					+ "\" already exists\n" + "in \""
					+ folder.getAbsolutePath() + "\"");
			return;
		}

		File newFileHidden = new File(folder, newFilename + ".x");
		if (newFileHidden.exists()) {
			// don't let them get away with it if they try to create something
			// with the same name as something hidden
			Base.showMessage("No Way",
					"A hidden tab with the same name already exists.\n"
							+ "Use \"Unhide\" to bring it back.");
			return;
		}

		if (renamingCode) {
			if (currentIndex == 0) {
				// get the new folder name/location
				File newFolder = new File(folder.getParentFile(), newName);
				if (newFolder.exists()) {
					Base.showWarning("Cannot Rename",
							"Sorry, a sketch (or folder) named " + "\""
									+ newName + "\" already exists.", null);
					return;
				}

				// unfortunately this can't be a "save as" because that
				// only copies the sketch files and the data folder
				// however this *will* first save the sketch, then rename

				// first get the contents of the editor text area
				if (current.modified) {
					current.program = editor.getText();
					try {
						// save this new SketchCode
						current.save();
					} catch (Exception e) {
						Base.showWarning("Error",
								"Could not rename the sketch. (0)", e);
						return;
					}
				}

				if (!current.file.renameTo(newFile)) {
					Base.showWarning("Error", "Could not rename \""
							+ current.file.getName() + "\" to \""
							+ newFile.getName() + "\"", null);
					return;
				}

				// save each of the other tabs because this is gonna be
				// re-opened
				try {
					for (int i = 1; i < codeCount; i++) {
						// if (code[i].modified) code[i].save();
						code[i].save();
					}
				} catch (Exception e) {
					Base.showWarning("Error",
							"Could not rename the sketch. (1)", e);
					return;
				}

				// now rename the sketch folder and re-open
				boolean success = folder.renameTo(newFolder);
				if (!success) {
					Base.showWarning("Error",
							"Could not rename the sketch. (2)", null);
					return;
				}
				// if successful, set base properties for the sketch

				File mainFile = new File(newFolder, newName + ".gcode");
				mainFilename = mainFile.getAbsolutePath();

				// having saved everything and renamed the folder and the main
				// .gcode,
				// use the editor to re-open the sketch to re-init state
				// (unfortunately this will kill positions for carets etc)
				editor.handleOpenUnchecked(mainFilename, currentIndex,
						editor.textarea.getSelectionStart(), editor.textarea
								.getSelectionEnd(), editor.textarea
								.getScrollPosition());

				// get the changes into the sketchbook menu
				// (re-enabled in 0115 to fix bug #332)
				//editor.sketchbook.rebuildMenus();

			} else { // else if something besides code[0]
				if (!current.file.renameTo(newFile)) {
					Base.showWarning("Error", "Could not rename \""
							+ current.file.getName() + "\" to \""
							+ newFile.getName() + "\"", null);
					return;
				}

				// just reopen the class itself
				current.name = newName;
				current.file = newFile;
				current.flavor = newFlavor;
			}

		} else { // creating a new file
			try {
				newFile.createNewFile(); // TODO returns a boolean
			} catch (IOException e) {
				Base.showWarning("Error", "Could not create the file \""
						+ newFile + "\"\n" + "in \"" + folder.getAbsolutePath()
						+ "\"", e);
				return;
			}
			SketchCode newCode = new SketchCode(newName, newFile, newFlavor);
			insertCode(newCode);
		}

		// sort the entries
		sortCode();

		// set the new guy as current
		setCurrent(newName + flavorExtensionsShown[newFlavor]);

		// update the tabs
		// editor.header.repaint();

		editor.header.rebuild();

		// force the update on the mac?
		Toolkit.getDefaultToolkit().sync();
		// editor.header.getToolkit().sync();
	}

	/**
	 * Remove a piece of code from the sketch and from the disk.
	 */
	public void deleteCode() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// if read-only, give an error
		if (isReadOnly()) {
			// if the files are read-only, need to first do a "save as".
			Base
					.showMessage(
							"Sketch is Read-Only",
							"Some files are marked \"read-only\", so you'll\n"
									+ "need to re-save the sketch in another location,\n"
									+ "and try again.");
			return;
		}

		// confirm deletion with user, yes/no
		Object[] options = { "OK", "Cancel" };
		String prompt = (currentIndex == 0) ? "Are you sure you want to delete this sketch?"
				: "Are you sure you want to delete \"" + current.name
						+ flavorExtensionsShown[current.flavor] + "\"?";
		int result = JOptionPane.showOptionDialog(editor, prompt, "Delete",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				options, options[0]);
		if (result == JOptionPane.YES_OPTION) {
			if (currentIndex == 0) {
				// need to unset all the modified flags, otherwise tries
				// to do a save on the handleNew()

				// delete the entire sketch
				Base.removeDir(folder);

				// get the changes into the sketchbook menu
				// sketchbook.rebuildMenus();

				// make a new sketch, and i think this will rebuild the sketch
				// menu
				editor.handleNewUnchecked();

			} else {
				// delete the file
				if (!current.file.delete()) {
					Base.showMessage("Couldn't do it", "Could not delete \""
							+ current.name + "\".");
					return;
				}

				// remove code from the list
				removeCode(current);

				// just set current tab to the main tab
				setCurrent(0);

				// update the tabs
				editor.header.repaint();
			}
		}
	}

	protected void removeCode(SketchCode which) {
		// remove it from the internal list of files
		// resort internal list of files
		for (int i = 0; i < codeCount; i++) {
			if (code[i] == which) {
				for (int j = i; j < codeCount - 1; j++) {
					code[j] = code[j + 1];
				}
				codeCount--;
				return;
			}
		}
		System.err.println("removeCode: internal error.. could not find code");
	}

	public void hideCode() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// if read-only, give an error
		if (isReadOnly()) {
			// if the files are read-only, need to first do a "save as".
			Base
					.showMessage(
							"Sketch is Read-Only",
							"Some files are marked \"read-only\", so you'll\n"
									+ "need to re-save the sketch in another location,\n"
									+ "and try again.");
			return;
		}

		// don't allow hide of the main code
		// TODO maybe gray out the menu on setCurrent(0)
		if (currentIndex == 0) {
			Base.showMessage("Can't do that", "You cannot hide the main "
					+ ".gcode file from a sketch\n");
			return;
		}

		// rename the file
		File newFile = new File(current.file.getAbsolutePath() + ".x");
		if (!current.file.renameTo(newFile)) {
			Base.showWarning("Error", "Could not hide " + "\""
					+ current.file.getName() + "\".", null);
			return;
		}
		current.file = newFile;

		// move it to the hidden list
		if (hiddenCount == hidden.length) {
			SketchCode temp[] = new SketchCode[hiddenCount + 1];
			System.arraycopy(hidden, 0, temp, 0, hiddenCount);
			hidden = temp;
		}
		hidden[hiddenCount++] = current;

		// remove it from the main list
		removeCode(current);

		// update the tabs
		setCurrent(0);
		editor.header.repaint();
	}

	public void unhideCode(String what) {
		SketchCode unhideCode = null;
		String name = what.substring(0, (what.indexOf(".") == -1 ? what
				.length() : what.indexOf(".")));
		String extension = what.indexOf(".") == -1 ? "" : what.substring(what
				.indexOf("."));

		for (int i = 0; i < hiddenCount; i++) {
			if (hidden[i].name.equals(name)
					&& Sketch.flavorExtensionsShown[hidden[i].flavor]
							.equals(extension)) {
				// unhideIndex = i;
				unhideCode = hidden[i];

				// remove from the 'hidden' list
				for (int j = i; j < hiddenCount - 1; j++) {
					hidden[j] = hidden[j + 1];
				}
				hiddenCount--;
				break;
			}
		}
		// if (unhideIndex == -1) {
		if (unhideCode == null) {
			System.err.println("internal error: could find " + what
					+ " to unhide.");
			return;
		}
		if (!unhideCode.file.exists()) {
			Base.showMessage("Can't unhide", "The file \"" + what
					+ "\" no longer exists.");
			// System.out.println(unhideCode.file);
			return;
		}
		String unhidePath = unhideCode.file.getAbsolutePath();
		File unhideFile = new File(unhidePath.substring(0,
				unhidePath.length() - 2));

		if (!unhideCode.file.renameTo(unhideFile)) {
			Base.showMessage("Can't unhide", "The file \"" + what
					+ "\" could not be" + "renamed and unhidden.");
			return;
		}
		unhideCode.file = unhideFile;
		insertCode(unhideCode);
		sortCode();
		setCurrent(unhideCode.name);
		editor.header.repaint();
	}

	/**
	 * Sets the modified value for the code in the frontmost tab.
	 */
	public void setModified(boolean state) {
		current.modified = state;
		calcModified();
	}

	public void calcModified() {
		modified = false;
		for (int i = 0; i < codeCount; i++) {
			if (code[i].modified) {
				modified = true;
				break;
			}
		}
		editor.header.repaint();
	}

	/**
	 * Save all code in the current sketch.
	 */
	public boolean save() throws IOException {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// first get the contents of the editor text area
		if (current.modified) {
			current.program = editor.getText();
		}

		// don't do anything if not actually modified
		// if (!modified) return false;

		if (isReadOnly()) {
			// if the files are read-only, need to first do a "save as".
			Base
					.showMessage(
							"Sketch is read-only",
							"Some files are marked \"read-only\", so you'll\n"
									+ "need to re-save this sketch to another location.");
			// if the user cancels, give up on the save()
			if (!saveAs())
				return false;
		}

		for (int i = 0; i < codeCount; i++) {
			if (code[i].modified)
				code[i].save();
		}
		calcModified();
		return true;
	}

	/**
	 * Handles 'Save As' for a sketch.
	 * <P>
	 * This basically just duplicates the current sketch folder to a new
	 * location, and then calls 'Save'. (needs to take the current state of the
	 * open files and save them to the new folder.. but not save over the old
	 * versions for the old sketch..)
	 * <P>
	 * Also removes the previously-generated .class and .jar files, because they
	 * can cause trouble.
	 */
	public boolean saveAs() throws IOException {
		// get new name for folder
		FileDialog fd = new FileDialog(editor, "Save sketch folder as...",
				FileDialog.SAVE);
		if (isReadOnly()) {
			// default to the sketchbook folder
			fd.setDirectory(Base.preferences.get("sketchbook.path",null));
		} else {
			// default to the parent folder of where this was
			fd.setDirectory(folder.getParent());
		}
		fd.setFile(folder.getName());

		fd.setVisible(true);
		String newParentDir = fd.getDirectory();
		String newName = fd.getFile();

		// user cancelled selection
		if (newName == null)
			return false;
		newName = Sketchbook.sanitizeName(newName);

		// make sure there doesn't exist a tab with that name already
		// (but allow it if it's just the main tab resaving itself.. oops)
		File codeAlready = new File(folder, newName + ".gcode");
		if (codeAlready.exists() && (!newName.equals(name))) {
			Base.showMessage("Nope", "You can't save the sketch as \""
					+ newName + "\"\n"
					+ "because the sketch already has a tab with that name.");
			return false;
		}

		// make sure there doesn't exist a tab with that name already
		File hiddenAlready = new File(folder, newName + ".gcode.x");
		if (hiddenAlready.exists()) {
			Base.showMessage("Nope", "You can't save the sketch as \""
					+ newName + "\"\n" + "because the sketch already has a "
					+ "hidden tab with that name.");
			return false;
		}

		// new sketch folder
		File newFolder = new File(newParentDir, newName);

		// make sure the paths aren't the same
		if (newFolder.equals(folder)) {
			Base.showWarning("You can't fool me",
					"The new sketch name and location are the same as\n"
							+ "the old. I ain't not doin nuthin' not now.",
					null);
			return false;
		}

		// check to see if the user is trying to save this sketch
		// inside the same sketch
		try {
			String newPath = newFolder.getCanonicalPath() + File.separator;
			String oldPath = folder.getCanonicalPath() + File.separator;

			if (newPath.indexOf(oldPath) == 0) {
				Base.showWarning("How very Borges of you",
						"You cannot save the sketch into a folder\n"
								+ "inside itself. This would go on forever.",
						null);
				return false;
			}
		} catch (IOException e) {
		}

		// if the new folder already exists, then need to remove
		// its contents before copying everything over
		// (user will have already been warned)
		if (newFolder.exists()) {
			Base.removeDir(newFolder);
		}
		// in fact, you can't do this on windows because the file dialog
		// will instead put you inside the folder, but it happens on osx a lot.

		// now make a fresh copy of the folder
		newFolder.mkdirs();

		// grab the contents of the current tab before saving
		// first get the contents of the editor text area
		if (current.modified) {
			current.program = editor.getText();
		}

		// save the other tabs to their new location
		for (int i = 1; i < codeCount; i++) {
			File newFile = new File(newFolder, code[i].file.getName());
			code[i].saveAs(newFile);
		}

		// save the hidden code to its new location
		for (int i = 0; i < hiddenCount; i++) {
			File newFile = new File(newFolder, hidden[i].file.getName());
			hidden[i].saveAs(newFile);
		}

		// re-copy the data folder (this may take a while.. add progress bar?)
		if (dataFolder.exists()) {
			File newDataFolder = new File(newFolder, "data");
			Base.copyDir(dataFolder, newDataFolder);
		}

		// re-copy the code folder
		if (codeFolder.exists()) {
			File newCodeFolder = new File(newFolder, "code");
			Base.copyDir(codeFolder, newCodeFolder);
		}

		// save the main tab with its new name
		File newFile = new File(newFolder, newName + ".gcode");
		code[0].saveAs(newFile);

		editor
				.handleOpenUnchecked(newFile.getPath(), currentIndex,
						editor.textarea.getSelectionStart(), editor.textarea
								.getSelectionEnd(), editor.textarea
								.getScrollPosition());

		// Name changed, rebuild the sketch menus
		//editor.sketchbook.rebuildMenusAsync();

		// let MainWindow know that the save was successful
		return true;
	}

	/**
	 * Prompt the user for a new file to the sketch. This could be .class or
	 * .jar files for the code folder, .gcode files for the project, or .dll,
	 * .jnilib, or .so files for the code folder
	 */
	public void addFile() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		// if read-only, give an error
		if (isReadOnly()) {
			// if the files are read-only, need to first do a "save as".
			Base
					.showMessage(
							"Sketch is Read-Only",
							"Some files are marked \"read-only\", so you'll\n"
									+ "need to re-save the sketch in another location,\n"
									+ "and try again.");
			return;
		}

		// get a dialog, select a file to add to the sketch
		String prompt = "Select an image or other data file to copy to your sketch";
		// FileDialog fd = new FileDialog(new Frame(), prompt, FileDialog.LOAD);
		FileDialog fd = new FileDialog(editor, prompt, FileDialog.LOAD);
		fd.setVisible(true);

		String directory = fd.getDirectory();
		String filename = fd.getFile();
		if (filename == null)
			return;

		// copy the file into the folder. if people would rather
		// it move instead of copy, they can do it by hand
		File sourceFile = new File(directory, filename);

		// now do the work of adding the file
		addFile(sourceFile);
	}

	/**
	 * Add a file to the sketch. <p/> .gcode files will be added to the sketch
	 * folder. <br/> All other files will be added to the "data" folder. <p/> If
	 * they don't exist already, the "code" or "data" folder will be created.
	 * <p/>
	 * 
	 * @return true if successful.
	 */
	public boolean addFile(File sourceFile) {
		String filename = sourceFile.getName();
		File destFile = null;
		boolean addingCode = false;

		if (filename.toLowerCase().endsWith(".gcode")) {
			destFile = new File(this.folder, filename);
			addingCode = true;

		} else {
			// File dataFolder = new File(this.folder, "data");
			if (!dataFolder.exists())
				dataFolder.mkdirs();
			destFile = new File(dataFolder, filename);
		}

		// make sure they aren't the same file
		if (!addingCode && sourceFile.equals(destFile)) {
			Base.showWarning("You can't fool me",
					"This file has already been copied to the\n"
							+ "location where you're trying to add it.\n"
							+ "I ain't not doin nuthin'.", null);
			return false;
		}

		// in case the user is "adding" the code in an attempt
		// to update the sketch's tabs
		if (!sourceFile.equals(destFile)) {
			try {
				Base.copyFile(sourceFile, destFile);

			} catch (IOException e) {
				Base.showWarning("Error adding file", "Could not add '"
						+ filename + "' to the sketch.", e);
				return false;
			}
		}

		// make the tabs update after this guy is added
		if (addingCode) {
			String newName = destFile.getName();
			int newFlavor = -1;
			if (newName.toLowerCase().endsWith(".gcode")) {
				newName = newName.substring(0, newName.length() - 6);
				newFlavor = GCODE;
			}

			// see also "nameCode" for identical situation
			SketchCode newCode = new SketchCode(newName, destFile, newFlavor);
			insertCode(newCode);
			sortCode();
			setCurrent(newName);
			editor.header.repaint();
		}
		return true;
	}

	/**
	 * Change what file is currently being edited.
	 * <OL>
	 * <LI> store the String for the text of the current file.
	 * <LI> retrieve the String for the text of the new file.
	 * <LI> change the text that's visible in the text area
	 * </OL>
	 */
	public void setCurrent(int which) {
		// if current is null, then this is the first setCurrent(0)
		if ((currentIndex == which) && (current != null)) {
			return;
		}

		// get the text currently being edited
		if (current != null) {
			current.program = editor.getText();
			current.selectionStart = editor.textarea.getSelectionStart();
			current.selectionStop = editor.textarea.getSelectionEnd();
			current.scrollPosition = editor.textarea.getScrollPosition();
		}

		current = code[which];
		currentIndex = which;
		editor.setCode(current);
		// editor.setDocument(current.document,
		// current.selectionStart, current.selectionStop,
		// current.scrollPosition, current.undo);

		// set to the text for this file
		// 'true' means to wipe out the undo buffer
		// (so they don't undo back to the other file.. whups!)
		/*
		 * editor.setText(current.program, current.selectionStart,
		 * current.selectionStop, current.undo);
		 */

		// set stored caret and scroll positions
		// editor.textarea.setScrollPosition(current.scrollPosition);
		// editor.textarea.select(current.selectionStart,
		// current.selectionStop);
		// editor.textarea.setSelectionStart(current.selectionStart);
		// editor.textarea.setSelectionEnd(current.selectionStop);
		editor.header.rebuild();
	}

	/**
	 * Internal helper function to set the current tab based on a name (used by
	 * codeNew and codeRename).
	 */
	public void setCurrent(String findName) {
		String name = findName.substring(0,
				(findName.indexOf(".") == -1 ? findName.length() : findName
						.indexOf(".")));
		String extension = findName.indexOf(".") == -1 ? "" : findName
				.substring(findName.indexOf("."));

		for (int i = 0; i < codeCount; i++) {
			if (name.equals(code[i].name)
					&& Sketch.flavorExtensionsShown[code[i].flavor]
							.equals(extension)) {
				setCurrent(i);
				return;
			}
		}
	}

	/**
	 * Cleanup temporary files used during a build/run.
	 */
	public void cleanup() {
		// if the java runtime is holding onto any files in the build dir, we
		// won't be able to delete them, so we need to force a gc here
		System.gc();

		// note that we can't remove the builddir itself, otherwise
		// the next time we start up, internal runs using Runner won't
		// work because the build dir won't exist at startup, so the classloader
		// will ignore the fact that that dir is in the CLASSPATH in run.sh
		Base.removeDescendants(tempBuildFolder);
	}

	/**
	 * Run the GCode.
	 */
	public boolean handleRun() {
		// make sure the user didn't hide the sketch folder
		ensureExistence();

		current.program = editor.getText();

		// TODO record history here
		// current.history.record(program, SketchHistory.RUN);

		// in case there were any boogers left behind
		// do this here instead of after exiting, since the exit
		// can happen so many different ways.. and this will be
		// better connected to the dataFolder stuff below.
		cleanup();

		return (mainClassName != null);
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
	 * Make sure the sketch hasn't been moved or deleted by some nefarious user.
	 * If they did, try to re-create it and save. Only checks to see if the main
	 * folder is still around, but not its contents.
	 */
	protected void ensureExistence() {
		if (folder.exists())
			return;

		Base.showWarning("Sketch Disappeared",
				"The sketch folder has disappeared.\n "
						+ "Will attempt to re-save in the same location,\n"
						+ "but anything besides the code will be lost.", null);
		try {
			folder.mkdirs();
			modified = true;

			for (int i = 0; i < codeCount; i++) {
				code[i].save(); // this will force a save
			}
			for (int i = 0; i < hiddenCount; i++) {
				hidden[i].save(); // this will force a save
			}
			calcModified();

		} catch (Exception e) {
			Base.showWarning("Could not re-save sketch",
					"Could not properly re-save the sketch. "
							+ "You may be in trouble at this point,\n"
							+ "and it might be time to copy and paste "
							+ "your code to another text editor.", e);
		}
	}

	/**
	 * Returns true if this is a read-only sketch. Used for the examples
	 * directory, or when sketches are loaded from read-only volumes or folders
	 * without appropriate permissions.
	 */
	public boolean isReadOnly() {
		// check to see if each modified code file can be written to
		for (int i = 0; i < codeCount; i++) {
			if (code[i].modified && !code[i].file.canWrite()
					&& code[i].file.exists()) {
				// System.err.println("found a read-only file " + code[i].file);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns path to the main .gcode file for this sketch.
	 */
	public String getMainFilePath() {
		return code[0].file.getAbsolutePath();
	}

	public void prevCode() {
		int prev = currentIndex - 1;
		if (prev < 0)
			prev = codeCount - 1;
		setCurrent(prev);
	}

	public void nextCode() {
		setCurrent((currentIndex + 1) % codeCount);
	}
}
