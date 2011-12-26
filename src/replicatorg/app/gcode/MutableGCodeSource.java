package replicatorg.app.gcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import replicatorg.app.Base;
import replicatorg.machine.model.ToolheadAlias;
import replicatorg.model.GCodeSource;

public class MutableGCodeSource implements GCodeSource {

	LinkedList<String> source = new LinkedList<String>();
	
	public MutableGCodeSource() {
	}
	
	public MutableGCodeSource(GCodeSource shallowCopy) {
		source.addAll(shallowCopy.asList());
	}
	
	public MutableGCodeSource(Collection<String> shallowCopy) {
		source.addAll(shallowCopy);
	}
	
	public MutableGCodeSource(File sourceFile) {
		String curline;
		
		try {
			BufferedReader bir = new BufferedReader(new FileReader(sourceFile));
			curline = bir.readLine();
			while (curline != null) {
				source.add(curline);
				curline = bir.readLine();
			}
			bir.close();
		} catch (IOException e) {
			System.err.println("couldnt read file " + sourceFile.getAbsolutePath());
			return;
		}
	}
	
	@Override
	public Iterator<String> iterator() {
		return source.iterator();
	}

	@Override
	public List<String> asList() {
		return source;
	}

	@Override
	public int getLineCount() {
		return source.size();
	}

	public void add(String line) {
		source.add(line);
	}
	public void add(GCodeSource toAdd) {
		add(toAdd.asList());
	}
	public void add(Collection<String> toAdd) {
		source.addAll(toAdd);
	}
	
	public void add(int location, String line) {
		source.add(location, line);
	}
	public void add(int location, GCodeSource toAdd) {
		add(location, toAdd.asList());
	}
	public void add(int location, Collection<String> toAdd) {
		source.addAll(location, toAdd);
	}
	
	public void writeToFile(File f) {

		try {
			FileWriter bwr = new FileWriter(f);

			for (String s : source) {
				bwr.write(s + "\n");
			}
			bwr.close();
		} catch (IOException e) {
			Base.logger.log(Level.SEVERE, "Could not write MutableGCodeSource to file.", e);
		}
	}
	/**
	 * This looks for and tries to remove sections that match what we expect from the start and end code
	 * It is not guaranteed to remove start and end code, just to try its best.
	 * @param source
	 * @return
	 */
	public void stripStartEndBestEffort() {
		//TODO: try harder
		return;
	}
	
	public void changeToolhead(ToolheadAlias tool) {
		
		for(String line : source)
		{
			GCodeCommand gcode = new GCodeCommand(line);
			//copy constructor
			if(gcode.hasCode('T'))
			{
				int value = (int)gcode.getCodeValue('T');
				if(value != tool.number)
				{
					if(value == 0)
						line = line.replace("T0", "T1");
					else if(value == 1)
						line = line.replace("T1", "T0");
				}
			}
			if(gcode.getCodeValue('G') == 54 && !(tool.getRecallOffsetGcodeCommand().equals("G54")))
			{
				line = line.replace("G54", tool.getRecallOffsetGcodeCommand());
			}
			if(gcode.getCodeValue('G') == 55 && !(tool.getRecallOffsetGcodeCommand().equals("G55")))
			{
				line = line.replace("G55", tool.getRecallOffsetGcodeCommand());
			}
		}
	}
	
	public MutableGCodeSource copy() {
		MutableGCodeSource newSource = new MutableGCodeSource();
		for(String line : source)
			newSource.add(new String(line));
		return newSource;
	}
}
