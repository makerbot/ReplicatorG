// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.StringUtils;

public class ServiceMain
{
    public static void main(final String[] arguments)
    {
        final ServiceMain serviceMain = new ServiceMain();
        final int status = serviceMain.run(arguments);
        System.exit(status);
    }

    public int run(final String[] arguments)
    {
        final CommandLineParser commandLineParser = new GnuParser();
        final Options options = createOptions();
        int status;
        try
        {
            final CommandLine commandLine = commandLineParser.parse(
                options, arguments);
            executeCommandLine(commandLine);
            status = 0;
        }
        catch (final AlreadySelectedException exception)
        {
            handleCliAlreadySelectedException(exception);
            status = 1;
        }
        catch (final MissingArgumentException exception)
        {
            handleCliMissingArgumentException(exception);
            status = 1;
        }
        catch (final MissingOptionException exception)
        {
            handleCliMissingOptionException(exception);
            status = 1;
        }
        catch (final UnrecognizedOptionException exception)
        {
            handleCliUnrecognizedOptionException(exception);
            status = 1;
        }
        catch (final ParseException exception)
        {
            //
            // This branch must follow any catch-clauses for subclasses of
            // ParseException.
            //

            handleCliParseException(exception);
            status = 1;
        }
        catch (final NoCommandException exception)
        {
            handleNoCommandException(exception);
            status = 1;
        }
        catch (final UnknownCommandException exception)
        {
            handleUnknownCommandException(exception);
            status = 1;
        }
        catch (final replicatorg.app.service.MissingArgumentException
            exception)
        {
            //
            // This is semantically different than the commons-cli exception of
            // the same base name. Unfortunately, the commons-cli exception
            // constructors want either an Option or a formatted message. The
            // program does not have an Option at the throw site and we do not
            // want to generate a formatted message deep within the call stack
            // (that's the *opposite* of how exceptions are supposed to be
            // used).
            //

            handleMissingArgumentException(exception);
            status = 1;
        }
        catch (final ExtraArgumentsException exception)
        {
            handleExtraArgumentsException(exception);
            status = 1;
        }
        catch (final ServiceCommandException exception)
        {
            // TODO: handle the specific sub-classes on ServiceCommandException
            // before this.

            //
            // This branch must follow any catch-clauses for subclasses of
            // ServiceCommandException.
            //

            status = 1;
        }
        return status;
    }

    private Options createOptions()
    {
        final Options options = new Options();
        return options;
    }

    private void executeCommandLine(final CommandLine commandLine)
        throws NoCommandException, UnknownCommandException, ParseException,
            replicatorg.app.service.MissingArgumentException,
            ExtraArgumentsException, ServiceCommandException
    {
        final ServiceContext serviceContext
            = createServiceContext(commandLine);
        final String commandName = getCommandName(commandLine);
        final List<String> commandArguments
            = getCommandArguments(commandLine);
        final ServiceCommandFactory serviceCommandFactory
            = createServiceCommandFactory(commandName);
        final ServiceCommand serviceCommand
            = serviceCommandFactory.createServiceCommand(commandArguments);
        serviceCommand.execute(serviceContext);
    }

    private ServiceContext createServiceContext(final CommandLine commandLine)
    {
        return null;
    }

    private String getCommandName(final CommandLine commandLine)
        throws NoCommandException
    {
        final String[] args = commandLine.getArgs();
        if (0 == args.length)
        {
            throw new NoCommandException();
        }
        else
        {
            final String commandName = args[0];
            return commandName;
        }
    }

    private List<String> getCommandArguments(final CommandLine commandLine)
    {
        final String[] array = commandLine.getArgs();
        final List<String> list = Arrays.asList(array);
        final List<String> subList = list.subList(1, list.size());
        return subList;
    }

    private ServiceCommandFactory createServiceCommandFactory(
        final String commandName)
        throws UnknownCommandException
    {
        final ServiceCommandFactory serviceCommandFactory;
        if (COMMAND_BUILD.equals(commandName))
        {
            serviceCommandFactory = new BuildServiceCommandFactory();
        }
        else
        if (COMMAND_PAUSE.equals(commandName))
        {
            serviceCommandFactory = new PauseServiceCommandFactory();
        }
        else
        if (COMMAND_UNPAUSE.equals(commandName))
        {
            serviceCommandFactory = new UnpauseServiceCommandFactory();
        }
        else
        if (COMMAND_STOP_MOTION.equals(commandName))
        {
            serviceCommandFactory = new StopMotionServiceCommandFactory();
        }
        else
        if (COMMAND_STOP_ALL.equals(commandName))
        {
            serviceCommandFactory = new StopAllServiceCommandFactory();
        }
        else
        {
            throw new UnknownCommandException(commandName);
        }
        return serviceCommandFactory;
    }

    private void handleCliAlreadySelectedException(
        final AlreadySelectedException exception)
    {
        // TODO
    }

    private void handleCliMissingArgumentException(
        final MissingArgumentException exception)
    {
        // TODO
    }

    private void handleCliMissingOptionException(
        final MissingOptionException exception)
    {
        // TODO
    }

    private void handleCliUnrecognizedOptionException(
        final UnrecognizedOptionException exception)
    {
        // TODO
    }

    private void handleCliParseException(final ParseException exception)
    {
    }

    private void handleNoCommandException(final NoCommandException exception)
    {
        System.err.printf("No command was specified.%n");
    }

    private void handleUnknownCommandException(
        final UnknownCommandException exception)
    {
        final String commandName = exception.getCommandName();
        System.err.printf("Unknown command: %s%n", commandName);
    }

    private void handleMissingArgumentException(
        final replicatorg.app.service.MissingArgumentException exception)
    {
        final String commandName = exception.getCommandName();
        final String argumentName = exception.getArgumentName();
        System.err.printf(
            "The \"%s\" command is missing a required argument: %s%n",
            commandName, argumentName);
    }

    private void handleExtraArgumentsException(
        final ExtraArgumentsException exception)
    {
        final String commandName = exception.getCommandName();
        final Iterable<String> extraArguments
            = exception.getExtraArguments();
        final String extraArgumentsString
            = StringUtils.join(extraArguments, " ");
        System.err.printf(
            "The \"%s\" command received extra unsupported arguments: %s%n", commandName,
            extraArgumentsString);
    }

    private static final String COMMAND_BUILD = "build";

    private static final String COMMAND_PAUSE = "pause";

    private static final String COMMAND_UNPAUSE = "unpause";

    private static final String COMMAND_STOP_MOTION = "stopMotion";

    private static final String COMMAND_STOP_ALL = "stopAll";
}
