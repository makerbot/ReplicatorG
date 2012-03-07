// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;

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

//
// TODO: fix exception badness!!
//

public class BuildServiceCommand implements ServiceCommand
{
    private final String filename;

    private final Object lock;

    public BuildServiceCommand(final String filename)
    {
        this.filename = filename;
        this.lock = new Object();
    }

    public void execute(final ServiceContext serviceContext)
    {
        final MachineLoader machineLoader = Base.getMachineLoader();
        final Listener listener = new Listener();
        machineLoader.addMachineListener(listener);

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
                    System.out.printf("i am waiting to connect%n");
                    waitForConnected(machineInterface);
                    System.out.printf("i am connected%n");

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
                        System.out.printf("i am starting a build%n");
                        machineInterface.buildDirect(gcodeSource);
                        System.out.printf("i am waiting for the build to start%n");
                        waitForBuilding(machineInterface);
                        System.out.printf("i am waiting for the build to end%n");
                        waitForReady(machineInterface);
                        System.out.printf("done!!%n");
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
        }
        else
        {
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

    private void waitOnLock(final long timeout) throws InterruptedException
    {
        synchronized (this.lock)
        {
            this.lock.wait(timeout);
        }
    }

    private void waitForConnected(final MachineInterface machineInterface)
    {
        final long start = System.currentTimeMillis();
        final long end = start + 60000;
        for (;;)
        {
            final MachineState machineState
                = machineInterface.getMachineState();
            if (machineState.isConnected())
            {
                break;
            }
            else
            {
                final long now = System.currentTimeMillis();
                final long remaining = end - now;
                if (remaining < 0)
                {
                    throw new RuntimeException("took too long");
                }
                else
                {
                    try
                    {
                        waitOnLock(remaining);
                    }
                    catch (final InterruptedException exception)
                    {
                        // IGNORED
                    }
                }
            }
        }
    }

    private void waitForBuilding(final MachineInterface machineInterface)
    {
        final long start = System.currentTimeMillis();
        final long end = start + 60000;
        for (;;)
        {
            final MachineState machineState
                = machineInterface.getMachineState();
            if (machineState.isBuilding())
            {
                break;
            }
            else
            {
                final long now = System.currentTimeMillis();
                final long remaining = end - now;
                if (remaining < 0)
                {
                    throw new RuntimeException("took too long");
                }
                else
                {
                    try
                    {
                        waitOnLock(remaining);
                    }
                    catch (final InterruptedException exception)
                    {
                        // IGNORED
                    }
                }
            }
        }
    }

    private void waitForReady(final MachineInterface machineInterface)
    {
        for (;;)
        {
            final MachineState machineState
                = machineInterface.getMachineState();
            if (machineState.canPrint())
            {
                break;
            }
            else
            {
                try
                {
                    waitOnLock(0);
                }
                catch (final InterruptedException exception)
                {
                    // IGNORED
                }
            }
        }
    }

    private class Listener implements MachineListener
    {
        public void machineStateChanged(final MachineStateChangeEvent event)
        {
            notifyLock();
        }

        public void machineProgress(final MachineProgressEvent event)
        {
            notifyLock();
        }

        public void toolStatusChanged(final MachineToolStatusEvent event)
        {
            notifyLock();
        }

        private void notifyLock()
        {
            synchronized (BuildServiceCommand.this.lock)
            {
                BuildServiceCommand.this.lock.notify();
            }
        }
    }
}
