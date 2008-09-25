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
  
  $Id: Editor.java 370 2008-01-19 16:37:19Z mellis $
*/

package processing.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.vecmath.*;
import java.util.*;

public class ControlPanelWindow extends JFrame
{
	protected JPanel jogPanel;
	protected JButton xPlusButton;
	protected JButton xMinusButton;
	protected JButton yPlusButton;
	protected JButton yMinusButton;
	protected JButton zPlusButton;
	protected JButton zMinusButton;

	protected JPanel extruderPanel;
		
	public ControlPanelWindow ()
	{
		super("Control Panel");
		
		//make it a reasonable size
 		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		//int myWidth = screen.width-40;
		//int myHeight = screen.height-40;
		int myWidth = 420;
		int myHeight = 600;
		
	 	this.setBounds(40, 40, myWidth, myHeight);
	
		//default behavior
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
	
		//no resizing... yet
		this.setResizable(false);
		
		//no menu bar.
		this.setMenuBar(null);
		
		//create all our GUI interfaces
		createJogPanel();
		//createExtruderPanel();
	}
	
	protected void createJogPanel()
	{
		//create our X+ button
		xPlusButton = new JButton("X+");
		//xPlusButton.setMnemonic(KeyEvent.VK_KP_RIGHT);
		xPlusButton.setToolTipText("Jog X axis in positive direction");
		xPlusButton.setMaximumSize(new Dimension(75, 75));
		xPlusButton.setPreferredSize(new Dimension(75, 75));
		xPlusButton.setMinimumSize(new Dimension(75, 75));
		
		//create our X- button
		xMinusButton = new JButton("X-");
		//xMinusButton.setMnemonic(KeyEvent.VK_KP_LEFT);
		xMinusButton.setToolTipText("Jog X axis in negative direction");
		xMinusButton.setMaximumSize(new Dimension(75, 75));
		xMinusButton.setPreferredSize(new Dimension(75, 75));
		xMinusButton.setMaximumSize(new Dimension(75, 75));

		//create our Y+ button
		yPlusButton = new JButton("Y+");
		//yPlusButton.setMnemonic(KeyEvent.VK_KP_UP);
		yPlusButton.setToolTipText("Jog Y axis in positive direction");
		yPlusButton.setMaximumSize(new Dimension(75, 75));
		yPlusButton.setPreferredSize(new Dimension(75, 75));
		yPlusButton.setMinimumSize(new Dimension(75, 75));
		
		//create our Y- button
		yMinusButton = new JButton("Y-");
		//yMinusButton.setMnemonic(KeyEvent.VK_KP_DOWN);
		yMinusButton.setToolTipText("Jog Y axis in negative direction");
		yMinusButton.setMaximumSize(new Dimension(75, 75));
		yMinusButton.setPreferredSize(new Dimension(75, 75));
		yMinusButton.setMinimumSize(new Dimension(75, 75));

		//create our Z+ button
		zPlusButton = new JButton("Z+");
		//zPlusButton.setMnemonic(KeyEvent.VK_PLUS);
		zPlusButton.setToolTipText("Jog Z axis in positive direction");
		zPlusButton.setMaximumSize(new Dimension(75, 75));
		zPlusButton.setPreferredSize(new Dimension(75, 75));
		zPlusButton.setMinimumSize(new Dimension(75, 75));
		
		//create our Z- button
		zMinusButton = new JButton("Z-");
		//zMinusButton.setMnemonic(KeyEvent.VK_MINUS);
		zMinusButton.setToolTipText("Jog Z axis in negative direction");
		zMinusButton.setMaximumSize(new Dimension(75, 75));
		zMinusButton.setPreferredSize(new Dimension(75, 75));

		//create our XY panel
		JPanel xyPanel = new JPanel();
		xyPanel.setLayout(new BoxLayout(xyPanel, BoxLayout.LINE_AXIS));
		xyPanel.add(xPlusButton);
		xyPanel.add(Box.createHorizontalGlue());
		
		//another panel to hold the vertical stuff
		JPanel yPanel = new JPanel();
		yPanel.setLayout(new BoxLayout(yPanel, BoxLayout.PAGE_AXIS));
		yPanel.add(yPlusButton);
		yPanel.add(Box.createVerticalGlue());
		yPanel.add(yMinusButton);
		xyPanel.add(yPanel);
		
		//finally our last button.
		xyPanel.add(Box.createHorizontalGlue());
		xyPanel.add(xMinusButton);
		
		//our z panel too
		JPanel zPanel = new JPanel();
		zPanel.setLayout(new BoxLayout(zPanel, BoxLayout.PAGE_AXIS));
		zPanel.add(zPlusButton);
		zPanel.add(Box.createVerticalGlue());
		zPanel.add(zMinusButton);

		//add them both to our xyz panel
		JPanel xyzPanel = new JPanel();
		xyzPanel.setLayout(new BoxLayout(xyzPanel, BoxLayout.LINE_AXIS));
		xyzPanel.add(xyPanel);
		xyzPanel.add(Box.createHorizontalGlue());
		xyzPanel.add(zPanel);
		
		//create our feedrate sliders
		//TODO: pull these values from our machine config!
		JSlider xyFeedrateSlider = new JSlider(JSlider.HORIZONTAL, 1, 5000, 1000);
		xyFeedrateSlider.setMajorTickSpacing(1000);
		xyFeedrateSlider.setMinorTickSpacing(100);

		JSlider zFeedrateSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 50);
		zFeedrateSlider.setMajorTickSpacing(10);
		zFeedrateSlider.setMinorTickSpacing(1);

		//create our jog panel
		jogPanel = new JPanel();
		jogPanel.setLayout(new BoxLayout(jogPanel, BoxLayout.PAGE_AXIS));
		
		//proper size!
		jogPanel.setMinimumSize(new Dimension(420, 200));
		jogPanel.setMaximumSize(new Dimension(420, 200));
		jogPanel.setPreferredSize(new Dimension(420, 200));

		//add it all to our jog panel
		jogPanel.add(xyzPanel);
		jogPanel.add(xyFeedrateSlider);
		jogPanel.add(zFeedrateSlider);		

		//add the whole deal to our window.
		add(jogPanel);
	}
	
	protected void createExtruderPanel()
	{
		extruderPanel = new JPanel();
		extruderPanel.setLayout(new BoxLayout(extruderPanel, BoxLayout.PAGE_AXIS));
		
		add(extruderPanel);
	}
}
