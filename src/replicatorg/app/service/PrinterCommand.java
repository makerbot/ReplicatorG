// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

import com.makerbot.Printer;
import replicatorg.app.Base;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineLoader;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

public class PrinterCommand implements Command
{
    private final String machineName;

    private final String port;

    private final String busName;

    public PrinterCommand(final String machineName, final String port,
        final String busName)
    {
        this.machineName = machineName;
        this.port = port;
        this.busName = busName;
    }

    public int execute()
    {
        int status;
        try
        {
            final DBusConnection connection
                = DBusConnection.getConnection(DBusConnection.SESSION);
            try
            {
                register(connection);

                if (null != this.busName)
                {
                    connection.requestBusName(busName);
                }

                final MachineLoader machineLoader = Base.getMachineLoader();
                final MachineListener listener = new PrinterListener(connection);
                machineLoader.addMachineListener(listener);
                final MachineInterface machineInterface
                    = machineLoader.getMachineInterface(machineName);
                final PrinterImpl printerImpl = new PrinterImpl(machineInterface, this.port);
                connection.exportObject("/com/makerbot/Printer", printerImpl);
                printerImpl.run();
                status = 0;
            }
            finally
            {
                unregister(connection);
            }
        }
        catch (final DBusException exception)
        {
            exception.printStackTrace();
            status = 1;
        }
        return status;
    }

    private void register(final DBusConnection connection)
    {
        // TODO
    }

    private void unregister(final DBusConnection connection)
    {
        // TODO
    }
}
