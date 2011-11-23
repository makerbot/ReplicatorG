package replicatorg.dualstrusion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import replicatorg.app.Base;

/**
 * 
 * @author Noah Levy, Ben Rockhold, Will Langford
 * @maintainer Far McKon (far@makerbot.com) Worker class to manage and contain
 *             processing to create dual extrusion files, or single extrusion
 *             files to run on a dual extrusion machine.
 * 
 * 
 */
public class DualStrusionWorker {

	/**
	 * <code>yDanger</code> When 'wipes' are added, space must be reserved at
	 * the back of the machine for wipe action. yDanger indicates the start of
	 * that 'wipe zone' in the rear of the machine, which must be kept clear of
	 * build materials
	 */
	private static float yDanger = 80.0f;

	/**
	 * Strips endlines and trailing whitespace from each string in the list
	 * 
	 * @param gcode
	 *            strings to strip, in place
	 */
	public static void stripEndlines(ArrayList<String> gcode) {
		for (String s : gcode) {
			s = s.replaceAll("\n", "");
			s = s.replaceAll("\r", "");
			s = s.trim();
		}
	}

	/**
	 * Function returns the comment section of a gcode command.
	 * 
	 * @param line
	 *            a line of gcode, may include a parens delimited comment
	 * @param prefixSpace
	 *            a boolean, to indicate to return a prefix ' ' in front of the
	 *            found comment
	 * @return a comment string (parens included) if a comment is found, or an
	 *         empty string if no comment was found TODO: move into GCode.java
	 */
	public static String pullComment(String line, boolean prefixSpace) {
		Pattern commentMatch = Pattern.compile("(.*?)(\\(.*\\))");
		Matcher m = commentMatch.matcher(line);
		if (m.matches()) {
			if (prefixSpace)
				return " " + m.group(2);
			return m.group(2);
		}
		return "";

	}

	/**
	 * Searches @gcode for the first M104 command line (temperature control)
	 * returns it.
	 * 
	 * @param gcode
	 *            list of gcode to search
	 * @returns the whole first line starting with M104, or an empty string
	 *          otherwise
	 */
	private static String getTemperatureCommand(ArrayList<String> gcode) {
		String toolheadCmd = "M104.*";
		for (String line : gcode) {
			if (line.matches(toolheadCmd)) {
				return line;
			}
		}
		return "";
	}

	/**
	 * This is a method that calls all the preprocessing methods individually
	 * 
	 * @param gcode
	 */
	public static void preprocessForCombine(ArrayList<String> gcode) {
		stripEndlines(gcode);
		stripEmptyLayers(gcode);
	}

	/**
	 * This method handles shuffling together two gcodes, it first executes
	 * preprocessing and then hands the gcodes off to Layer_Helper
	 * 
	 * @param primary
	 *            The primary Gcode File
	 * @param secondary
	 *            The secondary Gcode File
	 * @param dest
	 *            The destination Gcode file
	 * @param replaceStart
	 *            A boolean determined by the user in GUI as to whether to use
	 *            default start.gcode or strip it from primary gcode
	 * @param replaceEnd
	 *            A boolean determined by the user in GUI as to whether to use
	 *            default start.gcode or strip it from primary gcode
	 * @param uW
	 * @return A reference to the completed gcode File
	 */
	// private static wipeArrays
	public static File combineGcode(File primary, File secondary, File dest,
			boolean replaceStart, boolean replaceEnd, boolean useWipes) {

		ArrayList<String> primaryGcode = readFiletoArrayList(primary);
		ArrayList<String> secondaryGcode = readFiletoArrayList(secondary);
		ArrayList<String> master_layer = new ArrayList<String>();

		ArrayList<String> startGcode = readFiletoArrayList(new File(
				"DualStrusion_Snippets/start.gcode"));
		ArrayList<String> endGcode = readFiletoArrayList(new File(
				"DualStrusion_Snippets/end.gcode"));

		// get our temp commands on a per-file basis
		String primaryTemp = getTemperatureCommand(primaryGcode);
		String secondaryTemp = getTemperatureCommand(secondaryGcode);

		// TRICKY: since both files are 'default toolhead' (t1) swap primaryTemp
		// to be 'first toolhead' (t0)
		primaryTemp = swapToolhead(primaryTemp, Toolheads.PRIMARY.getTid());

		if (replaceStart) {
			stripStartGcode(primaryGcode);
		} else {
			startGcode = saveStartGcode(primaryGcode); // startGcode comes from
														// prior file
		}

		if (replaceEnd == false)
			endGcode = saveEndGcode(primaryGcode);

		// on combine, always strip start/end of secondaryGcode
		stripStartGcode(secondaryGcode);
		stripEndGcode(secondaryGcode);

		primaryGcode = replaceToolHeadReferences(primaryGcode,
				Toolheads.PRIMARY);
		secondaryGcode = replaceToolHeadReferences(secondaryGcode,
				Toolheads.SECONDARY);

		// interlace the layers for each toolhead
		LayerHelper helper = new LayerHelper(primaryGcode, secondaryGcode,
				false, useWipes);
		master_layer = helper.mergeLayers();

		//modifyTempReferences(startGcode, primaryTemp, secondaryTemp);
		modifyTempReference(startGcode, Toolheads.PRIMARY, primaryTemp);
		modifyTempReference(startGcode, Toolheads.SECONDARY, secondaryTemp);

		// add start and end gcode
		master_layer.addAll(0, startGcode);
		master_layer.addAll(master_layer.size(), endGcode);

		mayHaveWipeCrash(master_layer);
		writeArrayListtoFile(master_layer, dest);

		return dest;

	}
	
	private static void modifyTempReference(ArrayList<String> gcode, Toolheads toolhead, String newTempCmd)
	{
		String matchTarget = "M104.*" + toolhead.getTid() + ".*";

		for (int i = 0; i < gcode.size(); i++) {
			if (gcode.get(i).matches(matchTarget)) {
				gcode.set(i, newTempCmd);
			}
		}
	}

//	/**
//	 * This method iterates through the Gcode and replaces Toolhead indexes,
//	 * preserves post T0, T1 comments through blackmagic
//	 * 
//	 * @param gcode
//	 * @return
//	 */
//	private static void modifyTempReferences(ArrayList<String> gcode, String primaryTemp, String secondaryTemp) {
//
//		for (int i = 0; i < gcode.size(); i++) {
//			if (gcode.get(i).matches("M104.*T1.*")) {
//				System.out.println("** M104 update T1**");
//				System.out.println(gcode.get(i));
//				System.out.println(primaryTemp);
//
//				gcode.set(i, primaryTemp);
//			} else if (gcode.get(i).matches("M104.*T0.*")) {
//				System.out.println("** M104 update T0**");
//				System.out.println(gcode.get(i));
//				System.out.println(secondaryTemp);
//				gcode.set(i, secondaryTemp);
//			}
//		}
//	}

	/**
	 * Takes gcode, and changes all toolhead references(T0 -> T1 or similar)
	 * based on user input.
	 * 
	 * @param source
	 *            gcode to alter the toolhead info on
	 * @param Toolhead
	 *            string name of the toolhead, 'right' or 'left'
	 */
	public static void changeToolHead(File source, String Toolhead) {
		ArrayList<String> startGcode = readFiletoArrayList(new File(
				"DualStrusion_Snippets/start.gcode"));
		ArrayList<String> endGcode = readFiletoArrayList(new File(
				"DualStrusion_Snippets/end.gcode"));
		ArrayList<String> gcode = readFiletoArrayList(source);

		Toolheads t = Toolheads.SECONDARY;

		if (Toolhead.equalsIgnoreCase("right"))
			t = Toolheads.SECONDARY;
		else if (Toolhead.equalsIgnoreCase("left"))
			t = Toolheads.PRIMARY;

		// remove old start/end gcode
		stripStartGcode(gcode);
		stripEndGcode(gcode);

		// change reference to toolheads before adding start gcode
		gcode = replaceToolHeadReferences(gcode, t);

		// insert new start/end gcode stanzas
		gcode.addAll(0, startGcode);
		gcode.addAll(endGcode);

		// change offset recall after adding gcode
		changeOffsetRecall(gcode, t);

		writeArrayListtoFile(gcode, source);
	}

	/**
	 * Converts the 'offset recall' to the proper Gcode command based on the
	 * selected toolhead. For dualstrusion code this is not needed but may be
	 * done, for single head extrusion on dualstrusion machines this is needed.
	 * 
	 * @param gcode
	 *            list of gcode commands
	 * @param toolhead
	 *            target toolhead for this gcode
	 */
	public static void changeOffsetRecall(ArrayList<String> gcode,
			Toolheads toolhead) {
		// changeOffsetRecall(gcode, t);
		for (int i = 0; i < gcode.size(); i++) {
			if (gcode.get(i).matches("G54.*")) 
			{
				// G54 is the default template value for 'recall toolhead offset'
				//System.out.println("G54 found");
				String comment = pullComment(gcode.get(i), true);
				gcode.set(i, toolhead.getRecallOffsetGcodeCommand() + comment);
			}
		}
	}

	/**
	 * Append or Replace all toolhead info for Move commands with the new target
	 * toolhead
	 * 
	 * @param s
	 *            command string
	 * @param targetToolhead
	 *            new toolhead string
	 * @return an updated string with a new target toolhead
	 */
	private static String swapToolhead(String command, String targetToolhead) {
		int lastT0 = command.lastIndexOf("T0");
		int lastT1 = command.lastIndexOf("T1");
		String comments = pullComment(command, true);

		// strip off the core command, all text before T0/T1/ or '('
		if (lastT0 != -1) {
			command = command.substring(0, lastT0);
		} else if (lastT1 != -1) {
			command = command.substring(0, lastT1);
		} else if (lastT1 == -1 && lastT0 == -1 && !comments.equals("")) {
			command = command.substring(0, command.indexOf("("));

		}

		if (!command.endsWith(" "))
			command = command + " ";

		command = command + targetToolhead + comments;
		return command;
	}

	/**
	 * Scans a gcode file, and changes all references to toolheads to the
	 * specified toolhead.
	 * 
	 * @param gcode
	 * @param desired_toolhead
	 * @return
	 */
	private static ArrayList<String> replaceToolHeadReferences(
			ArrayList<String> gcode, Toolheads desiredToolhead) {
		ArrayList<String> newGcode = new ArrayList<String>();
		String newToolhead = desiredToolhead.getTid();

		for (String s : gcode) {

			if (s.matches("(M10[12345678]|M113).*")) // Don't change M109,
														// heated build platform
														// cmds.
			{
				// TODO: we may be able to process M109 commands now. Test that
				s = swapToolhead(s, newToolhead);
			}
			newGcode.add(s);

		}
		return newGcode;
	}

	private static ArrayList<String> saveStartGcode(ArrayList<String> gcode) {
		ArrayList<String> startGcode = new ArrayList<String>();
		int start = -1;
		int end = -1;
		for (int i = 0; i < gcode.size() - 1; i++) {
			if (start == -1
					&& gcode.get(i).equalsIgnoreCase(
							"(**** beginning of start.gcode ****)")) {
				start = i;
				Base.logger.finer("Saving start gcode");
			} else if (gcode.get(i).equalsIgnoreCase(
					"(**** end of start.gcode ****)")) {
				end = i;
				startGcode.addAll(gcode.subList(end, start));
				gcode.subList(end, start).clear();
			}
		}
		return startGcode;
	}

	private static ArrayList<String> saveEndGcode(ArrayList<String> gcode) {
		// ToDo: refactor
		// This starts 3/4 of the way through to save time, tiny files may fail
		ArrayList<String> endGcode = new ArrayList<String>();
		for (int i = (gcode.size() / 4 * 3); i < gcode.size() - 1; i++) 
		{
			// System.out.println(gcode.get(i));
			if (gcode.get(i).equalsIgnoreCase(
					"(**** Beginning of end.gcode ****)")) {
				Base.logger.finer("saving end gcode");
				int a = i;
				while (true) {

					a++;
					if (gcode.get(a).equalsIgnoreCase(
							"(**** end of end.gcode ****)")) {
						endGcode.addAll(gcode.subList(i, a + 1));
					}
				}
			}
		}
		return endGcode;
	}

	private static void stripEndGcode(ArrayList<String> gcode) {
		// ToDo: refactor
		// This starts 3/4 of the way thru to save time,
		// tiny files may fail
		for (int i = (gcode.size() / 4 * 3); i < gcode.size() - 1; i++) 
		{
			// System.out.println(gcode.get(i));
			if (gcode.get(i).equalsIgnoreCase(
					"(**** Beginning of end.gcode ****)")) {
				Base.logger.finer("Stripping end gcode");
				int a = i;
				while (true) {
					a++;
					if (gcode.get(a).equalsIgnoreCase(
							"(**** end of end.gcode ****)")) {
						gcode.subList(i, a + 1).clear();
						return;
					}
				}
			}
		}
	}

	public static void stripStartGcode(ArrayList<String> gcode) {
		// ToDo: refactor
		for (int i = 0; i < gcode.size() - 1; i++) {
			if (gcode.get(i).equalsIgnoreCase(
					"(**** beginning of start.gcode ****)")) {
				//System.out.println("stripping start gcode?");
				int a = i;
				while (true) {
					a++;
					if (gcode.get(a).equalsIgnoreCase(
							"(**** end of start.gcode ****)")) {
						gcode.subList(i, a + 1).clear();
						// gcode.addAll(i, startGcode);
						return;
					}
				}
			}
		}
	}

	/**
	 * This method iterates through the gcode and checks it against
	 * <code>yDanger</code>
	 * 
	 * @param gcode
	 *            code to test for potential crashes
	 * @return true if a wipe crash may happen due to gcode moving into the wipe
	 *         zone
	 */
	public static boolean mayHaveWipeCrash(ArrayList<String> gcode) {
		for (String command : gcode) {
			if (command.matches("G1 .*")) {
				if (outOfBoundsY(command, yDanger) == true) {
					// ruh roh, Danger zone. Toolhead too close to the back?
					Base.logger
							.warning("command may go beyond allowed Y distance");
					Base.logger.warning("Line: " + command);
					Base.logger.warning("Y boundary: " + yDanger);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if line contains a y move beyond the max y boundary
	 * 
	 * @param gcode
	 *            command to test
	 * @param yBoundary
	 *            max y to test for
	 * @return true of the command exceeds yBoundar
	 */
	private static boolean outOfBoundsY(String suspectLine, double yBoundry) {
		if (suspectLine.matches("G1 .*")) {
			String[] chunks = suspectLine.split(" ");
			for (String s : chunks) {
				if (s.length() > 1 && s.charAt(0) == 'Y') {
					/// gets the suspect y value
					float yVal = Float.parseFloat(s.substring(1, s.length()));
					if (yVal >= yBoundry) {
						return true;
					}
				}
			}
			return false;
		}
		return false;
	}

	private static void stripSurroundingLoop(ArrayList<String> gcode) {
		// ToDo: refactor
		for (int i = 0; i < gcode.size() - 2; i++) {
			if (gcode.get(i).equals("(<surroundingLoop>)")) {
				int a = i;
				while (true) {
					a++;
					if (gcode.get(a).equals("(</surroundingLoop>)")) {
						a++;
						gcode.subList(i, a).clear();
						// i = i - (a - i);
						break;
					}

				}
			} else if (gcode.get(i).equals("(<loop inner>)")) {
				int a = i;
				while (true) {
					a++;
					if (gcode.get(a).equals("(</loop>)")) {
						a++;
						gcode.subList(i, a).clear();
						// i = i - (a - i);
						break;
					}

				}
			}
		}
	}

	/**
	 * This method uses Regex to delete empty layers or layers filled only with
	 * comments
	 * 
	 * @param gcode
	 *            list of gcode to modify in place
	 */
	private static void stripEmptyLayers(ArrayList<String> gcode) {
		int max = gcode.size() - 2;
		for (int i = 0; i < gcode.size() - 2; i++) {
			if (gcode.get(i).matches("\\(\\<layer\\>.*\\)")) {
				boolean containsUsefulCode = false;
				int a = i + 1;
				while (true) {
					if (gcode.get(a).matches("\\(\\</layer\\>\\)")) {
						a++;
						break;
					} else if (!gcode.get(a).matches("\\(\\<.*\\>\\)")) {
						containsUsefulCode = true;
						break;
					}

					a++;
				}
				if (!containsUsefulCode) {
					// if you are stripping stuff out, resume iterating at
					// a spot that reflects ur removal
					gcode.subList(i, a).clear(); 
					i = i - (a - i);
				}
			} else {
				// System.out.println(gcode.get(i) + " does not ");
			}
		}
	}

	/**
	 * This method checks what version of SkeinForge was used to create this
	 * gcode
	 * 
	 * @param gcode
	 * @return A boolean of whether its an "acceptable" version of skeinforge
	 */
	private static boolean checkVersion(ArrayList<String> gcode) {
		boolean compliantSkein = false;
		boolean compliantVer = false;
		String version = "10.11.05";
		for (String s : gcode) {
			if (s.matches("\\(\\<creation\\>.*\\<\\/creation\\>\\)")) {
				//System.out.println(s);
				if (s.contains("skeinforge")) {
					compliantSkein = true;
				}
			}
			if (s.matches("\\(\\<version\\>.*\\<\\/version\\>\\)")) {
				System.out.println(s);

				if (s.contains(version)) {
					compliantVer = true;
				}
			}
			if (compliantSkein && compliantVer) {
				return true; // implicit break saves time
			}
		}
		return false;
	}

	/**
	 * This method is used to write finished combinedGcode to a file
	 * 
	 * @param t
	 *            writeThis arrayList
	 * @param f
	 *            to this Destination
	 */
	public static void writeArrayListtoFile(ArrayList<String> t, File f) {
		try {
			FileWriter bwr = new FileWriter(f);

			for (String s : t) {
				// System.out.println(s);
				bwr.write(s + "\n");
			}
			bwr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads a file f into an ArrayList to use for combination later
	 * 
	 * @param f
	 * @return an ArrayList<String> with one line per array item
	 */
	public static ArrayList<String> readFiletoArrayList(File f) {
		ArrayList<String> vect = new ArrayList<String>();
		String curline;
		try {
			BufferedReader bir = new BufferedReader(new FileReader(f));
			curline = bir.readLine();
			while (curline != null) {
				vect.add(curline);
				curline = bir.readLine();
			}

			//System.out.println("ArrayList production was successful");
			bir.close();
		} catch (IOException e) {
			System.err.println("couldnt read file " + f.getAbsolutePath());
		}
		return vect;
	}

}
