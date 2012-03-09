// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

public class NoFileException extends Exception
{
    private final String filename;

    public NoFileException(final String filename)
    {
        this.filename = filename;
    }

    public String getFilename()
    {
        return this.filename;
    }
}
