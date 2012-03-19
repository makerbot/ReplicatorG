package replicatorg.uploader.ui;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;

public class BoardSelectionPanel extends JPanel {
	interface BoardSelectionListener {
		public void boardSelected(Node board);
		public void boardConfirmed();
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
		Icon boardIcon;
		public BoardListCellRenderer(Icon boardIcon) {
			this.boardIcon = boardIcon;
		}
		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus) {
			Element e = (Element)value;
			String name = e.getAttribute("name");
			String iconStr = e.getAttribute("icon");
			if (iconStr != null && !(iconStr.length() == 0)) {
				ImageIcon icon = new ImageIcon(Base.getImage("images/"+iconStr, this));
				setIcon(icon);
			} else {
				setIcon(boardIcon);
			}
			StringBuffer versions = new StringBuffer();
			NodeList nl = e.getElementsByTagName("version");
			for (int i = 0; i < nl.getLength(); i++) {
				Element ve = (Element)nl.item(i);
				if (versions.length() != 0) { versions.append(", "); }
				versions.append("v");
				versions.append(ve.getAttribute("major"));
				versions.append(".");
				versions.append(ve.getAttribute("minor"));
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
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(arg0.getClickCount() == 2)
					listener.boardConfirmed();
			}
		});
		list.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER)
				{
					listener.boardConfirmed();
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
