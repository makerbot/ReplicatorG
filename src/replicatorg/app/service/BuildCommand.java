// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

import com.makerbot.Printer;
import replicatorg.app.Base;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineLoader;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

public class BuildCommand implements Command
{
    private final String busName;

    private final String filename;

    public BuildCommand(final String busName, final String filename)
    {
        this.busName = busName;
        this.filename = filename;
    }

    public int execute()
    {
        int status;
        try
        {
            final DBusConnection connection
                = DBusConnection.getConnection(DBusConnection.SESSION);
            final Printer printer = connection.getRemoteObject(this.busName,
                "/com/makerbot/Printer", Printer.class);
            printer.Build(this.filename);
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
