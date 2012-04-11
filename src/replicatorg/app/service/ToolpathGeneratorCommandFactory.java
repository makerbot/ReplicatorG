// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import replicatorg.app.Base;

public class ToolpathGeneratorCommandFactory extends AbstractCommandFactory
{
    public boolean isMatch(final String commandName)
    {
        final boolean result = "toolpathGenerator".equals(commandName);
        return result;
    }

    @Override
    protected Command createCommand(final CommandLine commandLine)
        throws MissingArgumentException, ExtraArgumentsException
    {
        final List<String> extraArguments = getArgumentsAsList(commandLine);
        if (0 != extraArguments.size())
        {
            throw new ExtraArgumentsException("toolpathGenerator", extraArguments);
        }
        else
        {
            final String machineName = handleMachineName(commandLine);
            final String busName = handleBusName(commandLine);
            final Command command = new ToolpathGeneratorCommand(machineName, busName);
            return command;
        }
    }

    @Override
    protected Options createOptions()
    {
        final Options options = new Options();
        options.addOption(OptionBuilder
            .withLongOpt("machine-name")
            .hasArg()
            .withArgName("MACHINE-NAME")
            .withDescription("set the machine name")
            .create());
        options.addOption(OptionBuilder
            .withLongOpt("bus-name")
            .hasArg()
            .withArgName("BUS-NAME")
            .withDescription("set the D-Bus bus name")
            .create());
        return options;
    }

    private String handleMachineName(final CommandLine commandLine)
    {
        final String machineName;
        if (commandLine.hasOption("machine-name"))
        {
            machineName = commandLine.getOptionValue("machine-name");
        }
        else
        {
            machineName = "The Replicator Dual";
        }
        return machineName;
    }
}
