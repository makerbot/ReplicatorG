package replicatorg.app.gcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import replicatorg.app.Base;
import replicatorg.machine.model.ToolheadAlias;

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
	
	private static void modifyTempReference(ArrayList<String> gcode, ToolheadAlias toolhead, String newTempCmd)
	{
		String matchTarget = "M104.*" + toolhead.getTcode() + ".*";

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
			ToolheadAlias toolhead) {
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
			ArrayList<String> gcode, ToolheadAlias desiredToolhead) {
		ArrayList<String> newGcode = new ArrayList<String>();
		String newToolhead = desiredToolhead.getTcode();

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

}
