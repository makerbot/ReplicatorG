package replicatorg.app.ui;

import java.awt.Container;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JTable;

import replicatorg.app.Base;

public class AdvancedPrefs extends JFrame {

	public AdvancedPrefs()
	{
		Container content = getContentPane();
		
		Object[][] prefs = getPreferences();
		
		JTable prefsDisplay = new JTable(prefs, new Object[]{"Preference name", "value"});
	}
	
	private Object[][] getPreferences()
	{
		Preferences p = Base.preferences;
//		OutputStream os = new OutputStream();
////		BufferedReader prefReader = new BufferedReader(os);
//		
//		try {
////			p.exportSubtree(os);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (BackingStoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
		return null;
	}
}
