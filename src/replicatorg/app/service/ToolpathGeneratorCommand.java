// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

import replicatorg.app.Base;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineLoader;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

public class ToolpathGeneratorCommand implements Command
{
    private final String machineName;

    private final String busName;

    public ToolpathGeneratorCommand(final String machineName,
        final String busName)
    {
        this.machineName = machineName;
        this.busName = busName;
    }

    public int execute()
    {
        int status;
        try
        {
            final DBusConnection connection
                = DBusConnection.getConnection(DBusConnection.SESSION);
            if (null != this.busName)
            {
                connection.requestBusName(busName);
            }
            final MachineLoader machineLoader = Base.getMachineLoader();
            final MachineInterface machineInterface
                = machineLoader.getMachineInterface(this.machineName);
            final ToolpathGeneratorImpl toolpathGeneratorImpl
                = new ToolpathGeneratorImpl(machineInterface, connection);
            connection.exportObject("/com/makerbot/ToolpathGenerator",
                toolpathGeneratorImpl);
            toolpathGeneratorImpl.run();

            status = 0;
        }
        catch (final DBusException exception)
        {
            exception.printStackTrace();
            status = 1;
        }
        return status;
    }
}
