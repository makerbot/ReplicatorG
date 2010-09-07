package replicatorg.uploader.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import replicatorg.app.Base;
import replicatorg.app.MachineController;
import replicatorg.uploader.AbstractFirmwareUploader;
import replicatorg.uploader.FirmwareUploader;
import replicatorg.uploader.FirmwareVersion;

public class UploaderDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -401692780113162450L;
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
		BoardSelectionPanel boardPanel = new BoardSelectionPanel(FirmwareUploader.getFirmwareDoc(),
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

	AbstractFirmwareUploader createUploader() {
		MachineController machine = Base.getMachine();
		if (machine != null) { machine.dispose(); }
		NodeList nl = selectedBoard.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if ("programmer".equalsIgnoreCase(n.getNodeName())) {
				return AbstractFirmwareUploader.makeUploader(n);
			}
		}
		return null;
	}
	
	void doUpload() {
		final AbstractFirmwareUploader uploader = createUploader();
		state = State.UPLOADING;
		nextButton.setEnabled(true);
		nextButton.setText("Upload");
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("fill"));
		panel.add(new JLabel("<html>"+uploader.getUploadInstructions()+"</html>"),"wrap");
		try {
			Method getWipe = uploader.getClass().getMethod("getWipe");
			final Method setWipe = uploader.getClass().getMethod("setWipe", Boolean.TYPE);
			Boolean v = (Boolean)getWipe.invoke(uploader);
			final JCheckBox cbox = new JCheckBox("erase current EEPROM settings", v.booleanValue());
			cbox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						setWipe.invoke(uploader, new Boolean(cbox.isSelected()));
					} catch (IllegalArgumentException e1) {
						e1.printStackTrace();
					} catch (IllegalAccessException e1) {
						e1.printStackTrace();
					} catch (InvocationTargetException e1) {
						e1.printStackTrace();
					}
				}
			});
			panel.add(cbox,"wrap");
			final String text = "Warning: this will reset any onboard parameters for this board to their default values.";
			panel.add(new JLabel("<html><i>"+text+"</i></html>"),"wrap");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				performUpload(uploader);
			}
		});
		showPanel(panel);
	}

	void performUpload(AbstractFirmwareUploader uploader) {
		if (uploader != null) {
			uploader.setPortName(portName);
			String path = Base.getUserFile(selectedVersion.getRelPath()).getAbsolutePath();
			uploader.setSource(path);
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
