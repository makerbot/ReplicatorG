/*
 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2008 Zach Smith

 Forked from Arduino: http://www.arduino.cc

 Based on Processing http://www.processing.org
 Copyright (c) 2004-05 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.drivers.SDCardCapture;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

/**
 * run/stop/etc buttons for the ide
 */
public class MainButtonPanel extends BGPanel implements MachineListener, ActionListener {

	// / height, width of the toolbar buttons
	static final int BUTTON_WIDTH = 27;
	static final int BUTTON_HEIGHT = 32;
	
	static final float disabledFactors[] = { 1.0f, 1.0f, 1.0f, 0.5f };
	static final float disabledOffsets[] = { 0.0f, 0.0f, 0.0f, 0.0f };
	static private RescaleOp disabledOp = new RescaleOp(disabledFactors,disabledOffsets,null);
	static final float activeFactors[] = { -1.0f, -1.0f, -1.0f, 1.0f };
	static final float activeOffsets[] = { 1.0f, 1.0f, 1.0f, 0.0f };
	static private RescaleOp activeOp = new RescaleOp(activeFactors,activeOffsets,null);

	class MainButton extends JButton implements ChangeListener {
		private String rolloverText; 
		public MainButton(String rolloverText,
				Image active,
				Image inactive,
				Image rollover,
				Image disabled) {
			this.rolloverText = rolloverText;
			setIcon(new ImageIcon(inactive));
			setSelectedIcon(new ImageIcon(active));
			setDisabledIcon(new ImageIcon(disabled));
			setRolloverEnabled(true);
			setRolloverIcon(new ImageIcon(rollover));
			setSize(new Dimension(active.getWidth(null),active.getHeight(null)));
			setBorder(null);
			getModel().addChangeListener(this);
			addActionListener(MainButtonPanel.this);
		}
		public String getRolloverText() { return rolloverText; }
		public void paint(Graphics g) {
			final Rectangle b = getBounds(); 
			if (getModel().isSelected()) {
				g.setColor(new Color(1.0f,1.0f,0.5f,0.8f));
				g.fillRect(0,0,b.width,b.height);
				getSelectedIcon().paintIcon(this,g,0,0);
			} else if (getModel().isEnabled()) {
				if (getModel().isPressed()) {
					g.setColor(new Color(1.0f,1.0f,0.5f,0.3f));
					g.fillRect(0,0,b.width,b.height);
					getSelectedIcon().paintIcon(this, g, 0, 0);
				} else if (getModel().isRollover()) {
					g.setColor(new Color(1.0f,1.0f,0.5f,0.3f));
					g.fillRect(0,0,b.width,b.height);
					getRolloverIcon().paintIcon(this, g, 0, 0);
				} else {
					getIcon().paintIcon(this,g,0,0);
				}
			} else {
				getDisabledIcon().paintIcon(this, g, 0, 0);
			}
		}
		public void stateChanged(ChangeEvent ce) {
			// It's possible to get a change event before the status label is initialized 
			if (statusLabel == null) return;
			if (getModel().isRollover()) {
				statusLabel.setText(getRolloverText());
			} else {
				statusLabel.setText("");
			}
		}
	}
	

	// / amount of space between groups of buttons on the toolbar
	static final int BUTTON_GAP = 15;

	MainWindow editor;

	Image offscreen;

	int maxLabelWidth;
	int width, height;

	Color bgcolor;

	JLabel statusLabel;

	MainButton simButton, pauseButton, stopButton;
	MainButton buildButton, resetButton, cpButton;
	MainButton disconnectButton;
	
	MainButton uploadButton, playbackButton;
	
	public MainButtonPanel(MainWindow editor) {
		setLayout(new MigLayout("gap 5"));
		this.editor = editor;

		// hardcoding new blue color scheme for consistency with images,
		// see EditorStatus.java for details.
		// bgcolor = Preferences.getColor("buttons.bgcolor");
		setBackground(new Color(0x5F, 0x73, 0x25));

		Font statusFont = Base.getFontPref("buttons.status.font","SansSerif,plain,12");
		Color statusColor = Base.getColorPref("buttons.status.color","#FFFFFF");

		simButton = makeButton("Simulate", "images/button-simulate.png");
		add(simButton);
		buildButton = makeButton("Build", "images/button-build.png");
		add(buildButton);
		uploadButton = makeButton("Upload to SD card", "images/button-upload.png");
		add(uploadButton);
		playbackButton = makeButton("Build from SD card", "images/button-playback.png");
		add(playbackButton);

		pauseButton = makeButton("Pause", "images/button-pause.png");
		add(pauseButton,"gap unrelated");
		stopButton = makeButton("Stop", "images/button-stop.png");
		add(stopButton);


		cpButton = makeButton("Control panel", "images/button-control-panel.png");
		add(cpButton,"gap unrelated");
		
		resetButton = makeButton("Reset machine", "images/button-reset.png");
		add(resetButton,"gap unrelated");
		disconnectButton = makeButton("Disconnect machine", "images/button-disconnect.png");
		add(disconnectButton,"gap unrelated");
		
		

		statusLabel = new JLabel();
		statusLabel.setFont(statusFont);
		statusLabel.setForeground(statusColor);
		add(statusLabel, "gap unrelated");

		//setMaximumSize(new Dimension(3000,40));
		//setPreferredSize(new Dimension(300,40));
	}

	public MainButton makeButton(String rolloverText, String source) {
		BufferedImage img = Base.getImage(source, this);
		
		BufferedImage disabled = disabledOp.filter(img,null);
		Image inactive = img;
		Image rollover = img;
		Image active = activeOp.filter(img,null);
		
		MainButton mb = new MainButton(rolloverText, active, inactive, rollover, disabled);
		mb.setEnabled(false);
		return mb;
	}


	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == simButton) {
			editor.handleSimulate();
		} else if (e.getSource() == buildButton) {
			editor.handleBuild();
		} else if (e.getSource() == uploadButton) {
			editor.handleUpload();
		} else if (e.getSource() == playbackButton) {
			editor.handlePlayback();
		} else if (e.getSource() == pauseButton) {
			editor.handlePause();
		} else if (e.getSource() == stopButton) {
			editor.handleStop();
		} else if (e.getSource() == resetButton) {
			editor.handleReset();
		} else if (e.getSource() == cpButton) {
			editor.handleControlPanel();
		} else if (e.getSource() == disconnectButton) {
			editor.handleDisconnect();
		}
	}

	public void machineStateChanged(MachineStateChangeEvent evt) {
		MachineState s = evt.getState();
		boolean ready = s.isReady();
		boolean building = s.isBuilding();
		boolean paused = s.isPaused();
		boolean hasPlayback = (editor != null) &&
			(editor.machine != null) && 
			(editor.machine.driver != null) &&
			(editor.machine.driver instanceof SDCardCapture) &&
			(((SDCardCapture)editor.machine.driver).hasFeatureSDCardCapture());
		
		uploadButton.setVisible(hasPlayback);
		playbackButton.setVisible(hasPlayback);

		simButton.setEnabled(!building);
		buildButton.setEnabled(ready);
		uploadButton.setEnabled(ready);
		playbackButton.setEnabled(ready);
		pauseButton.setEnabled(building);
		stopButton.setEnabled(building);

		pauseButton.setSelected(paused);

		MachineState.Target runningTarget = s.isBuilding()?s.getTarget():null;
		
		simButton.setSelected(runningTarget == MachineState.Target.SIMULATOR);
		buildButton.setSelected(runningTarget == MachineState.Target.MACHINE);
		uploadButton.setSelected(runningTarget == MachineState.Target.SD_UPLOAD);
		playbackButton.setSelected(runningTarget == MachineState.Target.NONE);

		boolean connected = s.isConnected();
		resetButton.setEnabled(connected); 
		disconnectButton.setEnabled(connected);
		cpButton.setEnabled(ready);
	}

	public void machineProgress(MachineProgressEvent event) {
	}

	public void toolStatusChanged(MachineToolStatusEvent event) {
	}
}
