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

public class BuildToFileCommandFactory extends RemoteCommandFactory
{
    public boolean isMatch(final String commandName)
    {
        final boolean result = "buildToFile".equals(commandName);
        return result;
    }

    @Override
    protected Command createCommand(final CommandLine commandLine)
        throws MissingArgumentException, ExtraArgumentsException
    {
        final List<String> buildArguments = getArgumentsAsList(commandLine);
        if (0 == buildArguments.size())
        {
            throw new MissingArgumentException("buildToFile", "INPUT-FILENAME");
        }
        else
        if (1 == buildArguments.size())
        {
            throw new MissingArgumentException("buildToFIle", "OUTPUT-FILENAME");
        }
        else
        if (buildArguments.size() > 2)
        {
            final List<String> extraArguments
                = buildArguments.subList(1, buildArguments.size());
            throw new ExtraArgumentsException("build", extraArguments);
        }
        else
        {
            final String busName = handleBusName(commandLine);
            final String inputFilename = buildArguments.get(0);
            final String outputFilename = buildArguments.get(1);
            final Command command = new BuildToFileCommand(busName,
                inputFilename, outputFilename);
            return command;
        }
    }
}
