package replicatorg.app.ui;

import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Font;
import java.io.BufferedOutputStream;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.table.DefaultTableModel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import replicatorg.app.Base;

public class AdvancedPrefs extends JFrame {
	private int leftWidth;
	private int rightWidth;
	private int rowCount;
	private JTable prefsDisplay = new JTable();

	public AdvancedPrefs()
	{
		Container content = getContentPane();
		
		Object[][] prefs = getPreferences();
		
		prefsDisplay.setModel(
			new DefaultTableModel(
				prefs, new Object[]{"Preference name", "value"}));
		prefsDisplay.setEnabled(false);

		content.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				KeyStroke wc = MainWindow.WINDOW_CLOSE_KEYSTROKE;
				if ((e.getKeyCode() == KeyEvent.VK_ESCAPE)
						|| (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
					dispose();
				}
			}
		});
		prefsDisplay.getColumnModel().getColumn(0).setMinWidth(leftWidth);
		prefsDisplay.getColumnModel().getColumn(1).setMinWidth(rightWidth);
		int height = rowCount * prefsDisplay.getRowHeight();
		Dimension size = new Dimension(leftWidth * 2, height);
		prefsDisplay.setPreferredScrollableViewportSize(size);
		prefsDisplay.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		content.add(new JScrollPane(prefsDisplay));
		pack();
	}
	
	private Object[][] getPreferences()
	{
	
		Object[][] result;
		Preferences p = Base.preferences;
		try {
			Font font = prefsDisplay.getFont();
			FontRenderContext frc =
				prefsDisplay.getFontMetrics(font).getFontRenderContext();
			String[] pNames = p.keys();
			rowCount = pNames.length;
			result = new Object[rowCount][2];
			leftWidth = 0;
			rightWidth = 0;
			for(int i = 0; i < rowCount; i++)
			{
				String s[] = new String[]{pNames[i], p.get(pNames[i], "")};
				Rectangle2D r = font.getStringBounds(s[0], frc);
				int x = (int)(r.getWidth() * 1.05);
				if (x > leftWidth) { leftWidth = x; }
				r = font.getStringBounds(s[1], frc);
				x = (int)(r.getWidth() * 1.05);
				if (x > rightWidth) { rightWidth = x; }
				result[i] = s;
			}
				
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return result;
	}
}
