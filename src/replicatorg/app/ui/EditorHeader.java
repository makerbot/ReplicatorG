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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.model.Build;
import replicatorg.model.BuildElement;

/**
 * Sketch tabs at the top of the editor window.
 */
public class EditorHeader extends BGPanel implements ActionListener {
	public enum Tab {
		MODEL,
		GCODE
	};
	
	private ButtonGroup tabGroup = new ButtonGroup();

	public Tab getSelectedTab() {
		// Enumeration isn't iterable yet?
		Enumeration<AbstractButton> e = tabGroup.getElements();
		while (e.hasMoreElements()) {
			TabButton tb = (TabButton)e.nextElement();
			if (tb.isSelected()) { return tb.getTab(); }
		}
		return null;
	}
	
	static Color backgroundColor;

	static Color textSelectedColor;
	static Color textUnselectedColor;
 
	private ChangeListener changeListener;
	void setChangeListener(ChangeListener listener) {
		changeListener = listener;
	}
	
	private class TabButtonUI extends BasicButtonUI {
		public void paint(Graphics g,JComponent c) {
			initTabImages();
			TabButton b = (TabButton)c;
			BufferedImage img = b.isSelected()?selectedTabBg:regularTabBg;
			final int partWidth = img.getWidth()/3;
			int height = img.getHeight();
			final int x = 0;
			final int y = 0;
			final int w = c.getWidth();
			// Draw left side of tab
			g.drawImage(img, x, y, x+partWidth, y+height, 0, 0, partWidth, height, null);
			final int rightTabStart = img.getWidth()-partWidth;
			// Draw center of tab
			g.drawImage(img, x+partWidth, y, x+w-partWidth, y+height, partWidth, 0, rightTabStart, height, null);
			// Draw right side of tab
			g.drawImage(img, x+w-partWidth, y, x+w, y+height, rightTabStart, 0, img.getWidth(), height, null);
			b.setForeground(b.isSelected()?textSelectedColor:textUnselectedColor);
			super.paint(g,c);
		}
	}

	static BufferedImage selectedTabBg;
	static BufferedImage regularTabBg;
	
	protected void initTabImages() {
		if (selectedTabBg == null) {
			selectedTabBg = Base.getImage("images/tab-selected.png", this);
		}
		if (regularTabBg == null) {
			regularTabBg = Base.getImage("images/tab-regular.png", this);
		}
	}


	private class TabButton extends JToggleButton {
		final Tab which;
		
		public Tab getTab() { return which; }
		
		public TabButton(String text, Tab which) {
			super(text);
			this.which = which;
			setUI(new TabButtonUI());
			setBorder(new EmptyBorder(6,8,8,10));
			tabGroup.add(this);
			addActionListener(EditorHeader.this);
		}
	}
	
	JLabel titleLabel = new JLabel("Untitled");
	
	MainWindow editor;

	int fontAscent;

	int menuLeft;

	int menuRight;

	public EditorHeader(MainWindow mainWindow) {
		initTabImages();
		setBorder(null);
		setLayout(new MigLayout("gap 10 10 0 0"));
		this.editor = mainWindow;

		add(titleLabel);
		backgroundColor = new Color(0x92, 0xA0, 0x6B);
		textSelectedColor = Base.getColorPref("header.text.selected.color","#1A1A00");
		textUnselectedColor = Base.getColorPref("header.text.unselected.color","#ffffff");
		setBackground(backgroundColor);
	}

	private void removeTabs() {
		tabGroup = new ButtonGroup();
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponent(i) instanceof TabButton) {
				remove(i);
				removeTabs();
				return;
			}
		}
		validate();
	}
	
	void setBuild(Build build) {
		removeTabs();
		Tab initialSelection = Tab.GCODE;
		if (build.getOpenedElement() != null) {
			if (build.getOpenedElement().getType() == BuildElement.Type.MODEL) {
				initialSelection = Tab.MODEL;
			}
		}
		if (build.getModel() != null) {
			TabButton tb = new TabButton("model",Tab.MODEL);
			add(tb);
			if (initialSelection == Tab.MODEL) { tb.doClick(); } 
		}
		if (build.getCode() != null) {
			TabButton tb = new TabButton("gcode",Tab.GCODE);
			add(tb);
			if (initialSelection == Tab.GCODE) { tb.doClick(); } 
		}
		titleLabel.setText(build.getName());
		validate();
		repaint();
	}
	
	/**
	 * Called when a new sketch is opened.
	 */
	public void rebuild() {
		// System.out.println("rebuilding editor header");
		repaint();
	}

	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, 30);
	}	
		
	public Dimension getMinimumSize() {
		return new Dimension(0, 0);
	}

	public void actionPerformed(ActionEvent a) {
		ChangeEvent e = new ChangeEvent(this);
		if (changeListener != null) changeListener.stateChanged(e);
	}
}
