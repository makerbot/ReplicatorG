// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;

import com.makerbot.Printer;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

public class PrinterListener implements MachineListener
{
    private final DBusConnection connection;

    public PrinterListener(final DBusConnection connection)
    {
        this.connection = connection;
    }

    public void machineStateChanged(final MachineStateChangeEvent event)
    {
        final MachineState.State state = event.getState().getState();
        System.out.println("state=" + state);
        try
        {
            final UInt32 oldState = new UInt32(0);
            final UInt32 newState = new UInt32(0);
            final DBusSignal stateChanged = new Printer.StateChanged(
                "/com/makerbot/Printer", oldState, newState);
            this.connection.sendSignal(stateChanged);
        }
        catch (final DBusException exception)
        {
            exception.printStackTrace();
        }
    }

    public void machineProgress(final MachineProgressEvent event)
    {
    }

    public void toolStatusChanged(final MachineToolStatusEvent event)
    {
    }
}
