// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
// import org.freedesktop.DBus.Introspectable;
// import org.freedesktop.DBus.Properties;
// import org.freedesktop.dbus.Variant;

import com.makerbot.alpha.Printer1;
import replicatorg.machine.MachineInterface;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

public class PrinterImpl implements Printer1 // , Introspectable, Properties
{
    private final MachineInterface machineInterface;

    private final String port;

    private boolean run;

    public PrinterImpl(final MachineInterface machineInterface,
        final String port)
    {
        this.machineInterface = machineInterface;
        this.port = port;
        this.run = true;
    }

    public boolean isRemote()
    {
        return false;
    }

    public void Build(final String filename)
    {
        System.out.printf("Build: filename=%1$s%n", filename);

        if (false == this.machineInterface.isConnected())
        {
            this.machineInterface.connect(this.port);
        }

        try
        {
            final GCodeSource gcodeSource = getGCodeSource(filename);
            this.machineInterface.buildDirect(gcodeSource);
        }
        catch (final IOException exception)
        {
            throw new RuntimeException(exception); // TODO: shameful....
        }
    }

    public void BuildToFile(final String inputFilename,
        final String outputFilename)
    {
        System.out.printf(
            "BuildToFilename: inputFilename=%1$s, outputFilename=%2$s%n",
            inputFilename, outputFilename);

        try
        {
            final GCodeSource gcodeSource = getGCodeSource(inputFilename);
            this.machineInterface.buildToFile(gcodeSource, outputFilename);
        }
        catch (final IOException exception)
        {
            throw new RuntimeException(exception); // TODO: more shameful....
        }
    }

    public void Pause()
    {
        System.out.println("Pause");
        this.machineInterface.pause();
    }

    public void Unpause()
    {
        System.out.println("Unpause");
        this.machineInterface.unpause();
    }

    public void StopMotion()
    {
        System.out.println("StopMotion");
        this.machineInterface.stopMotion();
    }

    public void StopAll()
    {
        System.out.println("StopAll");
        this.machineInterface.stopAll();
    }

    public void run()
    {
        while (this.run)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (final InterruptedException exception)
            {
            }
        }
    }

    private GCodeSource getGCodeSource(final String filename) throws IOException
    {
        final File file = new File(filename);
        final List<String> lines = FileUtils.readLines(file);
        final GCodeSource gcodeSource = new StringListSource(lines);
        return gcodeSource;
    }

}
