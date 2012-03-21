package com.makerbot.alpha;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;
public interface Printer1 extends DBusInterface
{
   public static class Progress extends DBusSignal
   {
      public final int lines;
      public final int totalLines;
      public Progress(String path, int lines, int totalLines) throws DBusException
      {
         super(path, lines, totalLines);
         this.lines = lines;
         this.totalLines = totalLines;
      }
   }

  public void Build(String filename);
  public void BuildToFile(String inputFilename, String outputFilename);
  public void Pause();
  public void Unpause();
  public void StopMotion();
  public void StopAll();

}
