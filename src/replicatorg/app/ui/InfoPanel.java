package replicatorg.app.ui;

import java.io.StringWriter;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Node;

import replicatorg.app.Base;
import replicatorg.drivers.Driver;
import replicatorg.drivers.OnboardParameters;
import replicatorg.drivers.OnboardParameters.CommunicationStatistics;
import replicatorg.machine.MachineFactory;

public class InfoPanel extends JFrame {
    JTextArea infoArea;
	
	public InfoPanel() {
		super("About this Makerbot");
		JPanel panel = new JPanel(new MigLayout());

		infoArea = new JTextArea("ReplicatorG:",40,60);
		infoArea.setFont(infoArea.getFont().deriveFont(11f));
		panel.add(new JScrollPane(infoArea));
		
		add(panel);
		pack();
		
		infoArea.setText(getMachineInfo());
	}
	
	// From here:
	// http://tech.chitgoks.com/2010/05/06/convert-org-w3c-dom-node-to-string-using-java/
	public static String convertNodeToHtml(Node node) {
		try {
			Transformer t;
			t = TransformerFactory.newInstance().newTransformer();
		    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		    StringWriter sw = new StringWriter();
			t.transform(new DOMSource(node), new StreamResult(sw));
		    return sw.toString();
		} catch (TransformerConfigurationException e) {
		} catch (TransformerFactoryConfigurationError e) {
		} catch (TransformerException e) {
		}
		
		return null;
	}
	
	public String getMachineInfo() {
		Driver driver = Base.getMachineLoader().getDriver();
		
		String info = new String();
		
		info += "System Information" + "\n";
		info += " ReplicatorG version: " + Base.VERSION_NAME + "\n";
		info += " Java version: " + System.getProperty("java.version") + "\n";
		
		info += "\n";
		info += "Machine" + "\n";
		info += " Profile Name: " + Base.preferences.get("machine.name", "") + "\n";
		info += " Driver Type: " + Base.getMachineLoader().getDriver().getDriverName() + "\n";
		info += " Name: " + Base.getMachineLoader().getMachine().getMachineName() + "\n";

		// TODO: Only if a printer is connected?
		info += " Motherboard firmware version: " + driver.getFirmwareInfo() + "\n";
		// Status dump
		
		// Communication Statistics
		if (driver instanceof OnboardParameters) {
			CommunicationStatistics stats = ((OnboardParameters)driver).getCommunicationStatistics();
			info += " Motherboard communication statistics" + "\n";
			info += "  Number of packets received from the USB interface:" + stats.packetCount + "\n";
			info += "  Number of packets sent over the RS485 interface:" + stats.sentPacketCount + "\n";
			info += "  Number of packets sent over the RS485 interface that were not responded to:" + stats.packetFailureCount + "\n";
			info += "  Number of packet retries attempted:" + stats.packetRetryCount + "\n";
			info += "  Number of bytes received over the RS485 interface that were discarded as noise:" + stats.noiseByteCount + "\n";
		}
		// EEPROM dump
		
		// Toolhead info (per toolhead)
		
		// Default skeinforge version/profile info?
		
        // Machine Driver XML dump	
		info += "\n";
		info += "Machine Driver XML:" + "\n";
		Node machineNode = MachineFactory.getMachineNode(Base.preferences.get("machine.name", ""));
		if (machineNode != null) {
			info += convertNodeToHtml(machineNode) + "\n";
		}
		
		// Test communication
		
		return info;
	}
}
