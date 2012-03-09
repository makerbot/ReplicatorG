// vim:cindent:cino=\:0:et:fenc=utf-8:ff=unix:sw=4:ts=4:

package replicatorg.app.service;

import java.io.IOException;

interface ServiceCommand
{
    void execute(ServiceContext serviceContext) throws IOException,
        NoFileException, NoMachineInterfaceException, NoPortException,
        NotConnectedException, NotReadyException, TimeoutException;
}
