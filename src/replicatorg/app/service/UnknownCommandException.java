// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

public class UnknownCommandException extends Exception
{
    private final String commandName;

    public UnknownCommandException(final String commandName)
    {
        this.commandName = commandName;
    }

    public String getCommandName()
    {
        return this.commandName;
    }
}
