// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

public class BuildServiceCommandFactory implements ServiceCommandFactory
{
    public ServiceCommand createServiceCommand(final List<String> arguments)
        throws MissingArgumentException, ExtraArgumentsException
    {
        if (0 == arguments.size())
        {
            throw new MissingArgumentException("build", "filename");
        }
        else
        if (arguments.size() > 1)
        {
            final List<String> extraArguments
                = arguments.subList(1, arguments.size());
            throw new ExtraArgumentsException("build", extraArguments);
        }
        else
        {
            final String filename = arguments.get(0);
            final ServiceCommand serviceCommand
                = new BuildServiceCommand(filename);
            return serviceCommand;
        }
    }
}
