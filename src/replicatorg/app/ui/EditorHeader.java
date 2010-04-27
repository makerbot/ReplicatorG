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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import replicatorg.app.Base;
import replicatorg.model.Build;
import replicatorg.model.BuildCode;

/**
 * Sketch tabs at the top of the editor window.
 */
public class EditorHeader extends JComponent {
	static Color backgroundColor;

	static Color textColor[] = new Color[2];

	MainWindow editor;

	int tabLeft[];

	int tabRight[];

	Font font;

	FontMetrics metrics;

	int fontAscent;

	int menuLeft;

	int menuRight;

	//

	static final String STATUS[] = { "unsel", "sel" };

	static final int UNSELECTED = 0;

	static final int SELECTED = 1;

	static final String WHERE[] = { "left", "mid", "right", "menu" };

	static final int LEFT = 0;

	static final int MIDDLE = 1;

	static final int RIGHT = 2;

	static final int MENU = 3;

	static final int PIECE_WIDTH = 4;

	Image[][] pieces;

	//

	Image offscreen;

	int sizeW, sizeH;

	int imageW, imageH;

	public EditorHeader(MainWindow eddie) {
		this.editor = eddie; // weird name for listener

		pieces = new Image[STATUS.length][WHERE.length];
		for (int i = 0; i < STATUS.length; i++) {
			for (int j = 0; j < WHERE.length; j++) {
				pieces[i][j] = Base.getImage("images/tab-" + STATUS[i] + "-"
						+ WHERE[j] + ".gif", this);
			}
		}

		if (backgroundColor == null) {
			// backgroundColor =
			// Preferences.getColor("header.bgcolor");
			// hardcoding new blue color scheme for consistency with images,
			// see EditorStatus.java for details.
			backgroundColor = new Color(0x92, 0xA0, 0x6B);
			textColor[SELECTED] = Base
					.getColorPref("header.text.selected.color","#1A1A00");
			textColor[UNSELECTED] = Base
					.getColorPref("header.text.unselected.color","#ffffff");
		}

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();

				for (int i = 0; i < editor.sketch.code.size(); i++) {
					if ((x > tabLeft[i]) && (x < tabRight[i])) {
						editor.sketch.setCurrent(i);
						repaint();
					}
				}
			}
		});
	}

	public void paintComponent(Graphics screen) {
		if (screen == null)
			return;

		Build sketch = editor.sketch;
		if (sketch == null)
			return; // ??

		Dimension size = getSize();
		if ((size.width != sizeW) || (size.height != sizeH)) {
			// component has been resized

			if ((size.width > imageW) || (size.height > imageH)) {
				// nix the image and recreate, it's too small
				offscreen = null;

			} else {
				// who cares, just resize
				sizeW = size.width;
				sizeH = size.height;
				// userLeft = 0; // reset
			}
		}

		if (offscreen == null) {
			sizeW = size.width;
			sizeH = size.height;
			imageW = sizeW;
			imageH = sizeH;
			offscreen = createImage(imageW, imageH);
		}

		Graphics g = offscreen.getGraphics();
		if (font == null) {
			font = Base.getFontPref("header.text.font","SansSerif,plain,12");
		}
		g.setFont(font); // need to set this each time through
		metrics = g.getFontMetrics();
		fontAscent = metrics.getAscent();
		// }

		// Graphics2D g2 = (Graphics2D) g;
		// g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
		// RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// set the background for the offscreen
		g.setColor(backgroundColor);
		g.fillRect(0, 0, imageW, imageH);

		if ((tabLeft == null) || (tabLeft.length < sketch.code.size())) {
			tabLeft = new int[sketch.code.size()];
			tabRight = new int[sketch.code.size()];
		}

		// int x = 0; //Preferences.GUI_SMALL;
		// int x = (Base.platform == Base.MACOSX) ? 0 : 1;
		int x = 6; // offset from left edge of the component
		for (int i = 0; i < sketch.code.size(); i++) {
			BuildCode code = sketch.code.get(i);

			String codeName = code.name;

			// if modified, add the li'l glyph next to the name
			String text = "  " + codeName + (code.modified ? " \u00A7" : "  ");

			// int textWidth = metrics.stringWidth(text);
			Graphics2D g2 = (Graphics2D) g;
			int textWidth = (int) font.getStringBounds(text,
					g2.getFontRenderContext()).getWidth();

			int pieceCount = 2 + (textWidth / PIECE_WIDTH);
			int pieceWidth = pieceCount * PIECE_WIDTH;

			int state = (code == sketch.currentCode) ? SELECTED : UNSELECTED;
			g.drawImage(pieces[state][LEFT], x, 0, null);
			x += PIECE_WIDTH;

			int contentLeft = x;
			tabLeft[i] = x;
			for (int j = 0; j < pieceCount; j++) {
				g.drawImage(pieces[state][MIDDLE], x, 0, null);
				x += PIECE_WIDTH;
			}
			tabRight[i] = x;
			int textLeft = contentLeft + (pieceWidth - textWidth) / 2;

			g.setColor(textColor[state]);
			int baseline = (sizeH + fontAscent) / 2;
			// g.drawString(sketch.code.get(i).name, textLeft, baseline);
			g.drawString(text, textLeft, baseline);

			g.drawImage(pieces[state][RIGHT], x, 0, null);
			x += PIECE_WIDTH - 1; // overlap by 1 pixel
		}

		screen.drawImage(offscreen, 0, 0, null);
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
		if (Base.isMacOS()) {
			return new Dimension(300, GRID_SIZE);
		}
		return new Dimension(300, GRID_SIZE - 1);
	}

	public Dimension getMaximumSize() {
		if (Base.isMacOS()) {
			return new Dimension(3000, GRID_SIZE);
		}
		return new Dimension(3000, GRID_SIZE - 1);
	}
}
