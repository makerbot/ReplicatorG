package replicatorg.uploader.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;
import replicatorg.uploader.AbstractFirmwareUploader;
import replicatorg.uploader.FirmwareVersion;

class UploaderDialog extends JDialog implements ActionListener {
	JButton nextButton;
	JPanel centralPanel;
	FirmwareUploader uploader;
	
	enum State {
		SELECTING_BOARD,
		SELECTING_FIRMWARE,
		SELECTING_PORT,
		UPLOADING
	};
	
	State state;

	public UploaderDialog(Frame parent, FirmwareUploader uploader) {
		super(parent,"Firmware upgrade",true);
		this.uploader = uploader;
		centralPanel = new JPanel(new BorderLayout());
		Container c = getContentPane();
		c.setLayout(new MigLayout());
		Dimension panelSize = new Dimension(500,180);
		centralPanel.setMinimumSize(panelSize);
		centralPanel.setMaximumSize(panelSize);
		c.add(centralPanel,"wrap,spanx");
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doCancel();
			}
		});
		c.add(cancelButton,"tag cancel");
		nextButton = new JButton("Next >");
		nextButton.setEnabled(false);
		nextButton.addActionListener(this);
		c.add(nextButton,"tag ok");
		doBoardSelection();
		pack();
		setLocationRelativeTo(parent);
	}
	public void showPanel(JComponent panel) {
		centralPanel.removeAll();
		centralPanel.setLayout(new BorderLayout());
		centralPanel.add(panel,BorderLayout.CENTER);
		panel.setVisible(true);
		pack();
	}


	public void actionPerformed(ActionEvent arg0) {
		if (state == State.SELECTING_BOARD) {
			doFirmwareSelection();
		} else if (state == State.SELECTING_FIRMWARE) {
			doPortSelection();
		} else if (state == State.SELECTING_PORT) {
			doUpload();
		}
	}

	public void doCancel() {
		dispose();
	}

	Node selectedBoard = null;

	void doBoardSelection() {
		state = State.SELECTING_BOARD;
		nextButton.setEnabled(false);
		BoardSelectionPanel boardPanel = new BoardSelectionPanel(uploader.getFirmwareDoc(),
				new BoardSelectionPanel.BoardSelectionListener() {
					public void boardSelected(Node board) {
						selectedBoard = board;
						nextButton.setEnabled(true);
					}
		});
		showPanel(boardPanel);
	}

	FirmwareVersion selectedVersion = null;

	void doFirmwareSelection() {
		state = State.SELECTING_FIRMWARE;
		nextButton.setEnabled(false);
		FirmwareSelectionPanel firmwarePanel = new FirmwareSelectionPanel(selectedBoard,
				new FirmwareSelectionPanel.FirmwareSelectionListener() {
					public void firmwareSelected(FirmwareVersion firmware) {
						selectedVersion = firmware;
						nextButton.setEnabled(true);
					}
		});
		showPanel(firmwarePanel);
	}

	String portName = null;

	void doPortSelection() {
		state = State.SELECTING_PORT;
		nextButton.setEnabled(false);
		PortSelectionPanel portPanel = new PortSelectionPanel(
				new PortSelectionPanel.PortSelectionListener() {
					public void portSelected(String port) {
						portName = port;
						nextButton.setEnabled(true);
					}
		});
		showPanel(portPanel);
	}

	void doUpload() {
		state = State.UPLOADING;
		nextButton.setEnabled(true);
		nextButton.setText("Upload");
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("fill"));
		panel.add(new JLabel("<html>Press the reset button on the target board and click the \"Upload\" button " +
				"to update the firmware.  Try to press the reset button as soon as you click \"Upload\".</html>"));
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
			boolean success = uploader.upload();
			if (success) {
				JOptionPane.showMessageDialog(this, 
						"Firmware update succeeded!", 
						"Firmware Uploaded",
						JOptionPane.INFORMATION_MESSAGE);
				doCancel();
			} else {
				JOptionPane.showMessageDialog(this, 
						"<html>The firmware update did not succeed.  Check the console for details.<br/>"+
						"You can click the \"Upload\" button to try again.</html>", 
						"Upload Failed",
						JOptionPane.ERROR_MESSAGE);
				
			}
		}
	}
}
