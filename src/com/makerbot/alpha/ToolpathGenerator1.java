package com.makerbot.alpha;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;
public interface ToolpathGenerator1 extends DBusInterface
{
   public static class Progress extends DBusSignal
   {
      public Progress(String path) throws DBusException
      {
         super(path);
      }
   }
   public static class Complete extends DBusSignal
   {
      public Complete(String path) throws DBusException
      {
         super(path);
      }
   }

  public void Generate(String filename);
  public void GenerateDualStrusion(String inputFilename0, String inputFilename1, String outputFilename);

}
