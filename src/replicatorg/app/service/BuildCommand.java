// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import com.makerbot.Printer;

public class BuildCommand extends RemoteCommand
{
    private final String filename;

    public BuildCommand(final String busName, final String filename)
    {
        super(busName);
        this.filename = filename;
    }

    @Override
    protected void executeRemoteCommand(final Printer printer)
    {
        printer.Build(this.filename);
    }
}
