/*
  Part of the ReplicatorG project - http://www.replicat.org
  Copyright (c) 2008 Zach Smith

  Forked from Arduino: http://www.arduino.cc

  Based on Processing http://www.processing.org
  Copyright (c) 2004-05 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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
*/

package replicatorg.app;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.apple.mrj.MRJFileUtils;
import com.apple.mrj.MRJOSType;


/**
 * Handles sketchbook mechanics for the sketch menu and file I/O.
 */
public class Sketchbook {
  Editor editor;

  JMenu openMenu;
  JMenu popupMenu;
  //JMenu examples;
  JMenu importMenu;

  // set to true after the first time it's built.
  // so that the errors while building don't show up again.
  boolean builtOnce;

  //File sketchbookFolder;
  //String sketchbookPath;  // canonical path

  // last file/directory used for file opening
  //String handleOpenDirectory;
  // opted against this.. in imovie, apple always goes
  // to the "Movies" folder, even if that wasn't the last used

  // these are static because they're used by Sketch
  static File examplesFolder;
  static String examplesPath;  // canonical path (for comparison)

  public Sketchbook(Editor editor) {
    this.editor = editor;

    // this shouldn't change throughout.. it may as well be static
    // but only one instance of sketchbook will be built so who cares
    examplesFolder = new File(System.getProperty("user.dir"), "examples");
    examplesPath = examplesFolder.getAbsolutePath();

    String sketchbookPath = Preferences.get("sketchbook.path");

    // if a value is at least set, first check to see if the
    // folder exists. if it doesn't, warn the user that the
    // sketchbook folder is being reset.
    if (sketchbookPath != null) {
      File skechbookFolder = new File(sketchbookPath);
      if (!skechbookFolder.exists()) {
        Base.showWarning("Sketchbook folder disappeared",
                         "The sketchbook folder no longer exists,\n" +
                         "so a new sketchbook will be created in the\n" +
                         "default location.", null);
        sketchbookPath = null;
      }
    }



    if (sketchbookPath == null) {
      // by default, set default sketchbook path to the user's
      // home folder with 'sketchbook' as a subdirectory of that

      /*
      File home = new File(System.getProperty("user.home"));

      if (Base.platform == Base.MACOSX) {
        // on macosx put the sketchbook in the "Documents" folder
        home = new File(home, "Documents");

      } else if (Base.platform == Base.WINDOWS) {
        // on windows put the sketchbook in the "My Documents" folder
        home = new File(home, "My Documents");
      }
      */

      // use a subfolder called 'sketchbook'
      //File home = Preferences.getProcessingHome();
      //String folderName = Preferences.get("sketchbook.name.default");
      //File sketchbookFolder = new File(home, folderName);

      //System.out.println("resetting sketchbook path");
      File sketchbookFolder = Base.getDefaultSketchbookFolder();
      //System.out.println("default is " + sketchbookFolder);
      Preferences.set("sketchbook.path",
                      sketchbookFolder.getAbsolutePath());

      if (!sketchbookFolder.exists()) sketchbookFolder.mkdirs();
    }
    openMenu = new JMenu("Sketchbook");
    popupMenu = new JMenu("Sketchbook");
  }


  static public String getSketchbookPath() {
    return Preferences.get("sketchbook.path");
  }


  /**
   * Handle creating a sketch folder, return its base .gcode file
   * or null if the operation was cancelled.
   */
  public String handleNew(boolean noPrompt,
                          boolean shift) throws IOException {
    File newbieDir = null;
    String newbieName = null;

    boolean prompt = Preferences.getBoolean("sketchbook.prompt");
    if (shift) prompt = !prompt; // reverse behavior if shift is down

    // no sketch has been started, don't prompt for the name if it's
    // starting up, just make the farker. otherwise if the person hits
    // 'cancel' i'd have to add a thing to make p5 quit, which is silly.
    // instead give them an empty sketch, and they can look at examples.
    // i hate it when imovie makes you start with that goofy dialog box.
    // unless, ermm, they user tested it and people preferred that as
    // a way to get started. shite. now i hate myself.
    //
    if (noPrompt) prompt = false;

    if (prompt) {
      // prompt for the filename and location for the new sketch
      FileDialog fd = new FileDialog(editor,
                                     "Create sketch folder named:",
                                     FileDialog.SAVE);
      fd.setDirectory(getSketchbookPath());
      fd.setVisible(true);

      String newbieParentDir = fd.getDirectory();
      newbieName = fd.getFile();
      if (newbieName == null) return null;

      newbieName = sanitizeName(newbieName);
      newbieDir = new File(newbieParentDir, newbieName);

    } else {
      // use a generic name like sketch_031008a, the date plus a char
      String newbieParentDir = getSketchbookPath();

      int index = 0;
      SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
      String purty = formatter.format(new Date());
      do {
        newbieName = "sketch_" + purty + ((char) ('a' + index));
        newbieDir = new File(newbieParentDir, newbieName);
        index++;
      } while (newbieDir.exists());
    }

    // make the directory for the new sketch
    newbieDir.mkdirs();

    // make an empty gcode file
    File newbieFile = new File(newbieDir, newbieName + ".gcode");
    new FileOutputStream(newbieFile);  // create the file

    // TODO this wouldn't be needed if i could figure out how to
    // associate document icons via a dot-extension/mime-type scenario
    // help me steve jobs, you're my only hope.

    // jdk13 on osx, or jdk11
    // though apparently still available for 1.4
    if (Base.isMacOS()) {
      MRJFileUtils.setFileTypeAndCreator(newbieFile,
                                         MRJOSType.kTypeTEXT,
                                         new MRJOSType("Gcde"));
      // thank you apple, for changing this @#$)(*
      //com.apple.eio.setFileTypeAndCreator(String filename, int, int)
    }

    // make a note of a newly added sketch in the sketchbook menu
    rebuildMenusAsync();

    // now open it up
    //handleOpen(newbieName, newbieFile, newbieDir);
    //return newSketch;
    return newbieFile.getAbsolutePath();
  }


  /**
   * Convert to sanitized name and alert the user
   * if changes were made.
   */
  static public String sanitizeName(String origName) {
    String newName = sanitizedName(origName);

    if (!newName.equals(origName)) {
      Base.showMessage("Naming issue",
                       "The sketch name had to be modified.\n" +
                       "You can only use basic letters and numbers\n" +
                       "to name a sketch (ascii only and no spaces,\n" +
                       "it can't start with a number, and should be\n" +
                       "less than 64 characters long)");
    }
    return newName;
  }


  /**
   * Return true if the name is valid for a Processing sketch.
   */
  static public boolean isSanitary(String name) {
    return sanitizedName(name).equals(name);
  }


  /**
   * Produce a sanitized name that fits our standards for likely to work.
   * <p/>
   * Java classes have a wider range of names that are technically allowed
   * (supposedly any Unicode name) than what we support. The reason for
   * going more narrow is to avoid situations with text encodings and
   * converting during the process of moving files between operating
   * systems, i.e. uploading from a Windows machine to a Linux server,
   * or reading a FAT32 partition in OS X and using a thumb drive.
   * <p/>
   * This helper function replaces everything but A-Z, a-z, and 0-9 with
   * underscores. Also disallows starting the sketch name with a digit.
   */
  static public String sanitizedName(String origName) {
    char c[] = origName.toCharArray();
    StringBuffer buffer = new StringBuffer();

    // can't lead with a digit, so start with an underscore
    if ((c[0] >= '0') && (c[0] <= '9')) {
      buffer.append('_');
    }
    for (int i = 0; i < c.length; i++) {
      if (((c[i] >= '0') && (c[i] <= '9')) ||
          ((c[i] >= 'a') && (c[i] <= 'z')) ||
          ((c[i] >= 'A') && (c[i] <= 'Z')) ||
          ((c[i] == '-') || (c[i] == '_'))) {
        buffer.append(c[i]);

      } else {
        buffer.append('_');
      }
    }
    // let's not be ridiculous about the length of filenames.
    // in fact, Mac OS 9 can handle 255 chars, though it can't really
    // deal with filenames longer than 31 chars in the Finder.
    // but limiting to that for sketches would mean setting the
    // upper-bound on the character limit here to 25 characters
    // (to handle the base name + ".class")
    if (buffer.length() > 63) {
      buffer.setLength(63);
    }
    return buffer.toString();
  }


  public String handleOpen() {
    // swing's file choosers are ass ugly, so we use the
    // native (awt peered) dialogs where possible
    FileDialog fd = new FileDialog(editor, //new Frame(),
                                   "Open a Processing sketch...",
                                   FileDialog.LOAD);
    //fd.setDirectory(Preferences.get("sketchbook.path"));
    //fd.setDirectory(getSketchbookPath());

    // only show .gcode files as eligible bachelors
    // TODO this doesn't seem to ever be used. AWESOME.
    fd.setFilenameFilter(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          //System.out.println("check filter on " + dir + " " + name);
          return name.toLowerCase().endsWith(".gcode");
        }
      });

    // gimme some money
    fd.setVisible(true);

    // what in the hell yu want, boy?
    String directory = fd.getDirectory();
    String filename = fd.getFile();

    // user cancelled selection
    if (filename == null) return null;

    // this may come in handy sometime
    //handleOpenDirectory = directory;

    File selection = new File(directory, filename);
    return selection.getAbsolutePath();
  }


  /**
   * Asynchronous version of menu rebuild to be used on 'new' and 'save',
   * to prevent the interface from locking up until the menus are done.
   */
  public void rebuildMenusAsync() {
    // disabling the async option for actual release, this hasn't been tested
    /*
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          rebuildMenus();
        }
      });
    */
    rebuildMenus();
  }


  /**
   * Rebuild the menu full of sketches based on the
   * contents of the sketchbook.
   *
   * Creates a separate JMenu object for the popup,
   * because it seems that after calling "getPopupMenu"
   * the menu will disappear from its original location.
   */
  public void rebuildMenus() {
    //EditorConsole.systemOut.println("rebuilding menus");
      // rebuild file/open and the toolbar popup menus
      buildMenu(openMenu);
      builtOnce = true;  // disable error messages while loading
      buildMenu(popupMenu);
    //EditorConsole.systemOut.println("done rebuilding menus");
  }


  public void buildMenu(JMenu menu) {
    JMenuItem item;

    // rebuild the popup menu
    menu.removeAll();

    //item = new JMenuItem("Open...");
    item = Editor.newJMenuItem("Open...", 'O', false);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleOpen(null);
        }
      });
    menu.add(item);
    menu.addSeparator();

    try {
      boolean sketches =
        addSketches(menu, new File(getSketchbookPath()));
      if (sketches) menu.addSeparator();
    } catch (IOException e) {
      e.printStackTrace();
    }
  
    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleOpen(e.getActionCommand());
        }
      };

  }


  public JMenu getOpenMenu() {
    if (openMenu == null) rebuildMenus();
    return openMenu;
  }


  public JPopupMenu getPopupMenu() {
    if (popupMenu == null) rebuildMenus();
    return popupMenu.getPopupMenu();
  }


  public JMenu getImportMenu() {
    return importMenu;
  }


  protected boolean addSketches(JMenu menu, File folder) throws IOException {
    // skip .DS_Store files, etc
    if (!folder.isDirectory()) return false;

    String list[] = folder.list();
    // if a bad folder or something like that, this might come back null
    if (list == null) return false;

    // alphabetize list, since it's not always alpha order
    // replaced hella slow bubble sort with this feller for 0093
    Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.handleOpen(e.getActionCommand());
        }
      };

    boolean ifound = false;

    for (int i = 0; i < list.length; i++) {
      if ((list[i].charAt(0) == '.') ||
          list[i].equals("CVS")) continue;

      File subfolder = new File(folder, list[i]);
      if (!subfolder.isDirectory()) continue;

      File entry = new File(subfolder, list[i] + ".gcode");
      // if a .gcode file of the same prefix as the folder exists..
      if (entry.exists()) {
        //String sanityCheck = sanitizedName(list[i]);
        //if (!sanityCheck.equals(list[i])) {
        if (!Sketchbook.isSanitary(list[i])) {
          if (!builtOnce) {
            String complaining =
              "The sketch \"" + list[i] + "\" cannot be used.\n" +
              "Sketch names must contain only basic letters and numbers\n" +
              "(ASCII-only with no spaces, " +
              "and it cannot start with a number).\n" +
              "To get rid of this message, remove the sketch from\n" +
              entry.getAbsolutePath();
            Base.showMessage("Ignoring sketch with bad name", complaining);
          }
          continue;
        }

        JMenuItem item = new JMenuItem(list[i]);
        item.addActionListener(listener);
        item.setActionCommand(entry.getAbsolutePath());
        menu.add(item);
        ifound = true;

      } else {
        // not a sketch folder, but maybe a subfolder containing sketches
        JMenu submenu = new JMenu(list[i]);
        // needs to be separate var
        // otherwise would set ifound to false
        boolean found = addSketches(submenu, subfolder); //, false);
        if (found) {
          menu.add(submenu);
          ifound = true;
        }
      }
    }
    return ifound;  // actually ignored, but..
  }

  /**
   * Clear out projects that are empty.
   */
  public void clean() {
    //if (!Preferences.getBoolean("sketchbook.auto_clean")) return;

    File sketchbookFolder = new File(getSketchbookPath());
    if (!sketchbookFolder.exists()) return;

    //String entries[] = new File(userPath).list();
    String entries[] = sketchbookFolder.list();
    if (entries != null) {
      for (int j = 0; j < entries.length; j++) {
        //System.out.println(entries[j] + " " + entries.length);
        if (entries[j].charAt(0) == '.') continue;

        //File prey = new File(userPath, entries[j]);
        File prey = new File(sketchbookFolder, entries[j]);
        File gcode = new File(prey, entries[j] + ".gcode");

        // make sure this is actually a sketch folder with a .gcode,
        // not a .DS_Store file or another random user folder

        if (gcode.exists() &&
            (Base.calcFolderSize(prey) == 0)) {
          //System.out.println("i want to remove " + prey);

          if (Preferences.getBoolean("sketchbook.auto_clean")) {
            Base.removeDir(prey);

          } else {  // otherwise prompt the user
            String prompt =
              "Remove empty sketch titled \"" + entries[j] + "\"?";

            Object[] options = { "Yes", "No" };
            int result =
              JOptionPane.showOptionDialog(editor,
                                           prompt,
                                           "Housekeeping",
                                           JOptionPane.YES_NO_OPTION,
                                           JOptionPane.QUESTION_MESSAGE,
                                           null,
                                           options,
                                           options[0]);
            if (result == JOptionPane.YES_OPTION) {
              Base.removeDir(prey);
            }
          }
        }
      }
    }
  }
}
