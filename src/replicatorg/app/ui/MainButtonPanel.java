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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

/**
 * run/stop/etc buttons for the ide
 */
public class MainButtonPanel extends JPanel implements MachineListener, ActionListener {

	// / height, width of the toolbar buttons
	static final int BUTTON_WIDTH = 27;
	static final int BUTTON_HEIGHT = 32;

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
			setSize(new Dimension(BUTTON_WIDTH,BUTTON_HEIGHT));
			setBorder(null);
			getModel().addChangeListener(this);
			addActionListener(MainButtonPanel.this);
		}
		public String getRolloverText() { return rolloverText; }
		public void paint(Graphics g) {
			if (getModel().isSelected()) {
				getSelectedIcon().paintIcon(this,g,0,0);
			} else if (getModel().isEnabled()) {
				if (getModel().isPressed()) {
					getSelectedIcon().paintIcon(this, g, 0, 0);
				} else if (getModel().isRollover()) {
					getRolloverIcon().paintIcon(this, g, 0, 0);
				} else {
					getIcon().paintIcon(this,g,0,0);
				}
			} else {
				getDisabledIcon().paintIcon(this, g, 0, 0);
			}
		}
		public void stateChanged(ChangeEvent ce) {
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
	MainButton buildButton, newButton, openButton;
	MainButton saveButton;
	
	MainButton uploadButton, playbackButton;
	
	public MainButtonPanel(MainWindow editor) {
		super(new MigLayout("gap 0"));
		this.editor = editor;

		BufferedImage src = Base.getImage("images/buttons.png", this);

		// hardcoding new blue color scheme for consistency with images,
		// see EditorStatus.java for details.
		// bgcolor = Preferences.getColor("buttons.bgcolor");
		setBackground(new Color(0x5F, 0x73, 0x25));

		Font statusFont = Base.getFontPref("buttons.status.font","SansSerif,plain,12");
		Color statusColor = Base.getColorPref("buttons.status.color","#FFFFFF");

		simButton = makeButton("Simulate", src, 0);
		add(simButton);
		pauseButton = makeButton("Pause", src, 1);
		add(pauseButton);
		stopButton = makeButton("Stop", src, 2);
		add(stopButton);
		buildButton = makeButton("Build", src, 3);
		add(buildButton);

		newButton = makeButton("New", src, 4);
		add(newButton, "gap unrelated");
		openButton = makeButton("Open", src, 5);
		add(openButton);
		saveButton = makeButton("Save", src, 6);
		add(saveButton);
		
		statusLabel = new JLabel();
		statusLabel.setFont(statusFont);
		statusLabel.setForeground(statusColor);
		add(statusLabel, "gap unrelated");

		setMaximumSize(new Dimension(3000,40));
		setPreferredSize(new Dimension(300,40));
	}

	public MainButton makeButton(String rolloverText, BufferedImage source, int offset) {
		int IMAGE_SIZE = 33;

		Image disabled = source.getSubimage((offset*IMAGE_SIZE)+3, 3*IMAGE_SIZE,
				BUTTON_WIDTH,BUTTON_HEIGHT);
		Image inactive = source.getSubimage((offset*IMAGE_SIZE)+3, 2*IMAGE_SIZE,
				BUTTON_WIDTH,BUTTON_HEIGHT);
		Image rollover = source.getSubimage((offset*IMAGE_SIZE)+3, 1*IMAGE_SIZE,
				BUTTON_WIDTH,BUTTON_HEIGHT);
		Image active = source.getSubimage((offset*IMAGE_SIZE)+3, 0*IMAGE_SIZE,
				BUTTON_WIDTH,BUTTON_HEIGHT);
		
		MainButton mb = new MainButton(rolloverText, active, inactive, rollover, disabled);
		return mb;
	}



	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == simButton) {
			editor.handleSimulate();
		} else if (e.getSource() == pauseButton) {
			editor.handlePause();
		} else if (e.getSource() == stopButton) {
			editor.handleStop();
		} else if (e.getSource() == buildButton) {
			editor.handleBuild();
		} else if (e.getSource() == openButton) {
			editor.handleOpen(null);
		} else if (e.getSource() == newButton) {
			editor.handleNew(false);
		} else if (e.getSource() == saveButton) {
			editor.handleSave(false);
		}
	}

	public void machineStateChanged(MachineStateChangeEvent evt) {
		MachineState s = evt.getState();
		boolean building = s == MachineState.BUILDING ||
			s == MachineState.PAUSED ||
			s == MachineState.CAPTURING ||
			s == MachineState.PLAYBACK_PAUSED ||
			s == MachineState.PLAYBACK_BUILDING;
		boolean paused = s == MachineState.PAUSED ||
			s == MachineState.PLAYBACK_PAUSED;
		boolean disconnected = s == MachineState.AUTO_SCAN ||
			s == MachineState.CONNECTING ||
			s == MachineState.NOT_ATTACHED;
		boolean noFileOps = building;

		newButton.setEnabled(!noFileOps);
		openButton.setEnabled(!noFileOps);
		saveButton.setEnabled(!noFileOps);
		
		simButton.setEnabled(!building);
		pauseButton.setEnabled(building);
		stopButton.setEnabled(building);
		buildButton.setEnabled(!building);
		buildButton.setSelected(building && !paused);
		pauseButton.setSelected(paused);
		
	}

	public void machineProgress(MachineProgressEvent event) {
	}

	public void toolStatusChanged(MachineToolStatusEvent event) {
	}
}
