// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

import org.apache.commons.cli.ParseException;

interface ServiceCommandFactory
{
    boolean isMatch(String commandName);

    ServiceCommand createServiceCommand(List<String> arguments)
        throws ParseException, MissingArgumentException,
            ExtraArgumentsException;
}
