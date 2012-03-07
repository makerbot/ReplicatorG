// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

public class MissingArgumentException extends Exception
{
    private final String commandName;

    private final String argumentName;

    public MissingArgumentException(
        final String commandName,
        final String argumentName)
    {
        this.commandName = commandName;
        this.argumentName = argumentName;
    }

    public String getCommandName()
    {
        return this.commandName;
    }

    public String getArgumentName()
    {
        return this.argumentName;
    }
}
