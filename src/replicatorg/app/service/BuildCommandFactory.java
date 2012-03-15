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

public class BuildCommandFactory extends RemoteCommandFactory
{
    public boolean isMatch(final String commandName)
    {
        final boolean result = "build".equals(commandName);
        return result;
    }

    @Override
    protected Command createCommand(final CommandLine commandLine)
        throws MissingArgumentException, ExtraArgumentsException
    {
        final List<String> buildArguments = getArgumentsAsList(commandLine);
        if (0 == buildArguments.size())
        {
            throw new MissingArgumentException("build", "FILENAME");
        }
        else
        if (buildArguments.size() > 1)
        {
            final List<String> extraArguments
                = buildArguments.subList(1, buildArguments.size());
            throw new ExtraArgumentsException("build", extraArguments);
        }
        else
        {
            final String busName = handleBusName(commandLine);
            final String filename = buildArguments.get(0);
            final Command command = new BuildCommand(busName, filename);
            return command;
        }
    }
}
