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
import replicatorg.app.util.serial.Name;
import replicatorg.app.util.serial.Serial;

public class PortSelectionPanel extends JPanel {
	interface PortSelectionListener {
		public void portSelected(String port);
	}
	
	class SerialListModel extends AbstractListModel {
		public Vector<Name> names = Serial.scanSerialNames();
		public Object getElementAt(int idx) {
			return names.elementAt(idx);
		}
		public int getSize() {
			return names.size();
		}			
	}

	public PortSelectionPanel(final PortSelectionListener listener)
	{
		setLayout(new MigLayout("fill","","[grow 0][grow 100]"));
		add(new JLabel("Select the serial port to use:"),"growy 0,wrap");
		final JList list = new JList(new SerialListModel());
		list.setFixedCellHeight(30);
		final JScrollPane scrollPane = new JScrollPane(list);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lse) {
				if (list.getSelectedIndex() != -1) {
					Name name = (Name)list.getModel().getElementAt(list.getSelectedIndex());
					String portName = name.getName();
					listener.portSelected(portName);
				}
			}
		});
		add(scrollPane,"growx,growx");		
	}
}
