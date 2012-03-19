// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import com.makerbot.alpha.Printer1;

public class PauseCommand extends RemoteCommand
{
    public PauseCommand(final String busName)
    {
        super(busName);
    }

    @Override
    protected void executeRemoteCommand(final Printer1 printer)
    {
        printer.Pause();
    }
}
