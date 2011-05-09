/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  AvrdudeUploader - uploader implementation using avrdude
  Part of the Arduino project - http://www.arduino.cc/

  Copyright (c) 2004-05
  Hernando Barragan

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
  
  $Id: AvrdudeUploader.java 609 2009-06-01 19:27:21Z dmellis $
*/

package replicatorg.uploader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import replicatorg.app.Base;
import replicatorg.app.util.serial.Serial;


public class AvrdudeUploader extends AbstractFirmwareUploader {
	String protocol = "stk500v1";

	boolean manualReset = false;
	
	String eepromPath = null;
	
	String uploadInstructions = null;
	public void setEeprom(String path) {
		eepromPath = path;
	}
	
	public String getEeprom() {
		return eepromPath;
	}
	
	public String getUploadInstructions() {
		if (uploadInstructions != null) {
			return uploadInstructions;
		}
		if (manualReset == true) {
			return "Press the reset button on the target board and click the \"Upload\" button " +
			"to update the firmware.  Try to press the reset button as soon as you click \"Upload\".";
		}
		return super.getUploadInstructions();
	}

	public void setManualreset(String val) {
		if (val == null) return;
		Base.logger.fine("Manual reset = "+val);
		if ("true".equalsIgnoreCase(val)) { manualReset = true; }
	}
	
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	
  public AvrdudeUploader() {
  }

  class StreamDumper extends Thread {
	  InputStream in;
	  OutputStream out;
	  public StreamDumper(InputStream in, OutputStream out) {
		  super("AVR uploader Stream Dumper");
		  this.in = in; this.out = out;
	  }
	  public void run() {
		  byte[] buffer = new byte[1024];
		  while (true) {
			  int cnt;
			  try {
				  cnt = in.read(buffer);
				  if (cnt == -1) break;
				  if (cnt > 0) out.write(buffer,0,cnt);
			  } catch (IOException e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  }
		  }
	  }
  }
  
  public boolean upload() {
    Vector<String> commandDownloader = new Vector<String>();
    String avrBasePath = Base.getToolsPath();
    
    commandDownloader.add(avrBasePath + File.separator + "avrdude");
    commandDownloader.add("-C" + avrBasePath + File.separator + "avrdude.conf");
    commandDownloader.add("-c" + protocol);
    commandDownloader.add("-P" + (Base.isWindows() ? "\\\\.\\" : "") + serialName);
    commandDownloader.add("-b" + Integer.toString(serialSpeed));
	commandDownloader.add("-D"); // don't erase
    if (eepromPath != null) {
    	commandDownloader.add("-Ueeprom:w:"+eepromPath+":i"); // erase
    }
    commandDownloader.add("-Uflash:w:" + source + ":i");

    if (Base.preferences.getBoolean("upload.verbose",false)) {
        commandDownloader.add("-v");
        commandDownloader.add("-v");
        commandDownloader.add("-v");
        commandDownloader.add("-v");
      } else {
        //commandDownloader.add("-q");
        //commandDownloader.add("-q");
      }

    commandDownloader.add("-p" + architecture); 

    int result = -1;
    try {
      String[] commandArray = new String[commandDownloader.size()];
      commandDownloader.toArray(commandArray);
      
      
      if (Base.preferences.getBoolean("upload.verbose",true)) {
        for(int i = 0; i < commandArray.length; i++) {
          System.out.print(commandArray[i] + " ");
        }
        System.out.println();
      }

      // Hit the reset line
      
      Serial serialPort = new Serial(serialName);
      serialPort.pulseRTSLow();
      serialPort.dispose();

      Process process = Runtime.getRuntime().exec(commandArray);
      //new MessageSiphon(process.getInputStream(), this);
      //new MessageSiphon(process.getErrorStream(), this);

      // wait for the process to finish.  if interrupted
      // before waitFor returns, continue waiting
      //
      boolean uploading = true;
      result = 0;
      new StreamDumper(process.getInputStream(),System.out).start();
      new StreamDumper(process.getErrorStream(),System.err).start();
      while (uploading) {
        try {
        	result = process.waitFor();
        	uploading = false;
        } catch (InterruptedException intExc) {
        }
      }
      if(result!=0)
    	  return false;
    } catch (Exception e) {
      e.printStackTrace();
      result = -1;
    }
    return (result == 0); // ? true : false;      
  }
}
