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

package replicatorg.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JComponent;

import replicatorg.app.syntax.JEditTextArea;

import replicatorg.app.drivers.EstimationDriver;


/**
 * Li'l status bar fella that shows the line number.
 */
public class EditorLineStatus extends JComponent {
  JEditTextArea textarea;
  int start = -1, stop;

  Image resize;

  Color foreground;
  Color background;
  Font font;
  int high;

  String text = "";


  public EditorLineStatus(JEditTextArea textarea) {
    this.textarea = textarea;
    textarea.editorLineStatus = this;

	// hardcoding new blue color scheme for consistency with images,
	// see EditorStatus.java for details.
    //background = Preferences.getColor("linestatus.bgcolor");
	background = new Color(0x5F, 0x73, 0x25);
    font = Preferences.getFont("linestatus.font");
    foreground = Preferences.getColor("linestatus.color");
    high = Preferences.getInteger("linestatus.height");

    if (Base.isMacOS()) {
      resize = Base.getImage("resize.gif", this);
    }
    //linestatus.bgcolor = #000000
    //linestatus.font    = SansSerif,plain,10
    //linestatus.color   = #FFFFFF
  }


  public void set(int currentLine, double elapsedTime, double timeRemaining)
  {
		long total = textarea.getLineCount() - 1;
		double percentage = Math.round(((double)currentLine / (double)total) * 10000.0) / 100.0;
		text = "Commands: ";
		text += String.format("%1$7d / %2$7d", currentLine, total);
		text += "     |     ";
		text += String.format("%1$3.2f", percentage) + "%";
		text += "     |     ";
		text += "Elapsed time: " + EstimationDriver.getBuildTimeString(elapsedTime, true);
		text += "     |     ";
		text += "Time remaining: " + EstimationDriver.getBuildTimeString(timeRemaining, true);

    repaint();
  }


  public void paintComponent(Graphics g) {
    g.setColor(background);
    Dimension size = getSize();
    g.fillRect(0, 0, size.width, size.height);

    g.setFont(font);
    g.setColor(foreground);
    int baseline = (high + g.getFontMetrics().getAscent()) / 2;
    g.drawString(text, 6, baseline);

    if (Base.isMacOS()) {
      g.drawImage(resize, size.width - 20, 0, this);
    }
  }


  public Dimension getPreferredSize() {
    return new Dimension(300, high);
  }

  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  public Dimension getMaximumSize() {
    return new Dimension(3000, high);
  }
}
