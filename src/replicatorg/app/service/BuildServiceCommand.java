// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;

import replicatorg.app.Base;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.MachineLoader;
import replicatorg.model.GCodeSource;
import replicatorg.model.StringListSource;

//
// TODO: fix exception badness!!
//

public class BuildServiceCommand implements ServiceCommand
{
    private final String filename;

    public BuildServiceCommand(final String filename)
    {
        this.filename = filename;
    }

    public void execute(final ServiceContext serviceContext)
    {
        final MachineLoader machineLoader = Base.getMachineLoader();

        final String machineName = Base.preferences.get("machine.name", "The Replicator Dual");
        if (null == machineName)
        {
            throw new RuntimeException("i have no idea what my machine name is");
        }
        else
        {
            final MachineInterface machineInterface
                = machineLoader.getMachineInterface(machineName);
            if (null == machineInterface)
            {
                throw new RuntimeException("i don't have an interface");
            }
            else
            {
                final String serial
                    = Base.preferences.get("serial.last_selected", null);
                if (null == serial)
                {
                    throw new RuntimeException("i don't have a port");
                }
                else
                {
                    machineLoader.connect(serial);
                    try
                    {
                        // TODO: omg use some sort of listener hoobajoob
                        Thread.sleep(5000);
                    }
                    catch (final InterruptedException exception)
                    {
                    }
                    if (false == machineLoader.isLoaded())
                    {
                        throw new RuntimeException("i'm not ready to build");
                    }
                    else
                    if (false == machineLoader.isConnected())
                    {
                        throw new RuntimeException("i'm not connected");
                    }
                    else
                    {
                        final GCodeSource gcodeSource = getGCodeSource();
                        machineInterface.buildDirect(gcodeSource);

                        while (true)
                        {
                            try
                            {
                                // TODO: omg you have to ^C when the print is done
                                Thread.sleep(1000);
                            }
                            catch (final InterruptedException exception)
                            {
                            }
                        }
                    }
                }
            }
        }
    }

    private GCodeSource getGCodeSource()
    {
        final File file = new File(filename);
        if (false == file.exists())
        {
            System.out.printf("fail!%n");
            throw new RuntimeException("your file doesn't exist and the error handling needs to be wired up in a sensible manner");
        } else {
            final List<String> lines;
            try
            {
                lines = FileUtils.readLines(file);
            }
            catch (final IOException exception)
            {
                throw new RuntimeException(exception);
            }
            final GCodeSource gcodeSource = new StringListSource(lines);
            return gcodeSource;
        }
    }
}
