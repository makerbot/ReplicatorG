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

public class PauseCommandFactory implements CommandFactory
{
    public boolean isMatch(final String commandName)
    {
        final boolean result = "pause".equals(commandName);
        return result;
    }

    public Command createCommand(final List<String> arguments)
        throws ParseException, ExtraArgumentsException
    {
        final String[] array = arguments.toArray(new String[0]);
        final CommandLineParser commandLineParser = new GnuParser();
        final Options options = createOptions();
        final CommandLine commandLine = commandLineParser.parse(
            options, array, false);
        final List<String> buildArguments = getArgumentsAsList(commandLine);
        if (0 != buildArguments.size())
        {
            throw new ExtraArgumentsException("pause", arguments);
        }
        else
        {
            final String busName = handleBusName(commandLine);
            final Command serviceCommand = new PauseCommand(busName);
            return serviceCommand;
        }
    }

    private Options createOptions()
    {
        final Options options = new Options();
        options.addOption(OptionBuilder
            .withLongOpt("bus-name")
            .hasArg()
            .withArgName("BUS-NAME")
            .withDescription("set the D-Bus bus name")
            .isRequired()
            .create());
        return options;
    }

    private List<String> getArgumentsAsList(final CommandLine commandLine)
    {
        final String[] array = commandLine.getArgs();
        final List<String> list = Arrays.asList(array);
        return list;
    }

    private String handleBusName(final CommandLine commandLine)
    {
        final String busName;
        if (commandLine.hasOption("bus-name"))
        {
            busName = commandLine.getOptionValue("bus-name");
        }
        else
        {
            busName = null;
        }
        return busName;
    }
}
