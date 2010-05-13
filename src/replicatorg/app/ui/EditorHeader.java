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
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.model.Build;

/**
 * Sketch tabs at the top of the editor window.
 */
public class EditorHeader extends JComponent {
	static Color backgroundColor;

	static Color textSelectedColor;
	static Color textUnselectedColor;

	private ButtonGroup tabGroup = new ButtonGroup();
	
	private class TabButtonUI extends BasicButtonUI {
		protected void paintText(Graphics g,AbstractButton b,Rectangle textRect,String text) {
			b.setForeground(b.isSelected()?textSelectedColor:textUnselectedColor);
			super.paintText(g,b,textRect,text);
		}
	}
	
	private class TabButton extends JToggleButton {
		public TabButton(String text) {
			super(text);
			setUI(new TabButtonUI());
			setBorder(new EmptyBorder(0,0,0,0));
			tabGroup.add(this);
		}
	}
	
	JToggleButton codeButton = new TabButton("gcode");
	JToggleButton modelButton = new TabButton("model");
	JLabel titleLabel = new JLabel("Untitled");
	
	MainWindow editor;

	int fontAscent;

	int menuLeft;

	int menuRight;

	public EditorHeader(MainWindow mainWindow) {
		setLayout(new MigLayout("gap 15"));
		this.editor = mainWindow;

		add(titleLabel);
		add(modelButton);
		add(codeButton);
		codeButton.setSelected(true);
		backgroundColor = new Color(0x92, 0xA0, 0x6B);
		textSelectedColor = Base.getColorPref("header.text.selected.color","#1A1A00");
		textUnselectedColor = Base.getColorPref("header.text.unselected.color","#ffffff");
	}

	public void paintComponent(Graphics g) {
		if (g == null)
			return;

		Build sketch = editor.sketch;
		if (sketch == null)
			return; // ??

		Dimension size = getSize();

		// set the background for the offscreen
		g.setColor(backgroundColor);
		g.fillRect(0, 0, size.width, size.height);

		super.paintComponent(g);
	}

	/**
	 * Called when a new sketch is opened.
	 */
	public void rebuild() {
		// System.out.println("rebuilding editor header");
		repaint();
	}

	
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	final static int GRID_SIZE = 33;
	
	public Dimension getMinimumSize() {
		return new Dimension(0, GRID_SIZE);
	}
}
