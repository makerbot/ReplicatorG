package replicatorg.uploader.ui;

import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.uploader.FirmwareVersion;

public class FirmwareSelectionPanel extends JPanel {
	private static final long serialVersionUID = -7960056942500494572L;

	interface FirmwareSelectionListener {
		public void firmwareSelected(FirmwareVersion firmware);
	}
	
	class FirmwareListModel extends AbstractListModel {
		private static final long serialVersionUID = 3534926635920932686L;
		
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

	final JLabel description = new JLabel("<html><font color=\"gray\">Select a firmware version.</font></html>");

	public FirmwareSelectionPanel(Node selectedBoard, final FirmwareSelectionListener listener) {
		setLayout(new MigLayout("fill","","[grow 0][grow 100]"));
		add(new JLabel("Select the firmware version to install:"),"growy 0,wrap");
		final JList list = new JList(new FirmwareListModel(selectedBoard));
		list.setFixedCellHeight(30);
		final JScrollPane scrollPane = new JScrollPane(list);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lse) {
				if (list.getSelectedIndex() != -1) {
					FirmwareVersion selectedVersion = 
						(FirmwareVersion)list.getModel().getElementAt(list.getSelectedIndex());
					listener.firmwareSelected(selectedVersion);
					String descr = selectedVersion.getDescription();
					if (descr == null) { descr = "<html><font color=\"gray\">No description available.</font></html>"; }
					else { descr = "<html>"+descr+"</html>"; }
					FirmwareSelectionPanel.this.description.setText(descr);
				}
			}
		});
		add(scrollPane,"width 50%");
		add(description,"width 50%");
	}
}
