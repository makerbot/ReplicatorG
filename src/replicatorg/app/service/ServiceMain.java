// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.StringUtils;

import replicatorg.app.Base;

public class ServiceMain
{
    public static void main(final String[] arguments)
    {
        final ServiceMain serviceMain = createServiceMain();
        final int status = serviceMain.run(arguments);
        System.exit(status);
    }

    private static ServiceMain createServiceMain()
    {
        final Iterable<ServiceCommandFactory> serviceCommandFactories
            = createServiceCommandFactories();
        final ServiceMain serviceMain
            = new ServiceMain(serviceCommandFactories);
        return serviceMain;
    }

    private static Iterable<ServiceCommandFactory>
        createServiceCommandFactories()
    {
        final List<ServiceCommandFactory> list
            = new ArrayList<ServiceCommandFactory>();
        list.add(new BuildServiceCommandFactory());
        list.add(new PauseServiceCommandFactory());
        list.add(new StopAllServiceCommandFactory());
        list.add(new StopMotionServiceCommandFactory());
        list.add(new UnpauseServiceCommandFactory());
        return list;
    }

    private final Iterable<ServiceCommandFactory> serviceCommandFactories;

    private ServiceMain(
        final Iterable<ServiceCommandFactory> serviceCommandFactories)
    {
        this.serviceCommandFactories = serviceCommandFactories;
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
            handleDebug(commandLine);
            handleHelp(commandLine, options);
            handleVersion(commandLine);
            if (false == commandLine.hasOption("help")
                && false == commandLine.hasOption("version"))
            {
                handleAlternatePrefs(commandLine);
                executeCommandLine(commandLine);
            }
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
        options.addOption(OptionBuilder
            .withLongOpt("help")
            .withDescription("print this message")
            .create());
        options.addOption(OptionBuilder
            .withLongOpt("version")
            .withDescription("print the version information")
            .create());
        options.addOption(OptionBuilder
            .withLongOpt("alternate-prefs")
            .hasArg()
            .withArgName("ALTERNATE_PREFS_NAME")
            .withDescription("use alternate preferences")
            .create());
        options.addOption(OptionBuilder
            .withLongOpt("debug")
            .hasArg()
            .withArgName("DEBUGLEVEL")
            .withDescription("set the debug level")
            .create());
        return options;
    }

    private void handleDebug(final CommandLine commandLine)
    {
        if (commandLine.hasOption("debug"))
        {
            Level level = Level.FINER;
            final String value = commandLine.getOptionValue("debug");
            try
            {
                final int number = Integer.parseInt(value);
                switch (number)
                {
                case 0:
                    level = Level.INFO;
                    break;
                case 1:
                    level = Level.FINE;
                    break;
                case 2:
                    level = Level.FINER;
                    break;
                case 3:
                    level = Level.FINEST;
                    break;
                default:
                    if (number >= 4)
                    {
                        level = Level.ALL;
                    }
                    break;
                }
            }
            catch (final NumberFormatException nfException)
            {
                try
                {
                    level = Level.parse(value);
                }
                catch (final IllegalArgumentException iaException)
                {
                    // Ignored. The level will remain at the default specified
                    // at the top of this method.
                }
            }
            Base.logger.setLevel(level);
            Base.logger.info("Debug level is '" + level + "'");
        }
    }

    private void handleHelp(final CommandLine commandLine,
        final Options options)
    {
        if (commandLine.hasOption("help"))
        {
            printHelp(commandLine, options);
        }
    }

    private void printHelp(final CommandLine commandLine,
        final Options options)
    {
        final HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(
            "replicatorg [global-options...] COMMAND [command-options...]",
            "", options, "", false);
        System.out.println("The supported commands are:");
        System.out.println("");
        System.out.println("  build         build");
        System.out.println("  pause         pause the machine");
        System.out.println("  unpause       unpause the machine");
        System.out.println("  stopMotion    stop machine motion");
        System.out.println("  stopAll       stop the machine");
    }

    private String getFooter()
    {
        final String footer = String.format(
            "The supported commands are:%n"
            + "%n"
            + "  build         build%n"
            + "  pause         pause the machine%n"
            + "  unpause       unpause the machine%n"
            + "  stopMotion    stop machine motion%n"
            + "  stopAll       stop the machine%n");
        return footer;
    }

    private void handleVersion(final CommandLine commandLine)
    {
        if (commandLine.hasOption("version"))
        {
            printVersion();
        }
    }

    private void printVersion()
    {
        System.out.printf("replicatorg version %s%n", Base.VERSION_NAME);
    }

    private void handleAlternatePrefs(final CommandLine commandLine)
    {
        if (commandLine.hasOption("alternate-prefs"))
        {
            final String alternatePrefs
                = commandLine.getOptionValue("alternate-prefs");
            Base.setAlternatePrefs(alternatePrefs);
        }
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
        for (final ServiceCommandFactory serviceCommandFactory
            : this.serviceCommandFactories)
        {
            if (serviceCommandFactory.isMatch(commandName))
            {
                return serviceCommandFactory;
            }
        }
        throw new UnknownCommandException(commandName);
    }

    private void handleCliAlreadySelectedException(
        final AlreadySelectedException exception)
    {
        //
        // We don't have any option groups right now so this method does not
        // bother to provide a user-friendly error message.
        //

        exception.printStackTrace();
        printErrorFooter();
    }

    private void handleCliMissingArgumentException(
        final MissingArgumentException exception)
    {
        final Option option = exception.getOption();
        final String optionName = getOptionName(option);
        final String argName = option.getArgName();
        System.out.printf(
            "The '%s' option is missing its required '%s' argument.%n",
            optionName, argName);
        printErrorFooter();
    }

    private void handleCliMissingOptionException(
        final MissingOptionException exception)
    {
        final List missingOptions = exception.getMissingOptions();
        final String missingOptionsString
            = StringUtils.join(missingOptions, ", ");
        System.err.printf(
            "One or more required options where not provided: %s%n",
            missingOptionsString);
        printErrorFooter();
    }

    private void handleCliUnrecognizedOptionException(
        final UnrecognizedOptionException exception)
    {
        final String option = exception.getOption();
        System.err.printf("Unrecognized option '%s'.%n", option);
        printErrorFooter();
    }

    private void handleCliParseException(final ParseException exception)
    {
        final String message = exception.getMessage();
        System.err.printf(
            "Error while processing command-line arguments: %s%n", message);
        printErrorFooter();
    }

    private void handleNoCommandException(final NoCommandException exception)
    {
        System.err.printf("No command was specified.%n");
        printErrorFooter();
    }

    private void handleUnknownCommandException(
        final UnknownCommandException exception)
    {
        final String commandName = exception.getCommandName();
        System.err.printf("Unknown command '%s'.%n", commandName);
        printErrorFooter();
    }

    private void handleMissingArgumentException(
        final replicatorg.app.service.MissingArgumentException exception)
    {
        final String commandName = exception.getCommandName();
        final String argumentName = exception.getArgumentName();
        System.err.printf(
            "The '%s' command is missing the required '%s' argument.%n",
            commandName, argumentName);
        printErrorFooter();
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
            "The '%s' command received extra unsupported arguments ('%s').%n",
            commandName, extraArgumentsString);
        printErrorFooter();
    }

    private String getOptionName(final Option option)
    {
        final String opt = option.getOpt();
        final String optionName;
        if (null != opt)
        {
            optionName = String.format("-%s", opt);
        }
        else
        {
            final String longOpt = option.getLongOpt();
            optionName = String.format("--%s", longOpt);
        }
        return optionName;
    }

    private void printErrorFooter()
    {
        System.err.println("Try 'replicatorg --help' for more information.");
    }
}
