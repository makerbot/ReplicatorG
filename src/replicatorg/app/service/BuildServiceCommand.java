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
                    Base.logger.info("Waiting for connection.");
                    waitForConnected(machineInterface);
                    Base.logger.info("Connected");

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
                        Base.logger.info("Starting build.");
                        machineInterface.buildDirect(gcodeSource);
                        Base.logger.info("Waiting for the build to start.");
                        waitForBuilding(machineInterface);
                        Base.logger.info("Waiting for the build to end.");
                        waitForReady(machineInterface);
                        Base.logger.info("Build is done.");
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

    private void waitForStates(
        final MachineInterface machineInterface,
        final long timeout,
        final MachineState.State ... states)
    {
        final long start = System.currentTimeMillis();
        final long end = start + timeout;
        for (;;)
        {
            final MachineState machineState
                = machineInterface.getMachineState();
            if (isInState(machineInterface, states))
            {
                break;
            }
            else
            {
                final long remaining;
                if (0 == timeout)
                {
                    remaining = 0;
                }
                else
                {
                    final long now = System.currentTimeMillis();
                    remaining = end - now;
                    if (remaining <= 0)
                    {
                        throw new RuntimeException("took too long");
                    }
                }
                try
                {
                    waitOnLock(remaining);
                }
                catch (final InterruptedException exception)
                {
                    // Ignored, but it causes us to re-check the state and
                    // either resume waiting or return.
                }
            }
        }
    }

    private boolean isInState(
        final MachineInterface machineInterface,
        final MachineState.State ... states)
    {
        final MachineState.State currentState
            = machineInterface.getMachineState().getState();
        for (final MachineState.State state : states)
        {
            if (currentState == state)
            {
                return true;
            }
        }
        return false;
    }

    private void waitForConnected(final MachineInterface machineInterface)
    {
        waitForStates(machineInterface, 60000, MachineState.State.READY,
            MachineState.State.BUILDING, MachineState.State.PAUSED,
            MachineState.State.ERROR);
    }

    private void waitForBuilding(final MachineInterface machineInterface)
    {
        waitForStates(machineInterface, 60000, MachineState.State.BUILDING,
            MachineState.State.PAUSED, MachineState.State.BUILDING_OFFLINE);
    }

    private void waitForReady(final MachineInterface machineInterface)
    {
        waitForStates(machineInterface, 0, MachineState.State.READY);
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
