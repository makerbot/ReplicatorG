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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.undo.UndoManager;

import replicatorg.app.Base;
import replicatorg.app.syntax.SyntaxDocument;

public class BuildCode extends BuildElement implements Comparable<BuildCode> {
	/** Pretty name (no extension), not the full file name */
	public String name;

	/** File object for where this code is located */
	public File file;

	/** Text of the program text for this tab */
	public String program;

	/** Document object for this tab; includes undo information, etc. */
	public SyntaxDocument document;

	/** Undo Manager for this tab, each tab keeps track of their own */
	public UndoManager undo;

	// saved positions from last time this tab was used
	public int selectionStart;

	public int selectionStop;

	public int scrollPosition;

	public BuildCode(String name, File file) {
		this.name = name;
		this.file = file;
		try {
			load();
		} catch (IOException e) {
			Base.logger.severe("error while loading code " + name);
		}
	}

	/**
	 * Load this piece of code from a file.
	 */
	public void load() throws IOException {
		if (file == null) {
			program = "";
			setModified(true);
		} else {
			program = Base.loadFile(file);
			setModified(false);
		}
	}

	/**
	 * Save this piece of code, regardless of whether the modified flag is set
	 * or not.
	 */
	public void save() throws IOException {
		// TODO re-enable history
		// history.record(s, SketchHistory.SAVE);

		Base.saveFile(program, file);
		setModified(false);
	}

	/**
	 * Save this file to another location, used by Sketch.saveAs()
	 */
	public void saveAs(File newFile) throws IOException {
		Base.saveFile(program, newFile);
		file = newFile;
		name = file.getName();
		// we're still truncating the suffix, for now.
		int lastIdx = name.lastIndexOf('.');
		if (lastIdx > 0) {
			name = name.substring(0, lastIdx);
		}
		setModified(false);
	}

	public int compareTo(BuildCode other) {
		if (name == null) { return (other.name == null)?0:-1; }
		return name.compareTo(other.name);
	}

	public Type getType() {
		return BuildElement.Type.GCODE;
	}

	@Override
	void writeToStream(OutputStream ostream) {
		// TODO Auto-generated method stub
		
	}
}
