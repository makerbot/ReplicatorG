package replicatorg.uploader.ui;

import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.uploader.FirmwareVersion;

public class FirmwareSelectionPanel extends JPanel {
	interface FirmwareSelectionListener {
		public void firmwareSelected(FirmwareVersion firmware);
	}
	
	class FirmwareListModel extends AbstractListModel {
		Vector<FirmwareVersion> versions = new Vector<FirmwareVersion>();
		public FirmwareListModel(final Node selectedBoard) {
			NodeList nl = selectedBoard.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if ("firmware".equalsIgnoreCase(n.getNodeName())) {
					versions.add(new FirmwareVersion(n));
				}
			}
		}
		public Object getElementAt(int idx) {
			return versions.elementAt(idx);
		}
		public int getSize() {
			return versions.size();
		}
	}
	
	public FirmwareSelectionPanel(Node selectedBoard, final FirmwareSelectionListener listener) {
		setLayout(new MigLayout("fill","","[grow 0][grow 100]"));
		add(new JLabel("Select the firmware version to install:"),"growy 0,wrap");
		final JList list = new JList(new FirmwareListModel(selectedBoard));
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lse) {
				if (list.getSelectedIndex() != -1) {
					FirmwareVersion selectedVersion = 
						(FirmwareVersion)list.getModel().getElementAt(list.getSelectedIndex());
					listener.firmwareSelected(selectedVersion);
				}
			}
		});
		add(list,"growx,growy");
	}
}
