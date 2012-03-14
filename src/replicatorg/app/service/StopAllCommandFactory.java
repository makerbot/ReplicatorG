// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

public class StopAllCommandFactory implements CommandFactory
{
    public boolean isMatch(final String commandName)
    {
        final boolean result = "stopAll".equals(commandName);
        return result;
    }

    public Command createCommand(final List<String> arguments)
        throws ExtraArgumentsException
    {
        if (0 != arguments.size())
        {
            throw new ExtraArgumentsException("stopAll", arguments);
        }
        else
        {
            return null;
        }
    }
}
