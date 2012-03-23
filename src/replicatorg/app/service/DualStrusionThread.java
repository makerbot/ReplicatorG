// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.exceptions.DBusException;

import com.makerbot.alpha.ToolpathGenerator1;
import replicatorg.app.Base;
import replicatorg.app.gcode.DualStrusionConstruction;
import replicatorg.app.gcode.MutableGCodeSource;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.model.MachineType;

public class DualStrusionThread extends Thread
{
    private final MachineInterface machineInterface;

    private final DBusConnection connection;

    private final File inputFile0;

    private final File inputFile1;

    private final File outputFile;

    public DualStrusionThread(final MachineInterface machineInterface,
        final DBusConnection connection, final File inputFile0,
        final File inputFile1, final File outputFile)
    {
        this.machineInterface = machineInterface;
        this.connection = connection;
        this.inputFile0 = inputFile0;
        this.inputFile1 = inputFile1;
        this.outputFile = outputFile;
    }

    @Override
    public void run()
    {
        final MutableGCodeSource startSource = new MutableGCodeSource(
            this.machineInterface.getModel().getDualstartBookendCode());
        final MutableGCodeSource endSource = new MutableGCodeSource(
            this.machineInterface.getModel().getEndBookendCode());
        final MachineType type = this.machineInterface.getMachineType();
        final DualStrusionConstruction construction
            = new DualStrusionConstruction(this.inputFile0, this.inputFile1,
            startSource, endSource, type, false);
        construction.combine();
        construction.getCombinedFile().writeToFile(this.outputFile);
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
