package replicatorg.app.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import replicatorg.app.Base;
import replicatorg.app.Serial;
import replicatorg.drivers.AbstractFirmwareUploader;

public class FirmwareUploader extends Thread {
	/**
	 * The constructor is non-public; only one firmware uploader is permitted to be running at
	 * any given time.  To start the uploader, use "startUploader".
	 * @param parent A parent widget, used to construct the interfaces, etc.
	 */
	private FirmwareUploader(Frame parent)
	{
		this.parent = parent;
	}
	
	private static FirmwareUploader uploader;
	private Frame parent; 
	/**
	 * Start the uploader.
	 * @param parent A parent widget, used to construct the user interfaces.
	 */
	public static synchronized void startUploader(Frame parent) {
		if (uploader == null || !uploader.isAlive()) {
			uploader = new FirmwareUploader(parent);
			uploader.start();
		}
	}
	
	public void run() {
		// Load firmware.xml
		if (loadXml()) {
			UploaderDialog selector = new UploaderDialog();
			selector.setVisible(true);
		}
	}
	
	Document firmwareDoc = null;
	
	private boolean loadXml() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			try {
				File f = new File("firmware.xml");
				if (!f.exists()) {
					f = new File("firmware.xml.dist");
					if (!f.exists()) {
						Base.showError(
								"Firmware.xml Not Found",
								"The firmware description file 'firmware.xml' was not found.\n"
								+ "Make sure you're running ReplicatorG from the correct directory.",
								null);
						return false;
					}
				}
				try {
					 firmwareDoc = db.parse(f);
				} catch (SAXException e) {
					Base.showError("Parse error",
							"Error parsing firmware.xml.  You may need to reinstall ReplicatorG.",
							e);
					return false;
				}
			} catch (IOException e) {
				Base.showError(null, "Could not read firmware.xml.\n"
						+ "You may need to reinstall ReplicatorG.", e);
				return false;
			}
		} catch (ParserConfigurationException e) {
			Base.showError("Unkown error", "Unknown error parsing firmware.xml.", e);
			return false;
		}
		return true;
	}

	private class UploaderDialog extends JDialog {
		JButton nextButton;
		JPanel centralPanel;
		
		public UploaderDialog() {
			super(parent,"Firmware upgrade",true);
			centralPanel = new JPanel(new BorderLayout());
			Container c = getContentPane();
			c.setLayout(new MigLayout());
			Dimension panelSize = new Dimension(500,300);
			centralPanel.setMinimumSize(panelSize);
			centralPanel.setMaximumSize(panelSize);
			c.add(centralPanel,"wrap");
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					doCancel();
				}
			});
			c.add(cancelButton,"tag cancel");
			nextButton = new JButton("Next >");
			nextButton.setEnabled(false);
			c.add(nextButton,"tag ok");
			doBoardSelection();
			pack();
		}
		public void showPanel(JComponent panel) {
			centralPanel.removeAll();
			centralPanel.setLayout(new BorderLayout());
			centralPanel.add(panel,BorderLayout.CENTER);
			panel.setVisible(true);
			pack();
		}
		
		public void doCancel() {
			dispose();
		}
		public void doNext() {
			
		}
		
		class BoardListModel extends AbstractListModel {
			NodeList nl = firmwareDoc.getElementsByTagName("board");
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
		
		void doBoardSelection() {
			nextButton.setEnabled(false);
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("fill","","[grow 0][grow 100]"));
			panel.add(new JLabel("Select the board to upgrade:"),"growy 0,wrap");
			final BoardListModel blm = new BoardListModel();
			final JList list = new JList(blm);
			ImageIcon icon = new ImageIcon(Base.getImage("images/icon-board.png", this));
			list.setCellRenderer(new BoardListCellRenderer(icon));
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent lse) {
					nextButton.setEnabled(list.getSelectedIndex() != -1);
				}
			});
			nextButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (list.getSelectedIndex() != -1) {
						nextButton.removeActionListener(this);
						selectedBoard = (Node)blm.getElementAt(list.getSelectedIndex());
						doFirmwareSelection();
					}
				}
			});
			panel.add(list,"growx,growy");
			showPanel(panel);
		}

		class FirmwareVersion {
			public int major;
			public int minor;
			public String where;
			public FirmwareVersion(Node n) {
				major = Integer.parseInt(n.getAttributes().getNamedItem("major").getNodeValue());
				minor = Integer.parseInt(n.getAttributes().getNamedItem("minor").getNodeValue());
				where = n.getAttributes().getNamedItem("relpath").getNodeValue();
			}
			public String toString() {
				return "v" + Integer.toString(major) + "." + Integer.toString(minor);
			}
		}

		class FirmwareListModel extends AbstractListModel {
			Vector<FirmwareVersion> versions = new Vector<FirmwareVersion>();
			public FirmwareListModel() {
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

		FirmwareVersion selectedVersion = null;
		
		void doFirmwareSelection() {
			nextButton.setEnabled(false);
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("fill","","[grow 0][grow 100]"));
			panel.add(new JLabel("Select the firmware version to install:"),"growy 0,wrap");
			final JList list = new JList(new FirmwareListModel());
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent lse) {
					nextButton.setEnabled(list.getSelectedIndex() != -1);
				}
			});
			nextButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (list.getSelectedIndex() != -1) {
						selectedVersion = (FirmwareVersion)list.getModel().getElementAt(list.getSelectedIndex());
						nextButton.removeActionListener(this);
						doPortSelection();
					}
				}
			});
			panel.add(list,"growx,growy");
			showPanel(panel);
		}
		
		class SerialListModel extends AbstractListModel {
			public Vector<Serial.Name> names = Serial.scanSerialNames();
			public Object getElementAt(int idx) {
				return names.elementAt(idx);
			}
			public int getSize() {
				return names.size();
			}			
		}

		String portName = null;
		
		void doPortSelection() {
			nextButton.setEnabled(false);
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("fill","","[grow 0][grow 100]"));
			panel.add(new JLabel("Select the serial port to use:"),"growy 0,wrap");
			final JList list = new JList(new SerialListModel());
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent lse) {
					nextButton.setEnabled(list.getSelectedIndex() != -1);
				}
			});
			nextButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (list.getSelectedIndex() != -1) {
						Serial.Name name = (Serial.Name)list.getModel().getElementAt(list.getSelectedIndex());
						portName = name.getName();
						nextButton.removeActionListener(this);
						doUpload();
					}
				}
			});
			panel.add(list,"growx,growy");
			showPanel(panel);
		}
		
		void doUpload() {
			nextButton.setEnabled(true);
			nextButton.setText("Upload");
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout("fill"));
			panel.add(new JLabel("<html>Press the reset button on the target board and click the \"next\" button to upload the firmware."));
			nextButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
						performUpload();
				}
			});
			showPanel(panel);
		}
				
		void performUpload() {
			Base.getMachine().dispose();
			AbstractFirmwareUploader uploader = null;
			NodeList nl = selectedBoard.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if ("programmer".equalsIgnoreCase(n.getNodeName())) {
					uploader = AbstractFirmwareUploader.makeUploader(n);
				}
			}
			if (uploader != null) {
				uploader.setPortName(portName);
				uploader.setSource(selectedVersion.where);
				uploader.upload();
			}
		}
	}
}
