package replicatorg.app.ui;

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

/**
 * BGPanel serves but one purpose: to get past the hideousness of the Mac L+F and actually draw
 * a freaking background.
 * 
 * @author phooky
 *
 */
public class BGPanel extends JPanel {
	public BGPanel() {
		setOpaque(true);
	}
	
	public void paint(Graphics g) {
		g.setColor(getBackground());
		Rectangle r = getBounds();
		g.fillRect(r.x,r.y,r.width,r.height);
		paintComponent(g);
		paintChildren(g);
	}
}
