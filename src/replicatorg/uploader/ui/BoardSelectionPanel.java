package replicatorg.uploader.ui;

import java.awt.Component;

import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;

public class BoardSelectionPanel extends JPanel {
	interface BoardSelectionListener {
		public void boardSelected(Node board);
	}
	
	class BoardListModel extends AbstractListModel {	
		NodeList nl ;
		BoardListModel(Document firmwareDoc) {
			nl = firmwareDoc.getElementsByTagName("board");
		}
		public Object getElementAt(int idx) {
			return nl.item(idx);
		}
		public int getSize() {
			return nl.getLength();
		}			
	}

	class BoardListCellRenderer extends JLabel implements ListCellRenderer {
		public BoardListCellRenderer(Icon boardIcon) {
			setIcon(boardIcon);
		}
		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus) {
			Node node = (Node)value;
			String name = node.getAttributes().getNamedItem("name").getNodeValue();
			StringBuffer versions = new StringBuffer();
			NodeList nl = node.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if ("version".equalsIgnoreCase(n.getNodeName())) {
					if (versions.length() != 0) { versions.append(", "); }
					versions.append("v");
					versions.append(n.getAttributes().getNamedItem("major").getNodeValue());
					versions.append(".");
					versions.append(n.getAttributes().getNamedItem("minor").getNodeValue());
				}
			}
			setText("<html>"+name+"<br/><font color=\"gray\" size=\"-2\">"+versions.toString()+"</font></html>");
			setBackground(isSelected?list.getSelectionBackground():list.getBackground());
			setForeground(isSelected?list.getSelectionForeground():list.getForeground());
			setOpaque(isSelected);
			return this;
		}
	}

	Node selectedBoard = null;
	
	public Node getSelectedBoard() { return selectedBoard; }
	
	public BoardSelectionPanel(Document firmwareDoc, final BoardSelectionListener listener) {
		setLayout(new MigLayout("fill","","[grow 0][grow 100]"));
		add(new JLabel("Select the board to upgrade:"),"growy 0,wrap");
		final BoardListModel blm = new BoardListModel(firmwareDoc);
		final JList list = new JList(blm);
		final JScrollPane scrollPane = new JScrollPane(list);
		ImageIcon icon = new ImageIcon(Base.getImage("images/icon-board.png", this));
		list.setCellRenderer(new BoardListCellRenderer(icon));
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lse) {
				selectedBoard = (Node)blm.getElementAt(list.getSelectedIndex());
				if (listener != null) { listener.boardSelected(selectedBoard); }
			}
		});
		add(scrollPane,"growx,growx");
	}

}
