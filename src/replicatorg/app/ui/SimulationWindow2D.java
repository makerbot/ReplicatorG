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
 
 $Id: MainWindow.java 370 2008-01-19 16:37:19Z mellis $
 */

package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.JComponent;
import javax.vecmath.Point3d;

import replicatorg.util.Point5d;

public class SimulationWindow2D extends SimulationWindow implements
		LayoutManager {
	private static final long serialVersionUID = -1940284103536979587L;

	// these guys are our extra components.
	protected static BuildView buildView;

	protected static HorizontalRuler hRuler;

	protected static VerticalRuler vRuler;

	private int rulerWidth = 25;

	public SimulationWindow2D() {
		super();
		setLayout(this);
		createComponents();
		// some inits to our build simulation
		setTitle("2D Build Simulation");
		setBackground(Color.white);
		setForeground(Color.white);

		this.setVisible(true);

		// start us off at 0,0,0
		buildView.queuePoint(new Point5d());
		getContentPane().setBackground(Color.white);
	}

	private void createComponents() {
		// figure out our content pane size.
		Container pane = getContentPane();
		// make our components with those sizes.
		hRuler = new HorizontalRuler(rulerWidth);
		pane.add(hRuler);
		vRuler = new VerticalRuler(rulerWidth);
		pane.add(vRuler);
		buildView = new BuildView();
		pane.add(buildView);
		invalidate();
	}

	synchronized public void queuePoint(Point5d point) {
		buildView.queuePoint(point);
	}

	class MyComponent extends JComponent {
		private static final long serialVersionUID = 3222037949637415135L;

		public MyComponent() // int width, int height)
		{
			// try and configure size.
			// setPreferredSize(new Dimension(width, height));
			// setMinimumSize(new Dimension(width, height));
			// setMaximumSize(new Dimension(width, height));
			// setSize(width, height);

			// set our colors and stuff
			setBackground(Color.white);
			setForeground(Color.white);
			this.setVisible(true);
		}
	}

	abstract class GenericRuler extends MyComponent {
		protected int machinePosition = 0;

		protected int mousePosition = 0;

		protected int rulerWidth = 25;

		public GenericRuler(int rulerWidth) {
			this.rulerWidth = rulerWidth;
		}

		public void setMousePosition(int i) {
			// only do work if needed!
			if (mousePosition != i) {
				mousePosition = i;
				// Removing for now; optimizing a bit.
				// repaint();
			}
		}

		public void setMachinePosition(int i) {
			// only do work if needed!
			if (mousePosition != i) {
				machinePosition = i;
				// Removing for now; optimizing a bit.
				// repaint();
			}
		}

		class IncrementInfo {
			public IncrementInfo(double major, double minor) {
				this.major = major;
				this.minor = minor;
			}

			public double major;

			public double minor;
		}

		/**
		 * Return the major and minor increment between ticks on the ruler, in
		 * range units.
		 * 
		 * Strategy: in 1s, 1/2s, 1/4s.
		 * 
		 */
		protected IncrementInfo getIncrementInfo(int length, double range) {
			if (range == 0.0 || length < 10)
				return null;

			final int minPixelsBetweenMinors = 10;
			final int minorsPerMajor = 10;

			// how many can we fit on the length?
			int numMinors = length / minPixelsBetweenMinors;
			double rangePerMinor = range / numMinors;

			double increment = 10.0;
			while (rangePerMinor < increment) {
				increment /= 10.0;
			}
			while (rangePerMinor > increment) {
				increment *= 10.0;
			}
			// increment is now greater than scale and a multiple of 10
			double factor = rangePerMinor / increment;
			if (factor < 0.5) {
				increment *= 0.5;
			} else if (factor < 0.25) {
				increment *= 0.25;
			}
			return new IncrementInfo(increment * minorsPerMajor, increment);
		}

		public void paint(Graphics g) {
			// clear it
			g.setColor(new Color(0xe0, 0xe0, 0xe0));
			g.fillRect(0, 0, getWidth(), getHeight());

			// draw our border.
			g.setColor(Color.gray);
			Rectangle bounds = getBounds();
			g.drawRect(0, 0, bounds.width - 1, bounds.height - 1);

			// init our graphics object
			Graphics2D g2 = (Graphics2D) g;

			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2.setPaint(Color.black);

			// draw some helper text.
			g.setFont(new Font("SansSerif", Font.PLAIN, 11));
			g.setColor(Color.black);

			// draw triangles
			// drawTriangles(g2);
			g.setColor(Color.black);
			// draw our tick marks
			drawTicks(g2);
		}

		protected abstract void drawTriangles(Graphics2D g);

		protected abstract void drawTicks(Graphics2D g);

	}

	class HorizontalRuler extends GenericRuler {
		public HorizontalRuler(int rWidth) {
			super(rWidth);
		}

		protected void drawTriangles(Graphics2D g2) {
			// draw our machine indicator
			g2.setPaint(Color.green);
			Polygon xTriangle = new Polygon();
			xTriangle.addPoint(machinePosition, rulerWidth - 1);
			xTriangle.addPoint(machinePosition - rulerWidth / 4, rulerWidth
					- rulerWidth / 4 - 1);
			xTriangle.addPoint(machinePosition + rulerWidth / 4, rulerWidth
					- rulerWidth / 4 - 1);
			g2.fill(xTriangle);

			// draw our mouse indicator
			g2.setPaint(Color.black);
			xTriangle = new Polygon();
			xTriangle.addPoint(mousePosition, rulerWidth - 1);
			xTriangle.addPoint(mousePosition - rulerWidth / 4, rulerWidth
					- rulerWidth / 4 - 1);
			xTriangle.addPoint(mousePosition + rulerWidth / 4, rulerWidth
					- rulerWidth / 4 - 1);
			g2.fill(xTriangle);
		}

		protected void drawTicks(Graphics2D g) {
			double range = SimulationWindow2D.buildView.getXRange();
			if (range < 0.01) {
				return;
			}
			int width = getWidth();
			IncrementInfo ii = getIncrementInfo(width, range);
			double increment = ii.minor;

			// setup our variables.
			int i = 0;
			double real;
			int point;
			int length;

			int textBaseline = g.getFontMetrics().getAscent() + 2;
			// loop thru all positive increments while we're in bounds
			do {
				real = i * increment;
				point = SimulationWindow2D.buildView.convertRealXToPointX(real) - 1;

				if (i % 10 == 0) {
					length = rulerWidth;
					g.drawString(Double.toString(real), point + 1,
									textBaseline);
				} else {
					length = rulerWidth / 3;
				}

				g.drawLine(point, rulerWidth, point, rulerWidth - length);

				i++;
			} while (point < getWidth());

			// loop thru all negative increments while we're in bounds
			i = 0;
			do {
				real = i * increment;
				point = SimulationWindow2D.buildView.convertRealXToPointX(real) - 1;

				if (i % 10 == 0) {
					length = rulerWidth;
					g.drawString(Double.toString(real), point + 1,
									textBaseline);
				} else {
					length = rulerWidth / 3;
				}

				g.drawLine(point, rulerWidth, point, rulerWidth - length);

				i--;
			} while (point > 0);
		}
	}

	class VerticalRuler extends GenericRuler {
		public VerticalRuler(int rWidth) {
			super(rWidth);
		}

		protected void drawTriangles(Graphics2D g2) {
			// draw our machine indicator
			g2.setPaint(Color.black);
			Polygon yTriangle = new Polygon();
			yTriangle.addPoint(rulerWidth - 1, mousePosition);
			yTriangle.addPoint(rulerWidth - rulerWidth / 4 - 1, mousePosition
					- rulerWidth / 4);
			yTriangle.addPoint(rulerWidth - rulerWidth / 4 - 1, mousePosition
					+ rulerWidth / 4);
			g2.fill(yTriangle);

			// draw our mouse indicator
			g2.setPaint(Color.green);
			yTriangle = new Polygon();
			yTriangle.addPoint(rulerWidth - 1, machinePosition);
			yTriangle.addPoint(rulerWidth - rulerWidth / 4 - 1, machinePosition
					- rulerWidth / 4);
			yTriangle.addPoint(rulerWidth - rulerWidth / 4 - 1, machinePosition
					+ rulerWidth / 4);
			g2.fill(yTriangle);
		}

		protected void drawTicks(Graphics2D g) {
			double range = SimulationWindow2D.buildView.getYRange();
			if (range < 0.01)
				return;
			int height = getHeight();
			IncrementInfo ii = getIncrementInfo(height, range);
			double increment = ii.minor;

			// setup our variables.
			int i = 0;
			double real;
			int point;
			int length;

			// loop thru all positive increments while we're in bounds
			do {
				real = i * increment;
				point = SimulationWindow2D.buildView.convertRealYToPointY(real) - 1;

				if (i % 10 == 0) {
					length = rulerWidth;
					g.drawString(Double.toString(real), 2, point - 1);
				} else {
					length = rulerWidth / 3;
				}

				g.drawLine(rulerWidth, point, rulerWidth - length, point);

				i++;
			} while (point > 0 && point < getHeight());

			// loop thru all negative increments while we're in bounds
			i = 0;
			do {
				real = i * increment;
				point = SimulationWindow2D.buildView.convertRealYToPointY(real) - 1;

				if (i % 10 == 0) {
					length = rulerWidth;
					g.drawString(Double.toString(real), 2, point - 1);
				} else {
					length = rulerWidth / 3;
				}

				g.drawLine(rulerWidth, point, rulerWidth - length, point);

				i--;
			} while (point < getHeight() && point > 0);

		}

	}

	class BuildView extends MyComponent implements MouseMotionListener {
		private Point3d minimum;

		private Point3d maximum;

		private Point3d current;

		private double currentZ;

		private int mouseX = 0;

		private int mouseY = 0;

		private double ratio = 1.0;

		private Vector<Point3d> points;

		public BuildView() {
			// setup our listeners.
			addMouseMotionListener(this);

			// init our bounds.
			minimum = new Point3d();
			maximum = new Point3d();
			currentZ = 0.0;

			// initialize our vector
			points = new Vector<Point3d>();
		}

		public void mouseMoved(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();

			SimulationWindow2D.hRuler.setMousePosition(mouseX);
			SimulationWindow2D.vRuler.setMousePosition(mouseY);

			//repaint();
		}

		public void mouseDragged(MouseEvent e) {
			mouseX = e.getX();
			mouseY = e.getY();

			SimulationWindow2D.hRuler.setMousePosition(mouseX);
			SimulationWindow2D.vRuler.setMousePosition(mouseY);

			//repaint();
		}

		public Point3d getMinimum() {
			return minimum;
		}

		public Point3d getMaximum() {
			return maximum;
		}

		public void queuePoint(Point5d point) {
			current = new Point3d(point.get3D());

			// System.out.println("queued: " + point.toString());

			if (current.x < minimum.x)
				minimum.x = current.x;
			if (current.y < minimum.y)
				minimum.y = current.y;
			if (current.z < minimum.z)
				minimum.z = current.z;

			if (current.x > maximum.x)
				maximum.x = current.x;
			if (current.y > maximum.y)
				maximum.y = current.y;
			if (current.z > maximum.z)
				maximum.z = current.z;

			Point3d myPoint = new Point3d(current);
			synchronized (points) {
				points.addElement(myPoint);
			}

			currentZ = current.z;

			calculateRatio();

			// set our machine position
			SimulationWindow2D.hRuler
					.setMachinePosition(convertRealXToPointX(current.x));
			SimulationWindow2D.vRuler
					.setMachinePosition(convertRealYToPointY(current.y));

			buildView.repaint();
		}

		public void paint(Graphics g) {
			// clear it
			g.setColor(Color.white);
			g.fillRect(0, 0, getWidth(), getHeight());
			// g.clearRect(0, 0, width, height);

			// draw our text
			drawHelperText(g);

			// draw our main stuff
			drawLastPoints(g);
		}

		private void drawHelperText(Graphics g) {
			// init some prefs
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2.setPaint(Color.black);
			// draw some helper text.
			g.setFont(new Font("SansSerif", Font.PLAIN, 14));
			g.setColor(Color.black);
			g.drawString("Layer at z: " + currentZ + "mm", 10, 20);

			// draw our mouse position
			double mouseRealX = convertPointXToRealX(mouseX);
			double mouseRealY = convertPointYToRealY(mouseY);
			g.drawString("Mouse: " + mouseRealX + "mm, " + mouseRealY + "mm",
					10, 40);
			if (current != null) {
				g.drawString("Machine: " + current.x + "mm, " + current.y
						+ "mm", 10, 60);
			}
		}

		private void drawLastPoints(Graphics g) {
			Color aboveColor = new Color(255, 0, 0);
			Color currentColor = new Color(0, 255, 0);
			Color belowColor = new Color(0, 0, 255);

			synchronized (points) {
				List<Point3d> lastPoints = getLastPoints(1000);
				Point3d start;
				Point3d end;

				double belowZ = currentZ;
				double aboveZ = currentZ;

				// color coding.
				int aboveTotal = 0;
				int belowTotal = 0;
				int currentTotal = 0;
				int aboveCount = 0;
				int belowCount = 0;
				int currentCount = 0;

				// draw our toolpaths.
				if (lastPoints.size() > 0) {
					start = (Point3d) lastPoints.get(0);

					// start from the most recent line backwards to find the
					// above/below layers.
					for (int i = lastPoints.size() - 1; i >= 0; i--) {
						end = (Point3d) lastPoints.get(i);

						if (!start.equals(end)) {
							// line below current plane
							if (end.z < currentZ) {
								// we only want one layer up/down
								if (end.z < belowZ && belowZ != currentZ)
									continue;

								belowZ = end.z;
								belowTotal++;
							}
							// line above current plane
							else if (end.z > currentZ) {
								// we only want one layer up/down
								if (end.z > aboveZ && aboveZ != currentZ)
									continue;

								aboveZ = end.z;
								aboveTotal++;
							}
							// current line.
							else if (end.z == currentZ) {
								currentTotal++;
							} else
								continue;

							start = new Point3d(end);
						}
					}

					// draw all our lines now!
					for (ListIterator<Point3d> li = lastPoints.listIterator(); li
							.hasNext();) {
						end = li.next();

						// we have to move somewhere!
						if (!start.equals(end)) {
							int startX = convertRealXToPointX(start.x);
							int startY = convertRealYToPointY(start.y);
							int endX = convertRealXToPointX(end.x);
							int endY = convertRealYToPointY(end.y);
							int colorValue;

							// line below current plane
							if (end.z < currentZ && end.z >= belowZ) {
								belowCount++;

								colorValue = 255 - 3 * (belowTotal - belowCount);
								colorValue = Math.max(0, colorValue);
								colorValue = Math.min(255, colorValue);

								belowColor = new Color(0, 0, colorValue);
								g.setColor(belowColor);
							}
							// line above current plane
							if (end.z > currentZ && end.z <= aboveZ) {
								aboveCount++;

								colorValue = 255 - 3 * (aboveTotal - aboveCount);
								colorValue = Math.max(0, colorValue);
								colorValue = Math.min(255, colorValue);

								aboveColor = new Color(colorValue, 0, 0);
								g.setColor(aboveColor);
							}
							// line in current plane
							else if (end.z == currentZ) {
								currentCount++;

								colorValue = 255 - 3 * (currentTotal - currentCount);
								colorValue = Math.max(0, colorValue);
								colorValue = Math.min(255, colorValue);

								currentColor = new Color(0, colorValue, 0);
								g.setColor(currentColor);
							}
							// bail, your'e not on our plane.
							else
								continue;

							// draw up arrow
							if (end.z > start.z) {
								g.setColor(Color.red);
								g.drawOval(startX - 5, startY - 5, 10, 10);
								g.drawLine(startX - 5, startY, startX + 5,
										startY);
								g.drawLine(startX, startY - 5, startX,
										startY + 5);
							}
							// draw down arrow
							else if (end.z < start.z) {
								g.setColor(Color.blue);
								g.drawOval(startX - 5, startY - 5, 10, 10);
								g.drawOval(startX - 1, startY - 1, 2, 2);
							}
							// normal XY line - only draw lines on current layer
							// or above.
							else if (end.z >= currentZ) {
								g.drawLine(startX, startY, endX, endY);
							}

							start = new Point3d(end);
						}
					}

					/*
					 * System.out.println("counts:");
					 * System.out.println(belowCount + " / " + belowTotal);
					 * System.out.println(aboveCount + " / " + aboveTotal);
					 * System.out.println(currentCount + " / " + currentTotal);
					 */
				}
			}
		}

		private List<Point3d> getLastPoints(int count) {
			synchronized (points) {
				int index = Math.max(0, points.size() - count);

				List<Point3d> mypoints = points.subList(index, points.size());

				return mypoints;
			}
		}

		@SuppressWarnings("unused")
		private void drawToolpaths(Graphics g) {
			Vector<Vector<Point3d>> toolpaths = getLayerPaths(currentZ);
			Point3d start = new Point3d();
			Point3d end = new Point3d();

			// System.out.println("toolpaths:" + toolpaths.size());

			// draw our toolpaths.
			if (toolpaths.size() > 0) {
				for (Enumeration<Vector<Point3d>> e = toolpaths.elements(); e.hasMoreElements();) {
					Vector<Point3d> path = e.nextElement();
					// System.out.println("path points:" + path.size());

					if (path.size() > 1) {
						g.setColor(Color.black);
						start = (Point3d) path.firstElement();

						for (Enumeration<Point3d> e2 = path.elements(); e2
								.hasMoreElements();) {
							end = (Point3d) e2.nextElement();

							int startX = convertRealXToPointX(start.x);
							int startY = convertRealYToPointY(start.y);
							int endX = convertRealXToPointX(end.x);
							int endY = convertRealYToPointY(end.y);

							// System.out.println("line from: " + startX + ", "
							// + startY + " to " + endX + ", " + endY);
							g.drawLine(startX, startY, endX, endY);

							start = new Point3d(end);
						}
					}
				}
			}
		}

		private Vector<Vector<Point3d>> getLayerPaths(double layerZ) {
			Vector<Vector<Point3d>> paths = new Vector<Vector<Point3d>>();
			Vector<Point3d> path = new Vector<Point3d>();
			Point3d p;

			synchronized (points) {
				for (Enumeration<Point3d> e = points.elements(); e
						.hasMoreElements();) {
					p = e.nextElement();

					// is this on our current layer?
					if (p.z == layerZ) {
						path.addElement(p);
						// System.out.println("added: " + p.toString());
					}
					// okay, not on layer... did we find a path?
					else if (path.size() > 0) {
						// System.out.println("added path of size " +
						// path.size());
						paths.addElement(path);
						path = new Vector<Point3d>();
					}
				}
			}

			// did we end on our current path?
			if (path.size() > 0)
				paths.addElement(path);

			return paths;
		}

		private void calculateRatio() {
			// calculate the ratios that will keep us inside our box
			double yRatio = (getWidth()) / (maximum.y - minimum.y);
			double xRatio = (getHeight()) / (maximum.x - minimum.x);

			// which one is smallest?
			ratio = Math.min(yRatio, xRatio);
		}

		public double getXRange() {
			return maximum.x - minimum.x;
		}

		public double getYRange() {
			return maximum.y - minimum.y;
		}

		public int convertRealXToPointX(double x) {
			return (int) ((x - minimum.x) * ratio);
		}

		public double convertPointXToRealX(int x) {
			return (Math.round(((x / ratio) - minimum.x) * 100) / 100);
		}

		public int convertRealYToPointY(double y) {
			// subtract from getheight to get a normal origin.
			return ((int) ((y - minimum.y) * ratio));
		}

		public double convertPointYToRealY(int y) {
			return (Math.round((maximum.y - (y / ratio)) * 100) / 100);
		}
	}

	// / LayoutManager implementation

	public void addLayoutComponent(String name, Component comp) {
	}

	public void layoutContainer(Container parent) {
		// figure out our content pane size.
		Container pane = getContentPane();
		int width = pane.getWidth();
		int height = pane.getHeight();

		// make our components with those sizes.
		Rectangle hRuleBounds = new Rectangle(rulerWidth, 0,
				width - rulerWidth, rulerWidth + 1);
		hRuler.setBounds(hRuleBounds);
		Rectangle vRuleBounds = new Rectangle(0, rulerWidth, rulerWidth + 1,
				height - rulerWidth);
		vRuler.setBounds(vRuleBounds);
		Rectangle viewBounds = new Rectangle(rulerWidth + 1, rulerWidth + 1,
				(width - rulerWidth) - 1, (height - rulerWidth) - 1);
		buildView.setBounds(viewBounds);
	}

	public Dimension minimumLayoutSize(Container parent) {
		return new Dimension(0, 0);
	}

	public Dimension preferredLayoutSize(Container parent) {
		return new Dimension(0, 0);
	}

	public void removeLayoutComponent(Component comp) {
	}

	public void setSimulationBounds(Rectangle2D.Double bounds) {
		this.simulationBounds = bounds;
		buildView.maximum.x = bounds.getMaxX();
		buildView.minimum.x = bounds.getMinX();
		buildView.maximum.y = bounds.getMaxY();
		buildView.minimum.y = bounds.getMinY();
		buildView.calculateRatio();
		hRuler.repaint();
		vRuler.repaint();
	}

}
