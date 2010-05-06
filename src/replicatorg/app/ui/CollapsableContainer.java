/*
 CollapsableContainer.java
 
 Part of the ReplicatorG project - http://www.replicat.org
 Copyright (c) 2010 Adam Mayer

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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;

/**
 * A simple container that shows the title, a collapse/expand icon, and
 * a contained area that can be shown or hidden.
 * @author phooky
 *
 */
public class CollapsableContainer extends JComponent {
	private boolean isCollapsed = false;
	private static Icon collapsedIcon = null;
	private static Icon expandedIcon = null;
	
	private static synchronized void initIcons(JComponent comp) {
		if (collapsedIcon != null && expandedIcon != null) return;
		collapsedIcon = new ImageIcon(Base.getImage("images/icon-collapsed.png", comp));
		expandedIcon = new ImageIcon(Base.getImage("images/icon-expanded.png", comp));
	}
	
	protected class Header extends JButton implements ActionListener {

		public Header(String title, boolean collapsed) {
			setText(title);
			initIcons(this);
			setIcon(collapsed);
			setBorderPainted(false);
			addActionListener(this);
		}
		public void setIcon(boolean collapsed) {
			setIcon(collapsed?collapsedIcon:expandedIcon);			
		}
		public void actionPerformed(ActionEvent e) {
			toggleCollapsed();
		}
}
	
	/**
	 * Toggle the collapsed state of this container.
	 */
	public void toggleCollapsed() {
		setCollapsed(!isCollapsed); 
	}
	
	/**
	 * Explicitly set the collapsed state of this container.
	 * @param collapsed true to collapse, false to expand.
	 */
	public void setCollapsed(boolean collapsed) {
		if (isCollapsed == collapsed) return;
		header.setIcon(collapsed);
		isCollapsed = collapsed;
		if (isCollapsed) {
			remove(content);
		} else {
			add(content);
		}
		invalidate();
		Window w = SwingUtilities.windowForComponent(this);
		if (w != null) w.pack();
		//if (getParent() != null) getParent().repaint();
	}
	
	private Header header;
	private JPanel content;
	
	/**
	 * Create a new collapsable container.
	 * @param title the title to display at the top.
	 * @param collapsed the initial collapsed state: true for collapsed, false for expanded.
	 */
	public CollapsableContainer(String title, boolean collapsed) {
		setLayout(new MigLayout());
		header = new Header(title,collapsed);
		isCollapsed = collapsed;
		content = new JPanel(new MigLayout());
		add(header,"wrap");
		if (!isCollapsed) add(content);
	}

	/**
	 * Returns the container to which collapsable content should be added.  The panel is
	 * initially given a MigLayout.
	 */
	public JPanel getContent() {
		return content;
	}
}
