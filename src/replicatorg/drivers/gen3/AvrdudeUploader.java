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

package replicatorg.drivers.gen3;

import java.io.File;
import java.util.Vector;

import replicatorg.app.Base;


public class AvrdudeUploader {
  public AvrdudeUploader() {
  }

  protected boolean uploadViaBootloader(String firmwarePath, String portName, int speed, String arch) {
    Vector<String> commandDownloader = new Vector<String>();
    final String protocol = "stk500v1";
    String avrBasePath = Base.getAppPath() + File.separator + "tools";
    commandDownloader.add(avrBasePath + File.separator + "avrdude");
    commandDownloader.add("-C" + " avrdude.conf");
    commandDownloader.add("-c" + protocol);
    commandDownloader.add("-P" + (Base.isWindows() ? "\\\\.\\" : "") + portName);
    commandDownloader.add("-b" + Integer.toString(speed));
    commandDownloader.add("-D"); // don't erase
    commandDownloader.add("-Uflash:w:" + firmwarePath + ".hex:i");

    if (Base.preferences.getBoolean("upload.verbose",false)) {
        commandDownloader.add("-v");
        commandDownloader.add("-v");
        commandDownloader.add("-v");
        commandDownloader.add("-v");
      } else {
        //commandDownloader.add("-q");
        //commandDownloader.add("-q");
      }

    commandDownloader.add("-p" + arch); 

    int result = -1;
    try {
      String[] commandArray = new String[commandDownloader.size()];
      commandDownloader.toArray(commandArray);
      
      
      if (Base.preferences.getBoolean("upload.verbose",false)) {
        for(int i = 0; i < commandArray.length; i++) {
          System.out.print(commandArray[i] + " ");
        }
        System.out.println();
      }
      Process process = Runtime.getRuntime().exec(commandArray);
      //new MessageSiphon(process.getInputStream(), this);
      //new MessageSiphon(process.getErrorStream(), this);

      // wait for the process to finish.  if interrupted
      // before waitFor returns, continue waiting
      //
      boolean uploading = true;
      result = 0;
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
