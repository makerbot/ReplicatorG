// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.util.List;

import org.apache.commons.cli.ParseException;

interface CommandFactory
{
    boolean isMatch(String commandName);

    Command createCommand(List<String> arguments) throws ParseException,
        MissingArgumentException, ExtraArgumentsException;
}
