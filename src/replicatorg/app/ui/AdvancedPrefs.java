package replicatorg.app.ui;

import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import replicatorg.app.Base;

public class AdvancedPrefs extends JFrame {

	public AdvancedPrefs()
	{
		Container content = getContentPane();
		
		Object[][] prefs = getPreferences();
		
		JTable prefsDisplay = new JTable(prefs, new Object[]{"Preference name", "value"});
		prefsDisplay.setEnabled(false);
		content.add(prefsDisplay);

		content.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				KeyStroke wc = MainWindow.WINDOW_CLOSE_KEYSTROKE;
				if ((e.getKeyCode() == KeyEvent.VK_ESCAPE)
						|| (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
					dispose();
				}
			}
		});
		pack();
	}
	
	private Object[][] getPreferences()
	{
		Object[][] result;
		Preferences p = Base.preferences;
		try {
			String[] pNames = p.keys();
			result = new Object[pNames.length][2];
			for(int i = 0; i < pNames.length; i++)
			{
				result[i] = new String[]{pNames[i], p.get(pNames[i], "")};
			}
				
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return result;
	}
}
