package com.makerbot.alpha;
import org.freedesktop.dbus.DBusInterface;
public interface Printer1 extends DBusInterface
{

  public void Build(String filename);
  public void Pause();
  public void Unpause();
  public void StopMotion();
  public void StopAll();

}
