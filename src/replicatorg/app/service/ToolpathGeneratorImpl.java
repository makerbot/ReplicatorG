// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.IOException;
import java.util.logging.Level;
import org.freedesktop.dbus.DBusConnection;

import com.makerbot.alpha.ToolpathGenerator1;
import replicatorg.app.Base;
import replicatorg.app.gcode.MutableGCodeSource;
import replicatorg.machine.MachineInterface;
import replicatorg.machine.model.MachineType;
import replicatorg.model.Build;
import replicatorg.plugin.toolpath.ToolpathGenerator;
import replicatorg.plugin.toolpath.ToolpathGeneratorFactory;
import replicatorg.plugin.toolpath.ToolpathGeneratorThread;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgeGenerator;
import replicatorg.plugin.toolpath.skeinforge.SkeinforgePostProcessor;

public class ToolpathGeneratorImpl implements ToolpathGenerator1
{
    private final MachineInterface machineInterface;

    private final DBusConnection connection;

    private boolean run;

    public ToolpathGeneratorImpl(final MachineInterface machineInterface,
        final DBusConnection connection)
    {
        this.machineInterface = machineInterface;
        this.connection = connection;
        this.run = true;
    }

    public boolean isRemote()
    {
        return false;
    }

    public void Generate(final String filename)
    {
        Base.logger.log(Level.INFO, "Generate: filename={0}", filename);
        try
        {
            final ToolpathGenerator generator
                = ToolpathGeneratorFactory.createSelectedGenerator();
            if (generator instanceof SkeinforgeGenerator)
            {
                final SkeinforgeGenerator skeinforgeGenerator
                    = (SkeinforgeGenerator) generator;
                final String profile = Base.preferences.get(
                    "replicatorg.skeinforge.profile", null);
                if (null == profile)
                {
                    throw new RuntimeException("no profile");
                }
                else
                {
                    skeinforgeGenerator.setProfile(profile);
                    final SkeinforgePostProcessor processor
                        = skeinforgeGenerator.getPostProcessor();
                    processor.setMachineType(
                        this.machineInterface.getMachineType());
                    processor.setPrependMetaInfo(true);
                    processor.setStartCode(new MutableGCodeSource(
                        this.machineInterface.getModel().getStartBookendCode()));
                    processor.setEndCode(new MutableGCodeSource(
                        this.machineInterface.getModel().getEndBookendCode()));
                    processor.setMultiHead(isDualDriver());
                    if (this.machineInterface.getMachineType()
                        == MachineType.THE_REPLICATOR)
                    {
                        processor.setAddProgressUpdates(true);
                    }
                }
            }
            final Build build = new Build(filename);
            final ToolpathGeneratorThread thread
                = new ToolpathGeneratorThread(null, generator, build, true);
            final ToolpathGeneratorListener listener
                = new ToolpathGeneratorListener(this.connection);
            thread.addListener(listener);
            thread.start();
        }
        catch (final IOException exception)
        {
            throw new RuntimeException(exception);
        }
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

    private boolean isDualDriver()
    {
        return 2 == this.machineInterface.getModel().getTools().size();
    }
}
