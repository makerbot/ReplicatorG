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

public class BuildCommandFactory implements CommandFactory
{
    public boolean isMatch(final String commandName)
    {
        final boolean result = "build".equals(commandName);
        return result;
    }

    public Command createCommand(final List<String> arguments)
        throws ParseException, MissingArgumentException,
        ExtraArgumentsException
    {
        final String[] array = arguments.toArray(new String[0]);
        final CommandLineParser commandLineParser = new GnuParser();
        final Options options = createOptions();
        final CommandLine commandLine = commandLineParser.parse(
            options, array, false);
        final List<String> buildArguments = getArgumentsAsList(commandLine);
        if (0 == buildArguments.size())
        {
            throw new MissingArgumentException("build", "FILENAME");
        }
        else
        if (buildArguments.size() > 1)
        {
            final List<String> extraArguments
                = arguments.subList(1, buildArguments.size());
            throw new ExtraArgumentsException("build", extraArguments);
        }
        else
        {
            final String busName = handleBusName(commandLine);
            final String filename = buildArguments.get(0);
            final Command serviceCommand = new BuildCommand(busName, filename);
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
