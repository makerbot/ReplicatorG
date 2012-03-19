// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

import com.makerbot.alpha.Printer1;

public abstract class RemoteCommand implements Command
{
    private final String busName;

    public RemoteCommand(final String busName)
    {
        this.busName = busName;
    }

    public int execute()
    {
        int status;
        try
        {
            final DBusConnection connection
                = DBusConnection.getConnection(DBusConnection.SESSION);
            final Printer1 printer = connection.getRemoteObject(this.busName,
                "/com/makerbot/Printer", Printer1.class);
            executeRemoteCommand(printer);
            status = 0;
        }
        catch (final DBusException exception)
        {
            exception.printStackTrace();
            status = 1;
        }
        return status;
    }

    protected abstract void executeRemoteCommand(Printer1 printer);
}
