package replicatorg.dualstrusion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayList;


public class DualStrusionWorker implements Runnable{

	private static ArrayList<String>endGcode;
	private static ArrayList<String>startGcode;
	private static float yDanger = 80.0f;
	private static ArrayList<String> MasterLayer;

	public static void stripWhitespace(ArrayList<String> gcode)
	{
		for(String s : gcode)
		{

			s = s.replaceAll("\n", "");  
			s = s.replaceAll("\r", ""); 
			s = s.trim();
		}
	}
	
	public static void main(String[]args)
	{
		DualStrusionConstruction dsc = new DualStrusionConstruction(new File("/home/makerbot/baghandle/ergo_bag_handle_top.gcode"), new File("/home/makerbot/baghandle/ergo_bag_handle_bottom.gcode"), new File ("/home/makerbot/baghandle/ergocombine.gcode"), true, true);
		Thread th = new Thread(dsc);
		th.run();
		File result = dsc.getCombinedFile();
	}
	private static void prepGcode(ArrayList<String> gcode)
	{
		stripWhitespace(gcode);
		stripEmptyLayers(gcode);
		//stripSurroundingLoop(gcode);
	}
	public static File shuffle(File primary, File secondary, File dest, boolean replaceStart, boolean replaceEnd)
	{
		ArrayList<String> primary_lines = readFiletoArrayList(primary);
		ArrayList<String> secondary_lines = readFiletoArrayList(secondary);
		ArrayList<String> master_layer = new ArrayList<String>();
		//
		startGcode = readFiletoArrayList(new File("skein_engines/skeinforge-35/skeinforge_application/prefs/SF35-Thingomatic-HBP-Stepstruder-1.75mm-DUAL/alterations/start.gcode"));
		endGcode = readFiletoArrayList(new File("skein_engines/skeinforge-35/skeinforge_application/prefs/SF35-Thingomatic-HBP-Stepstruder-1.75mm-DUAL/alterations/end.gcode"));
		
		stripStartEnd(primary_lines, replaceStart, replaceEnd);
		stripStartEnd(secondary_lines, true, true);

		//if(checkVersion(primary_lines) &&  checkVersion(secondary_lines))
		prepGcode(primary_lines);
		prepGcode(secondary_lines);
	
		primary_lines = replaceToolHeadReferences(primary_lines, Toolheads.Primary);
		secondary_lines = replaceToolHeadReferences(secondary_lines, Toolheads.Secondary);
		//writeArrayListtoFile(primary_lines, new File("/home/makerbot/baghandle/bh1stripped.gcode"));
		//writeArrayListtoFile(secondary_lines, new File("/home/makerbot/baghandle/bh0stripped.gcode"));
		checkCrashes(primary_lines);
		checkCrashes(secondary_lines);
		
		writeArrayListtoFile(replaceStartEnd(Layer_Helper.doMerge(primary_lines, secondary_lines)), dest);
		return dest;




	}


	private static ArrayList<String> replaceToolHeadReferences(ArrayList<String> gcode, Toolheads desired_toolhead)
	{
		ArrayList<String> answer = new ArrayList<String>();
		for(String s  : gcode)
		{
			if(s.matches("(M10[123456789]|M113).*"))
			{
				//System.out.println("!");
				int lastT0 = s.lastIndexOf("T0");
				int lastT1 = s.lastIndexOf("T1");
				String comments = "";
				if(s.contains("("))
				{
					int firstparens = s.indexOf("(");
					int lastparens = s.lastIndexOf(")");
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
				if(desired_toolhead.equals(Toolheads.Secondary))
				{
					s = s + (" T0"); //This works under the assumption that one space is not different from two spaces
				}
				else if(desired_toolhead.equals(Toolheads.Primary))
				{
					s = s + (" T1");
				}
				s = s + " " + comments;
			}
			answer.add(s);
		}
		return answer;
	}
	private static boolean checkCrashes(ArrayList<String> gcode)
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
	private static ArrayList<String> replaceStartEnd(ArrayList<String> gcode)
	{
	
			gcode.addAll(0, startGcode);
			gcode.addAll(gcode.size(), endGcode);
			return gcode;
		
	}
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
	private static void stripStartGcode(ArrayList<String> gcode)
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
	private static void stripEmptyLayers(ArrayList<String> gcode)
	{
		//for(int i = 0; i < gcode.size()-2;  i++)
		for(int i = 0; i < gcode.size()-2;  i++)
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
		}
		//return gcode;
	}
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
	private static void writeArrayListtoFile(ArrayList<String> t, File f)
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
	public static void printArrayList(ArrayList<String> s)
	{
		for(String t : s)
		{
			System.out.println(t);
		}
	}
	private static ArrayList<String> readFiletoArrayList(File f)
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
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
