package fabman.queue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A FabServer is a socket server that manages connections to a fabricator and is responsible
 * for handling and responding to messages.
 * 
 * FabServers are configured by piping an XML descriptor in to stdin on startup, and return the
 * URL of the server.
 * 
 * By default FabServers only accept local connections.
 * 
 * @author phooky
 *
 */
public class FabServer {
	//
	ServerSocket socket;
	
	private void init(InputSource source) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(source);
		socket = new ServerSocket();
		socket.bind(null);
	}

	public FabServer(String configuration) throws IOException, SAXException, ParserConfigurationException {
		init(new InputSource(new StringReader(configuration)));
	}

	public FabServer(InputStream configuration) throws IOException, SAXException, ParserConfigurationException {
		init(new InputSource(configuration));
	}

	public URI getServerURI() throws URISyntaxException {
		return new URI("fab://localhost:"+Integer.toString(socket.getLocalPort()));
	}
	
	/**
	 * Start a new FabServer with the given configuration, and return the URI representing
	 * the bound socket.
	 * @param configuration the XML configuration string that describes the fabricator to drive
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public static URI startServer(String configuration) throws IOException, URISyntaxException {
		String javaPath = System.getProperty("java.home") + File.separator +
			"bin" + File.separator +
			"java";
		String classPath = System.getProperty("java.class.path");
		ProcessBuilder pb = new ProcessBuilder(javaPath, "-cp", classPath, FabServer.class.getCanonicalName());
		Process proc = pb.start();
		PrintStream outStream = new PrintStream( proc.getOutputStream() );
		outStream.print(configuration);
		outStream.close();
		Reader reader = new InputStreamReader(proc.getInputStream());
		StringWriter writer = new StringWriter();
		for (int c = reader.read(); c != -1; c = reader.read()) {
			writer.append((char)c);
		}
		return new URI(writer.toString());
	}
	
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
		FabServer server = new FabServer(System.in);
		System.out.print(server.getServerURI().toString());
		System.out.close();
		while (true) {}
	}
}
