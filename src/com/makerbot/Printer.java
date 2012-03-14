package com.makerbot;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.exceptions.DBusException;
public interface Printer extends DBusInterface
{
   public static class StateChanged extends DBusSignal
   {
      public final UInt32 oldState;
      public final UInt32 newState;
      public StateChanged(String path, UInt32 oldState, UInt32 newState) throws DBusException
      {
         super(path, oldState, newState);
         this.oldState = oldState;
         this.newState = newState;
      }
   }

  public void Build(String filename);
  public void Pause();
  public void Unpause();
  public void StopMotion();
  public void StopAll();

}
