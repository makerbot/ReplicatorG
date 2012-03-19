// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import com.makerbot.alpha.Printer1;

public class BuildCommand extends RemoteCommand
{
    private final String filename;

    public BuildCommand(final String busName, final String filename)
    {
        super(busName);
        this.filename = filename;
    }

    @Override
    protected void executeRemoteCommand(final Printer1 printer)
    {
        printer.Build(this.filename);
    }
}
