// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import com.makerbot.Printer;

public class PauseCommand extends RemoteCommand
{
    public PauseCommand(final String busName)
    {
        super(busName);
    }

    @Override
    protected void executeRemoteCommand(final Printer printer)
    {
        printer.Pause();
    }
}
