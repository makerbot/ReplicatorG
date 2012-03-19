// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import com.makerbot.alpha.Printer1;

public class StopAllCommand extends RemoteCommand
{
    public StopAllCommand(final String busName)
    {
        super(busName);
    }

    public void executeRemoteCommand(final Printer1 printer)
    {
        printer.StopAll();
    }
}
