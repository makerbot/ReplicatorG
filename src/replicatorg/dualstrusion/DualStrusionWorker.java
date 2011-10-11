package replicatorg.dualstrusion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


/**
 * 
 * @author Noah Levy, Ben Rockhold, Will Langford
 * This class handles some of the processing of the two constituent gcodes to be combined.
 * It mostly does preprocessing work such as checking what version of SkeinForge the gcode was rendered in and stripping empty layers and whiteSpace
 *
 */
public class DualStrusionWorker {

	/**
	 * <code>endGCode</code> This object holds the end.gcode, it is either instantiated from reading a file or the primary GCodes end
	 */
	private static ArrayList<String>endGcode;
	/**
	 * <code>startGCode</code> This object holds the start.gcode, it is either instantiated from reading a file or the primary GCodes start
	 */
	private static ArrayList<String>startGcode;
	/**
	 * <code>yDanger</code> This is the maximum Y cooridnate that a makerbot can handle, checkCrashes consults this
	 */
	private static float yDanger = 80.0f;

	private static ArrayList<String>raftCode;
	/**
	 * Strips white space and carriage returns from gcode
	 * @param gcode source gcode
	 */
	public static String primaryTemp = "";
	public static String secondaryTemp = "";
	public static void stripWhitespace(ArrayList<String> gcode)
	{
		for(String s : gcode)
		{

			s = s.replaceAll("\n", "");  
			s = s.replaceAll("\r", ""); 
			s = s.trim();
		}
	}

	private static void getTemps(ArrayList<String> primary, ArrayList<String> secondary)
	{
		for(String pt : primary)
		{
			if(pt.matches("M104.*"))
			{

				primaryTemp = pt;
				System.out.println("primaryTemp" + pt);
				break; // We want the first mention of temp to avoid conflict with Skein cool
			}
		}
		for(String st : secondary)
		{
			if(st.matches("M104.*"))
			{
				secondaryTemp = st;
				break; // We want the first mention of temp to avoid conflict with Skein cool
			}
		}
	}
	/**
	 * This method is a testing main for directly accessing DualStrusion
	 * @param args
	 */
	public static void main(String[]args)
	{
		DualStrusionConstruction dsc = new DualStrusionConstruction(new File("/home/makerbot/baghandle/ergo_bag_handle_top.gcode"), new File("/home/makerbot/baghandle/ergo_bag_handle_bottom.gcode"), new File ("/home/makerbot/baghandle/ergocombine.gcode"), true, true);
		Thread th = new Thread(dsc);
		th.run();
		File result = dsc.getCombinedFile();
	}
	/**
	 * This is a method that calls all the preprocessing methods individually
	 * @param gcode
	 */
	public static void prepGcode(ArrayList<String> gcode)
	{
		stripWhitespace(gcode);
		stripEmptyLayers(gcode);
		//stripSurroundingLoop(gcode);
	}
	/**
	 * This method handles shuffling together two gcodes, it first executes preprocessing and then hands the gcodes off to Layer_Helper
	 * @param primary The primary Gcode File
	 * @param secondary The secondary Gcode File
	 * @param dest The destination Gcode file
	 * @param replaceStart A boolean determined by the user in GUI as to whether to use default start.gcode or strip it from primary gcode
	 * @param replaceEnd A boolean determined by the user in GUI as to whether to use default start.gcode or strip it from primary gcode
	 * @return A reference to the completed gcode File
	 */
	//private static wipeArrays
	public static File shuffle(File primary, File secondary, File dest, boolean replaceStart, boolean replaceEnd)
	{
		if(endGcode != null)
		{
		endGcode.clear(); //cleanse this just in case
		}
		if(startGcode != null)
		{
		startGcode.clear();
		}
		ArrayList<String> primary_lines = readFiletoArrayList(primary);
		ArrayList<String> secondary_lines = readFiletoArrayList(secondary);
		ArrayList<String> master_layer = new ArrayList<String>();
		//
		startGcode = readFiletoArrayList(new File("DualStrusion_Snippets/start.gcode"));
		endGcode = readFiletoArrayList(new File("DualStrusion_Snippets/end.gcode"));
		
		
		
		//if(checkVersion(primary_lines) &&  checkVersion(secondary_lines))
		prepGcode(primary_lines);
		prepGcode(secondary_lines);

		primary_lines = replaceToolHeadReferences(primary_lines, Toolheads.Primary);
		secondary_lines = replaceToolHeadReferences(secondary_lines, Toolheads.Secondary);
		getTemps(primary_lines, secondary_lines);
		stripStartEnd(primary_lines, replaceStart, replaceEnd);
		stripStartEnd(secondary_lines, true, true);
		//writeArrayListtoFile(primary_lines, new File("/home/makerbot/baghandle/bh1stripped.gcode"));
		//writeArrayListtoFile(secondary_lines, new File("/home/makerbot/baghandle/bh0stripped.gcode"));
		master_layer = Layer_Helper.doMerge(primary_lines, secondary_lines, false);
		
		replaceStartEnd(master_layer);
		modifyTempReferences(startGcode);
		checkCrashes(master_layer);
		writeArrayListtoFile(master_layer, dest);

		return dest;




	}
	/**
	 * This method iterates through the Gcode and replaces Toolhead indexes, preserves post T0, T1 comments through blackmagic
	 * @param gcode
	 * @param desired_toolhead an Enum representing the desired toolhead
	 * @return
	 */
	private static void modifyTempReferences(ArrayList<String> gcode)
	{
		for(int i = 0; i < gcode.size(); i++)
		{
			if(gcode.get(i).matches("M104.*T1.*"))
			{
				System.out.println("replaced " + gcode.get(i) + " with " + primaryTemp);
				gcode.set(i, primaryTemp);	
			}
			else if(gcode.get(i).matches("M104.*T0.*"))
			{
				System.out.println("replaced " + gcode.get(i) + " with " + secondaryTemp);
				gcode.set(i, secondaryTemp);
			}
		}
	}
	public static void changeToolHead(File source, int Toolhead)
	{
		ArrayList<String> changeMe = readFiletoArrayList(source);
		Toolheads t = Toolheads.Secondary;
		if(Toolhead == 0)
		{
			System.out.println("hit0");
			t = Toolheads.Secondary;
		}
		else if(Toolhead == 1)
		{
			System.out.println("hit1");

			t = Toolheads.Primary;	
		}
		changeMe = replaceToolHeadReferences(changeMe, t);
		printArrayList(new ArrayList(changeMe.subList(0,15)));
		writeArrayListtoFile(changeMe, source);
	}
	/*
	public static void stripRaft(ArrayList<String> gcode)
	{
		int length = gcode.size();
		for(int i = 0; i < length; i++)
		{
			
			if(s.equals("(<extrusion>)"))
			{
				for(int a = 0; length <)
			}
			
		}
	}
	*/
	private static ArrayList<String> replaceToolHeadReferences(ArrayList<String> gcode, Toolheads desired_toolhead)
	{
		ArrayList<String> answer = new ArrayList<String>();
		for(String s  : gcode)
		{
			if(s.matches("(M10[12345678]|M113).*")) //STOP Dont change M109
			{
				//System.out.println("!");
				int lastT0 = s.lastIndexOf("T0");
				int lastT1 = s.lastIndexOf("T1");
				String comments = "";
				int firstparens = s.indexOf("(");
				int lastparens = s.lastIndexOf(")");
				if(s.contains("("))
				{
					comments = s.substring(firstparens,lastparens+1);
				}
				if(lastT0 != -1)
				{
					s = s.substring(0, lastT0);
				}
				if(lastT1 != -1)
				{
					s = s.substring(0, lastT1);
				}
				if(lastT1 == -1 && lastT0 == -1 && !comments.equals(""))
				{
					s = s.substring(0, firstparens);
				}
				if(desired_toolhead.equals(Toolheads.Secondary))
				{
					if(s.matches(".* "))
					{
						s = s + ("T0");
					}
					else
					{
						s = s + (" T0");
					}
				}
				else if(desired_toolhead.equals(Toolheads.Primary))
				{
					if(s.matches(".* "))
					{
						s = s + ("T1");
					}
					else
					{
						s = s + (" T1");
					}
				}
				s = s + " " + comments; //blackmagic
			}
			answer.add(s);
		}
		return answer;
	}
	/**
	 * This method iterates through the gcode and checks it against <code>yDanger</code>
	 * @param gcode
	 * @return A boolean represents whether the gcode risks crashing
	 */
	public static boolean checkCrashes(ArrayList<String> gcode)
	{
		boolean crashes = false;
		for(String s : gcode)
		{
			if(checkCrash(s))
			{
				
				crashes = true;
			}
		}
		return crashes;

	}
	/**
	 * This method adds the start and end to the combined gcode, its probably named poorly.
	 * @param gcode
	 * 
	 */
	private static void replaceStartEnd(ArrayList<String> gcode)
	{

		gcode.addAll(0, startGcode);
		gcode.addAll(gcode.size(), endGcode);
		//return gcode;

	}
	/**
	 * depending on user input strips out start and end and either does or does not save them
	 * @param gcode
	 * @param replaceStart True = Dont save start False = save start
	 * @param replaceEnd True = dont save end False = save end
	 */
	private static void stripStartEnd(ArrayList<String> gcode, boolean replaceStart, boolean replaceEnd)
	{
		if(replaceStart)
		{
			stripStartGcode(gcode);
		}
		if(!replaceStart)
		{
			saveStartGcode(gcode);
		}
		if(replaceEnd)
		{
			//System.out.println("here");
			stripEndGcode(gcode);
		}
		if(!replaceEnd)
		{
			saveEndGcode(gcode);	
		}
	}
	private static void saveStartGcode(ArrayList<String> gcode)
	{
		for(int i = 0; i < gcode.size()-1; i++ ) //This starts 3/4 of the way thru to save time, tiny files may fail
		{
			//System.out.println(gcode.get(i));
			if(gcode.get(i).equalsIgnoreCase("(**** beginning of start.gcode ****)"))
			{
				System.out.println("Stripping?");
				int a = i;
				while(true)
				{
					a++;
					if(gcode.get(a).equalsIgnoreCase("(**** end of start.gcode ****)"))
					{
						startGcode.clear();
						startGcode.addAll(gcode.subList(i, a+1));
						gcode.subList(i, a+1).clear();
						return;
					}
				}
			}
		}
	}
	private static void saveEndGcode(ArrayList<String> gcode)
	{
		for(int i = (gcode.size()/4*3); i < gcode.size()-1; i++ ) //This starts 3/4 of the way thru to save time, tiny files may fail
		{
			//System.out.println(gcode.get(i));
			if(gcode.get(i).equalsIgnoreCase("(**** Beginning of end.gcode ****)"))
			{
				System.out.println("Stripping?");
				int a = i;
				while(true)
				{

					a++;
					if(gcode.get(a).equalsIgnoreCase("(**** end of end.gcode ****)"))
					{
						endGcode.clear();
						endGcode.addAll(gcode.subList(i, a+1));
						return;
					}
				}
			}
		}
	}
	private static void stripEndGcode(ArrayList<String> gcode)
	{
		for(int i = (gcode.size()/4*3); i < gcode.size()-1; i++ ) //This starts 3/4 of the way thru to save time, tiny files may fail
		{
			//System.out.println(gcode.get(i));
			if(gcode.get(i).equalsIgnoreCase("(**** Beginning of end.gcode ****)"))
			{
				System.out.println("Stripping?");
				int a = i;
				while(true)
				{
					a++;
					if(gcode.get(a).equalsIgnoreCase("(**** end of end.gcode ****)"))
					{
						gcode.subList(i, a+1).clear();
						//gcode.addAll(i, endGcode);
						return;
					}
				}
			}
		}
	}
	public static void stripStartGcode(ArrayList<String> gcode)
	{
		for(int i = 0; i < gcode.size()-1; i++ ) //This starts 3/4 of the way thru to save time, tiny files may fail
		{
			if(gcode.get(i).equalsIgnoreCase("(**** beginning of start.gcode ****)"))
			{
				System.out.println("Stripping?");
				int a = i;
				while(true)
				{
					a++;
					if(gcode.get(a).equalsIgnoreCase("(**** end of start.gcode ****)"))
					{
						gcode.subList(i, a+1).clear();
						//gcode.addAll(i, startGcode);
						return;
					}
				}
			}
		}
	}
	/**
	 * called by checkCrashes, checks individual lines
	 * @param suspectLine
	 * @return
	 */
	private static boolean checkCrash(String suspectLine)
	{
		if(suspectLine.matches("G1 .*"))
		{
			//System.out.println(suspectLine);
			String[] chunks = suspectLine.split(" ");
			for(String s : chunks)
			{
				if(s.length() > 1 && s.charAt(0) == 'Y')
				{

					float yVal = Float.parseFloat(s.substring(1,s.length())); // get the suspect y val
					if(yVal > 50)
					{
						System.out.println(yVal);
					}
					if(yVal >= yDanger)
					{
						return true; // ruh roh, Danger zone
					}
				}
			}
			return false;
		}
		return false;
	}
	private static void stripSurroundingLoop(ArrayList<String> gcode)
	{
		for(int i = 0; i < gcode.size() -2; i++)
		{
			if(gcode.get(i).equals("(<surroundingLoop>)"))
			{
				int a = i;
				while(true)
				{
					a++;
					if(gcode.get(a).equals("(</surroundingLoop>)"))
					{
						a++;
						gcode.subList(i, a).clear();
						//i = i - (a - i);
						break;
					}

				}
			}
			else if(gcode.get(i).equals("(<loop inner>)"))
			{
				int a = i;
				while(true)
				{
					a++;
					if(gcode.get(a).equals("(</loop>)"))
					{
						a++;
						gcode.subList(i, a).clear();
						//i = i - (a - i);
						break;
					}

				}
			}
		}
	}
	/**
	 * This method uses Regex to delete empty layers or layers filled only with comments
	 * @param gcode
	 */
	private static void stripEmptyLayers(ArrayList<String> gcode)
	{
		//for(int i = 0; i < gcode.size()-2;  i++)
		int max = gcode.size()-2;
		for(int i = 0; i < gcode.size()-2;   i++)
		{
			//if(gcode.get(i).matches("(<layer> .* ).*"))
			if(gcode.get(i).matches("\\(\\<layer\\>.*\\)"))
			{
				//System.out.println("checking line: " + i);
				//System.out.println(gcode.get(i) + "matches ");
				boolean containsUsefulCode = false;
				int a = i + 1;
				while(true)
				{
					if(gcode.get(a).matches("\\(\\</layer\\>\\)"))
					{
						//System.out.println(gcode.get(i) + " was not followed by useful code");
						a++;
						break;
					}
					else if(!gcode.get(a).matches("\\(\\<.*\\>\\)"))
					{
						//System.out.println(gcode.get(i) + " was followed by useful code");
						containsUsefulCode = true;
						break;
					}

					a++;
				}
				if(!containsUsefulCode)
				{

					gcode.subList(i, a).clear(); // if you are stripping stuff out, resume iterating at  a spot that reflects ur removal
					i = i - (a - i);
				}
			}
			else
			{
				//System.out.println(gcode.get(i) + " does not ");
			}
			//max = gcode.size()-2;
		}
		//return gcode;
	}
	/**
	 * This method checks what version of SkeinForge was used to create this gcode
	 * @param gcode
	 * @return A boolean of whether its an "acceptable" version of skeinforge
	 */
	private static boolean checkVersion(ArrayList<String> gcode)
	{
		boolean compliantSkein = false;
		boolean compliantVer = false;
		String version = "10.11.05";
		for(String s : gcode)
		{
			if(s.matches("\\(\\<creation\\>.*\\<\\/creation\\>\\)"))
			{
				System.out.println(s);
				if(s.contains("skeinforge"))
				{
					compliantSkein = true;
				}
			}
			if(s.matches("\\(\\<version\\>.*\\<\\/version\\>\\)"))
			{			
				System.out.println(s);

				if(s.contains(version))
				{
					compliantVer = true;
				}
			}
			if(compliantSkein && compliantVer)
			{
				return true; //implicit break saves time
			}
		}
		return false;
	}
	/**
	 * This method is used to write finished combinedGcode to a file
	 * @param t writeThis arrayList
	 * @param f to this Destination
	 */
	public static void writeArrayListtoFile(ArrayList<String> t, File f)
	{
		try{
			FileWriter bwr = new FileWriter(f);

			for(String s : t)
			{
				//System.out.println(s);
				bwr.write(s + "\n");
			}
			bwr.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * This is a debugging method for printing arrayLists of gcode, not invoked currently
	 * @param s arrayList to print
	 */
	public static void printArrayList(ArrayList<String> s)
	{
		for(String t : s)
		{
			System.out.println(t);
		}
	}
	/**
	 * This method is used to read in files
	 * @param f
	 * @return
	 */
	public static ArrayList<String> readFiletoArrayList(File f)
	{
		ArrayList<String> vect = new ArrayList<String>();
		String curline;	
		try
		{
			BufferedReader bir = new BufferedReader(new FileReader(f));
			curline = bir.readLine();
			while(curline != null)
			{
				vect.add(curline);
				curline = bir.readLine();
			}

			System.out.println("ArrayList production was successful");
			bir.close();
		}
		catch(IOException e)
		{

			System.err.println("couldnt read file " + f.getAbsolutePath());

		}


		return vect;
	}

}
