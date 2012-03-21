// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;

import com.makerbot.alpha.Printer1;
import replicatorg.app.Base;
import replicatorg.machine.MachineInterface;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

public class PrinterImpl implements Printer1
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
        Base.logger.log(Level.INFO, "Build: filename={0}", filename);

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
            throw new RuntimeException(exception);
        }
    }

    public void BuildToFile(final String inputFilename,
        final String outputFilename)
    {
        Base.logger.log(Level.INFO,
            "BuildToFile: inputFilename={0}, outputFilename={1}",
            new Object[] {inputFilename, outputFilename});

        try
        {
            final GCodeSource gcodeSource = getGCodeSource(inputFilename);
            this.machineInterface.buildToFile(gcodeSource, outputFilename);
        }
        catch (final IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }

    public void Pause()
    {
        Base.logger.log(Level.INFO, "Pause");
        this.machineInterface.pause();
    }

    public void Unpause()
    {
        Base.logger.log(Level.INFO, "Unpause");
        this.machineInterface.unpause();
    }

    public void StopMotion()
    {
        Base.logger.log(Level.INFO, "StopMotion");
        this.machineInterface.stopMotion();
    }

    public void StopAll()
    {
        Base.logger.log(Level.INFO, "StopAll");
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
                // Ignored
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
