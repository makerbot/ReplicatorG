// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

public class UnpauseServiceCommandFactory implements ServiceCommandFactory
{
    public boolean isMatch(final String commandName)
    {
        final boolean result = "unpause".equals(commandName);
        return result;
    }

    public ServiceCommand createServiceCommand(final List<String> arguments)
        throws ExtraArgumentsException
    {
        if (0 != arguments.size())
        {
            throw new ExtraArgumentsException("unpause", arguments);
        }
        else
        {
            return null;
        }
    }
}
