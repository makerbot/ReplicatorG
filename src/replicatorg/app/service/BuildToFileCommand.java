// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import com.makerbot.alpha.Printer1;

public class BuildToFileCommand extends RemoteCommand
{
    private final String inputFilename;

    private final String outputFilename;

    public BuildToFileCommand(final String busName, final String inputFilename,
        final String outputFilename)
    {
        super(busName);
        this.inputFilename = inputFilename;
        this.outputFilename = outputFilename;
    }

    @Override
    protected void executeRemoteCommand(final Printer1 printer)
    {
        printer.BuildToFile(this.inputFilename, this.outputFilename);
    }
}
