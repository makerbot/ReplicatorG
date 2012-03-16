// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;
import org.freedesktop.dbus.BusAddress;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Message;
import org.freedesktop.dbus.MethodCall;
import org.freedesktop.dbus.Transport;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

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
        final MachineState machineState = event.getState();
        final UInt32 state = convertStateToDbus(machineState);

        try
        {
            final BusAddress address = this.connection.getAddress();
            final Transport transport = new Transport(address);
            try
            {
                final Message hello = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
                transport.mout.writeMessage(hello);

                final String source = "com.makerbot.Printer.printer0";
                final String path = "/com/makerbot/Printer";
                final String iface = "com.makerbot.Printer";
                final String member = "State";
                final String sig = "sa{sv}as";
                final Map<String, Variant> changedProperties
                    = new HashMap<String, Variant>();
                final Variant<UInt32> stateVariant = new Variant<UInt32>(state,
                    UInt32.class);
                changedProperties.put("State", stateVariant);
                final List<String> invalidatedProperties
                    = new ArrayList<String>();
                final Message message = new DBusSignal(source, path, iface,
                    member, sig, iface, changedProperties,
                    invalidatedProperties);
                transport.mout.writeMessage(message);
            }
            finally
            {
                transport.disconnect();
            }
        }
        catch (final IOException exception)
        {
            throw new RuntimeException(exception);
        }
        catch (final ParseException exception)
        {
            throw new RuntimeException(exception);
        }
        catch (final DBusException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    public void machineProgress(final MachineProgressEvent event)
    {
    }

    public void toolStatusChanged(final MachineToolStatusEvent event)
    {
    }

    private UInt32 convertStateToDbus(final MachineState machineState)
    {
        long code;
        switch (machineState.getState())
        {
        case NOT_ATTACHED:
            code = 0;
            break;
        case CONNECTING:
            code = 1;
            break;
        case READY:
            code = 2;
            break;
        case BUILDING:
            code = 3;
            break;
        case PAUSED:
            code = 4;
            break;
        case ERROR:
            code = 5;
            break;
        default:
            throw new IllegalArgumentException();
        }
        final UInt32 uint32 = new UInt32(code);
        return uint32;
    }
}
