// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

public class ExtraArgumentsException extends Exception
{
    private final String commandName;

    private final List<String> extraArguments;

    public ExtraArgumentsException(
        final String commandName,
        final List<String> extraArguments)
    {
        this.commandName = commandName;
        this.extraArguments = extraArguments;
    }

    public String getCommandName()
    {
        return this.commandName;
    }

    public List<String> getExtraArguments()
    {
        return this.extraArguments;
    }
}
