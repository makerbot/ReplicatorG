package replicatorg.uploader.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
		public void portConfirmed();
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
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getClickCount() == 2)
					listener.portConfirmed();
			}
		});
		list.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER)
				{
					listener.portConfirmed();
				} else if(arg0.getKeyCode() == KeyEvent.VK_UP) {
					list.setSelectedIndex(Math.max(list.getSelectedIndex(), 0));
				} else if(arg0.getKeyCode() == KeyEvent.VK_DOWN) {
					list.setSelectedIndex(Math.min(list.getSelectedIndex(), list.getModel().getSize()));
				}
			}
		});
		add(scrollPane,"growx,growx");		
	}
}
