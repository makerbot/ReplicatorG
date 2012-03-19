// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;

import com.makerbot.alpha.ToolpathGenerator1;
import replicatorg.plugin.toolpath.ToolpathGenerator;

public class ToolpathGeneratorListener
    implements ToolpathGenerator.GeneratorListener
{
    private final DBusConnection connection;

    public ToolpathGeneratorListener(final DBusConnection connection)
    {
        this.connection = connection;
    }

    public void updateGenerator(
        final ToolpathGenerator.GeneratorEvent event)
    {
        try
        {
            final DBusSignal signal = new ToolpathGenerator1.Progress(
                "/com/makerbot/ToolpathGenerator");
            this.connection.sendSignal(signal);
        }
        catch (final DBusException exception)
        {
            exception.printStackTrace();
        }
    }

    public void generationComplete(
        final ToolpathGenerator.GeneratorEvent event)
    {
        try
        {
            final DBusSignal signal = new ToolpathGenerator1.Complete(
                "/com/makerbot/ToolpathGenerator");
            this.connection.sendSignal(signal);
        }
        catch (final DBusException exception)
        {
            exception.printStackTrace();
        }
    }
}
