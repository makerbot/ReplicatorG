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
import java.util.Vector;
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
		
		// T only needs to be called once in a file, so we find the first,
		// make sure it's correct, and then remove every T after it
		// (unless it's a comment, we can skip comments)
		boolean foundT = false;
		// same goes for G54&55
		boolean foundG = false;
		
		for(String line : source)
		{
			GCodeCommand g = new GCodeCommand(line);

			// replace the first toolhead call with the correct one
			// removes every following toolhead call
			if(!foundT)
			{
//				if(g.removeCode('T') != null)
				if(g.hasCode('T'))
				{
					foundT = true;
//					g.addCode('T', tool.number);

					// replace T* with the correct T code
					line = line.replace("T0", tool.getTcode());
					line = line.replace("T1", tool.getTcode());
				}
			}
			else
			{
//				g.removeCode('T');
				line = line.replace("T0", "");
				line = line.replace("T1", "");
			}
			
			// replaces the first G54/55 with the correct one
			// removes every following G54/55
			if(!foundG)
			{
				if(g.getCodeValue('G') == 55)
				{
					foundG = true;
					if(tool == ToolheadAlias.LEFT)
					{
//						g.removeCode('G');
//						g.addCode('G', 54);
						line = line.replace("G55", "G54");
					}
				}
				else if(g.getCodeValue('G') == 54)
				{
					foundG = true;
					if(tool == ToolheadAlias.RIGHT)
					{
						line = line.replace("G54", "G55");
					}
				}
			}
			else
			{
				if (g.getCodeValue('G') == 55 || g.getCodeValue('G') == 54)
				{
//					g.removeCode('G');
					line = line.replace("G54", "");
					line = line.replace("G55", "");
				}
			}
			
		}

	}
}
