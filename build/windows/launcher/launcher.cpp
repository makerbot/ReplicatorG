// -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-

// launcher.cpp : Defines the class behaviors for the application.
//

// I have no idea what kind of staggering mental challenges the original
// authors of this file had to overcome, but I've gutted it.
//

// TODO: The memory size should be set in a config file or something
// similar.  Hardcoding is bad mkay?
// Size in megs.
#define JAVA_MEMORY_SIZE 256
#define JAVA_MAIN_CLASS "replicatorg.app.Base"

#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <sstream>
#include <vector>

using namespace std;

int STDCALL
WinMain (HINSTANCE hInst, HINSTANCE hPrev, LPSTR lpCmd, int nShow)
{
  // Parameters to be passed to the program.
  string incomingParams(lpCmd);

  boolean console = false;
  int memInMegs = JAVA_MEMORY_SIZE;
  // Check for --console and --mem parameters
  if (incomingParams.find("--console") != string::npos) {
    console = true;
  }
  string::size_type memloc = incomingParams.find("--mem");
  if (memloc != string::npos) {
    // parse out mem size (--mem=512, --mem 512 should both work)
    memloc += 5;
    memInMegs = 0;
    if (incomingParams[memloc] == ' ' ||
        incomingParams[memloc] == '=') memloc++;
    while (memloc < incomingParams.size()) {
      char c = incomingParams[memloc++];
      if (c < '0' || c > '9') break;
      memInMegs *= 10;
      memInMegs += c-'0';
    }
  }

  // append the classpath and launcher.Application
  char loadDir[MAX_PATH];
  loadDir[0] = '\0';
  GetModuleFileName(NULL, loadDir, MAX_PATH);
  // remove the application name
  char* lastBackslash = strrchr(loadDir, '\\');
  if (lastBackslash != NULL) {
    *lastBackslash = '\0';
  }

  // Enumerate the jars used by the application
  // TODO: We really ought to be loading these dynamically by
  // scanning the lib directory.
  vector<const char*> libs;
  libs.push_back("ReplicatorG.jar");
  libs.push_back("mrj.jar");
  libs.push_back("vecmath.jar");
  libs.push_back("j3dcore.jar");
  libs.push_back("j3dutils.jar");
  libs.push_back("RXTXcomm.jar");
  libs.push_back("oro.jar");
  libs.push_back("registry.jar");
  libs.push_back("antlr.jar");
  libs.push_back("miglayout-3.7.jar");
  libs.push_back("jfreechart-1.0.13.jar");
  libs.push_back("jcommon-1.0.16.jar");

  // Construct the classpath from the jar list
  stringstream cp;
  cp << loadDir << "\\lib;";
  cp << loadDir << "\\lib\\build;";
  for (vector<const char*>::iterator i = libs.begin(); i != libs.end(); i++) {
    cp << loadDir << "\\lib\\" << *i << ";";
  }

  // Set the environment's classpath to the one we constructed.
  if (!SetEnvironmentVariable("CLASSPATH", cp.str().c_str())) {
    MessageBox(NULL, "Could not set CLASSPATH environment variable",
               "ReplicatorG Error", MB_OK);
    return 0;
  }
  
  // what gets put together to pass to jre
  stringstream outgoing;

  // prepend the args for -mx and -ms
  outgoing << "-Xms" << memInMegs << "m ";
  outgoing << "-Xmx" << memInMegs << "m ";
  // add the name of the class to execute and a space before the next arg
  outgoing << JAVA_MAIN_CLASS << " ";
  // append additional command line parameters
  outgoing << incomingParams;


  const char* executable = console?"java.exe":"javaw.exe";

  SHELLEXECUTEINFO ShExecInfo;

  // set up the execution info
  ShExecInfo.cbSize = sizeof(SHELLEXECUTEINFO);
  ShExecInfo.fMask = 0;
  ShExecInfo.hwnd = 0;
  ShExecInfo.lpVerb = "open";
  ShExecInfo.lpFile = executable;
  ShExecInfo.lpParameters = outgoing.str().c_str();
  ShExecInfo.lpDirectory = loadDir;
  ShExecInfo.nShow = SW_SHOWNORMAL;
  ShExecInfo.hInstApp = NULL;

  if (!ShellExecuteEx(&ShExecInfo)) {
    MessageBox(NULL, "Error calling ShellExecuteEx()", 
               "ReplicatorG Error", MB_OK);
    return 0;
  }

  if (reinterpret_cast<int>(ShExecInfo.hInstApp) <= 32) {

    // some type of error occurred
    switch (reinterpret_cast<int>(ShExecInfo.hInstApp)) {
    case ERROR_FILE_NOT_FOUND:
    case ERROR_PATH_NOT_FOUND:
	    MessageBox(NULL, "A required file could not be found. \n"
                 "You may need to install a Java runtime\n"
                 "or re-install ReplicatorG.",
                 "ReplicatorG Error", MB_OK);
	    break;
    case 0:
    case SE_ERR_OOM:
	    MessageBox(NULL, "Not enough memory or resources to run at"
                 " this time.", "ReplicatorG Error", MB_OK);
	    
	    break;
    default:
	    MessageBox(NULL, "There is a problem with your installation.\n"
                 "If the problem persists, re-install the program.", 
                 "ReplicatorG Error", MB_OK);
	    break;
    }
  }

  return 0;
}
