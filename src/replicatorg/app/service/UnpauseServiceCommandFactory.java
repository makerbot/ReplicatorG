// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

public class UnpauseServiceCommandFactory implements ServiceCommandFactory
{
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
