// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

import com.makerbot.Printer;

public class StopAllCommand implements Command
{
    private final String busName;

    public StopAllCommand(final String busName)
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
            final Printer printer = connection.getRemoteObject(this.busName,
                "/com/makerbot/Printer", Printer.class);
            printer.StopAll();
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
