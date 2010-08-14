package testing.fabman;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import fabman.queue.FabServer;

public class FabServerTest {

	static final String xmlStub = "<?xml version=\"1.1\" encoding=\"utf-8\"?><machines></machines>";

	@Test
	public void createTestServer() throws URISyntaxException, IOException, SAXException, ParserConfigurationException {
		FabServer server = new FabServer(xmlStub);
		URI serverUri = server.getServerURI();
		assert serverUri != null;
		assert "localhost".equals(serverUri.getHost());
		assert serverUri.getPort() > 0;
		assert "fab".equals(serverUri.getScheme());
	}
	
	@Test
	public void createTestServerProcess() throws IOException, URISyntaxException {
		
		FabServer.ServerInfo info = FabServer.startServer(xmlStub);
		URI serverUri = info.socketUri;
		assert serverUri != null;
		assert "localhost".equals(serverUri.getHost());
		assert serverUri.getPort() > 0;
		assert "fab".equals(serverUri.getScheme());
		info.process.destroy();
	}
}
