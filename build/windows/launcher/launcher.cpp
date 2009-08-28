// -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-

// launcher.cpp : Defines the class behaviors for the application.
//

// The size of all of the strings was made sort of ambiguously large, since
// 1) nothing is hurt by allocating an extra few bytes temporarily and
// 2) if the user has a long path, and it gets copied five times over for the
// classpath, the program runs the risk of crashing. Bad bad.

//
// I would just like to state for the record that the fact that
// this launcher even exists is absolutely fucking pathetic.
//

#define JAVA_ARGS "-Xms256m -Xmx256m "
#define JAVA_MAIN_CLASS "replicatorg.app.Base"

#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <string>


int STDCALL
WinMain (HINSTANCE hInst, HINSTANCE hPrev, LPSTR lpCmd, int nShow)
{
  // all these malloc statements... things may need to be larger.

  // what was passed to this application
  char *incoming_cmdline = (char *)malloc(strlen(lpCmd) * sizeof(char));
  strcpy (incoming_cmdline, lpCmd);

  // what gets put together to pass to jre
  char *outgoing_cmdline = (char *)malloc(16384 * sizeof(char));
        
  // prepend the args for -mx and -ms
  strcpy(outgoing_cmdline, JAVA_ARGS);

  // append the classpath and launcher.Application
  char *loaddir = (char *)malloc(MAX_PATH * sizeof(char));
  *loaddir = 0;

  GetModuleFileName(NULL, loaddir, MAX_PATH);
  // remove the application name
  *(strrchr(loaddir, '\\')) = '\0';

  char *cp = (char *)malloc(8 * strlen(loaddir) + 4096);

  char *env_classpath = (char *)malloc(16384 * sizeof(char));

  // ignoring CLASSPATH for now, because it's not needed
  // and causes more trouble than it's worth [0060]
  env_classpath[0] = 0;

  // We really ought to be loading these dynamically.
  std::vector<const char*> libs;
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

  std::stringstream cp;
  cp << loaddir << "\\lib;"
  cp << loaddir << "\\lib\\build;"
  for (std::vector<const char*> i = libs.begin(); i != libs.end(); i++) {
    cp << loaddir << "\\lib\\" << *i << ";"
  }

  if (!SetEnvironmentVariable("CLASSPATH", cp.str().c_str())) {
    MessageBox(NULL, "Could not set CLASSPATH environment variable",
               "ReplicatorG Error", MB_OK);
    return 0;
  }
  
  

  // add the name of the class to execute and a space before the next arg
  strcat(outgoing_cmdline, JAVA_MAIN_CLASS " ");

  // append additional incoming stuff (document names), if any
  strcat(outgoing_cmdline, incoming_cmdline);

  char *executable = (char *)malloc((strlen(loaddir) + 256) * sizeof(char));
  // loaddir is the name path to the current application

  strcpy(executable, "javaw.exe");

  SHELLEXECUTEINFO ShExecInfo;

  // set up the execution info
  ShExecInfo.cbSize = sizeof(SHELLEXECUTEINFO);
  ShExecInfo.fMask = 0;
  ShExecInfo.hwnd = 0;
  ShExecInfo.lpVerb = "open";
  ShExecInfo.lpFile = executable;
  ShExecInfo.lpParameters = outgoing_cmdline;
  ShExecInfo.lpDirectory = loaddir;
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
