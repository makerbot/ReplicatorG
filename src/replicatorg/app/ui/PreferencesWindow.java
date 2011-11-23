/**
 * 
 */
package replicatorg.app.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.Base.InitialOpenBehavior;
import replicatorg.app.util.PythonUtils;
import replicatorg.app.util.SwingPythonSelector;
import replicatorg.uploader.FirmwareUploader;

/**
 * Edit the major preference settings.
 * @author phooky
 *
 */
public class PreferencesWindow extends JFrame implements GuiConstants {
	// gui elements

	// the calling editor, so updates can be applied
	MainWindow editor;

	JFormattedTextField fontSizeField;
	JTextField firmwareUpdateUrlField;
	JTextField logPathField;
	
	private void showCurrentSettings() {		
		Font editorFont = Base.getFontPref("editor.font","Monospaced,plain,12");
		fontSizeField.setText(String.valueOf(editorFont.getSize()));
		String firmwareUrl = Base.preferences.get("replicatorg.updates.url", FirmwareUploader.DEFAULT_UPDATES_URL);
		firmwareUpdateUrlField.setText(firmwareUrl);
		String logPath = Base.preferences.get("replicatorg.logpath", "");
		logPathField.setText(logPath);
	}
	
	private JCheckBox addCheckboxForPref(Container c, String text, final String pref, boolean defaultVal) {
		JCheckBox cb = new JCheckBox(text);
		cb.setSelected(Base.preferences.getBoolean(pref,defaultVal));
		c.add(cb,"wrap");
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JCheckBox box = (JCheckBox)e.getSource();
				Base.preferences.putBoolean(pref,box.isSelected());
			}
		});
		return cb;
	}

	private void addInitialFilePrefs(Container c) {
		final String prefName = "replicatorg.initialopenbehavior";
		int defaultBehavior = InitialOpenBehavior.OPEN_LAST.ordinal();
		int ordinal = Base.preferences.getInt(prefName, defaultBehavior);
		if (ordinal >= InitialOpenBehavior.values().length) {
			ordinal = defaultBehavior;
		}
		final InitialOpenBehavior openBehavior = InitialOpenBehavior.values()[ordinal];
		ButtonGroup bg = new ButtonGroup();
		class RadioAction extends AbstractAction {
			private InitialOpenBehavior behavior;
		    public RadioAction(String text, InitialOpenBehavior behavior) {
		    	super(text);
		    	this.behavior = behavior;
		    }
		    public void actionPerformed(ActionEvent e) {
		    	Base.preferences.putInt(prefName,behavior.ordinal());
		    }
		}
		c.add(new JLabel("On ReplicatorG launch:"),"split");
		// We don't have SELECTED_KEY in Java 1.5, so we'll do things the old-fashioned, ugly way.
		JRadioButton b;
		b = new JRadioButton(new RadioAction("Open last opened or save file",InitialOpenBehavior.OPEN_LAST));
    	if (InitialOpenBehavior.OPEN_LAST == openBehavior) { b.setSelected(true); }
		bg.add(b);
		c.add(b,"split");
		b = new JRadioButton(new RadioAction("Open new file",InitialOpenBehavior.OPEN_NEW));
    	if (InitialOpenBehavior.OPEN_NEW == openBehavior) { b.setSelected(true); }
		bg.add(b);
		c.add(b,"wrap 10px");
	}

	JComboBox makeDebugLevelDropdown() {
		String levelName = Base.preferences.get("replicatorg.debuglevel", Level.INFO.getName());
		Level l = Level.parse(levelName);
		if (l == null) { l = Level.INFO; }
		Vector<Level> levels = new Vector<Level>();
		levels.add(Level.ALL);
		levels.add(Level.FINEST);
		levels.add(Level.FINER);
		levels.add(Level.FINE);
		levels.add(Level.INFO);
		levels.add(Level.WARNING);
		final ComboBoxModel model = new DefaultComboBoxModel(levels);
		model.setSelectedItem(l);
		JComboBox cb = new JComboBox(model);
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Level level = (Level)(model.getSelectedItem());
				Base.preferences.put("replicatorg.debuglevel", level.getName());
				Base.logger.setLevel(level);
			}
		});
		return cb;
	}
	
	// Copied from app.ui.controlpanel.ExtruderPanel
	private double confirmTemperature(double target, String limitPrefName, double defaultLimit) {
		double limit = Base.preferences.getDouble("temperature.acceptedLimit", defaultLimit);
		if (target > limit){
			// Temperature warning dialog!
			int n = JOptionPane.showConfirmDialog(this,
					"<html>Setting the temperature to <b>" + Double.toString(target) + "\u00b0C</b> may<br>"+
					"involve health and/or safety risks or cause damage to your machine!<br>"+
					"The maximum recommended temperature is <b>"+Double.toString(limit)+"</b>.<br>"+
					"Do you accept the risk and still want to set this temperature?",
					"Danger",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE);
			if (n == JOptionPane.YES_OPTION) {
				return target;
			} else if (n == JOptionPane.NO_OPTION) {
				return Double.MIN_VALUE;
			} else { // Cancel or whatnot
				return Double.MIN_VALUE;
			}
		}  else {
			return target;
		}
	}
	
	public PreferencesWindow() {
		super("Preferences");
		setResizable(true);
		
		Image icon = Base.getImage("images/icon.gif", this);
		setIconImage(icon);
		
		JTabbedPane basicVSadvanced = new JTabbedPane();
		
		JPanel basic = new JPanel();
		
//		Container content = this.getContentPane();
		Container content = basic;
		content.setLayout(new MigLayout("fill"));

		content.add(new JLabel("MainWindow font size: "), "split");
		fontSizeField = new JFormattedTextField(Base.getLocalFormat());
		fontSizeField.setColumns(4);
		content.add(fontSizeField);
		content.add(new JLabel("  (requires restart of ReplicatorG)"), "wrap");

		addCheckboxForPref(content,"Monitor temperature during builds","build.monitor_temp",false);
		addCheckboxForPref(content,"Automatically connect to machine at startup","replicatorg.autoconnect",true);
		addCheckboxForPref(content,"Show experimental machine profiles","machine.showExperimental",false);
		addCheckboxForPref(content,"Review GCode for potential toolhead problems before building","build.safetyChecks",true);
		addCheckboxForPref(content,"Break Z motion into seperate moves (normally false)","replicatorg.parser.breakzmoves",false);
		addCheckboxForPref(content,"Show starfield in model preview window","ui.show_starfield",false);
		addCheckboxForPref(content,"Notifications in System tray","ui.preferSystemTrayNotifications",false);
		addCheckboxForPref(content,"Show warning when building from model w/ existing gcode","build.showRegenCheck",true);
		
		JPanel advanced = new JPanel();
		content = advanced;
		content.setLayout(new MigLayout("fill"));
		
		JButton modelColorButton;
		modelColorButton = new JButton("Choose model color");
		modelColorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Note that this color is also defined in EditingModel.java
				Color modelColor = new Color(Base.preferences.getInt("ui.modelColor",-19635));
				modelColor = JColorChooser.showDialog(
						null,
		                "Choose Model Color",
		                modelColor);
		        if(modelColor == null)
		        	return;
				
		        Base.preferences.putInt("ui.modelColor", modelColor.getRGB());
		        Base.getEditor().refreshPreviewPanel();
			}
		});
		modelColorButton.setVisible(true);
		content.add(modelColorButton,"split");
		
		
		JButton backgroundColorButton;
		backgroundColorButton = new JButton("Choose background color");
		backgroundColorButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Note that this color is also defined in EditingModel.java
				Color backgroundColor = new Color(Base.preferences.getInt("ui.backgroundColor", 0));
				backgroundColor = JColorChooser.showDialog(
						null,
		                "Choose Background Color",
		                backgroundColor);
		        if(backgroundColor == null)
		        	return;
		                
		        Base.preferences.putInt("ui.backgroundColor", backgroundColor.getRGB());
		        Base.getEditor().refreshPreviewPanel();
			}
		});
		backgroundColorButton.setVisible(true);
		content.add(backgroundColorButton,"wrap");
		
		
		content.add(new JLabel("Firmware update URL: "),"split");
		firmwareUpdateUrlField = new JTextField(34);
		content.add(firmwareUpdateUrlField,"growx, wrap");

		{
			JLabel arcResolutionLabel = new JLabel("Arc resolution (in mm): ");
			content.add(arcResolutionLabel,"split");
			double value = Base.preferences.getDouble("replicatorg.parser.curve_segment_mm", 1.0);
			JFormattedTextField arcResolutionField = new JFormattedTextField(Base.getLocalFormat());
			arcResolutionField.setValue(new Double(value));
			content.add(arcResolutionField);
			String arcResolutionHelp = "<html><small><em>" +
				"The arc resolution is the default segment length that the gcode parser will break arc codes <br>"+
				"like G2 and G3 into.  Drivers that natively handle arcs will ignore this setting." +
				"</em></small></html>";
			arcResolutionField.setToolTipText(arcResolutionHelp);
			arcResolutionLabel.setToolTipText(arcResolutionHelp);
//			content.add(new JLabel(arcResolutionHelp),"growx,wrap");
			arcResolutionField.setColumns(10);
			arcResolutionField.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getPropertyName() == "value") {
						try {
							Double v = (Double)evt.getNewValue();
							if (v == null) return;
							Base.preferences.putDouble("replicatorg.parser.curve_segment_mm", v.doubleValue());
						} catch (ClassCastException cce) {
							Base.logger.warning("Unexpected value type: "+evt.getNewValue().getClass().toString());
						}
					}
				}
			});
		}
		
		{
			JLabel sfTimeoutLabel = new JLabel("Skeinforge timeout: ");
			content.add(sfTimeoutLabel,"split, gap unrelated");
			int value = Base.preferences.getInt("replicatorg.skeinforge.timeout", -1);
			JFormattedTextField sfTimeoutField = new JFormattedTextField(Base.getLocalFormat());
			sfTimeoutField.setValue(new Integer(value));
			content.add(sfTimeoutField,"wrap 10px, growx");
			String sfTimeoutHelp = "<html><small><em>" +
				"The Skeinforge timeout is the number of seconds that replicatorg will wait while the<br>" +
				"Skeinforge preferences window is open. If you find that RepG freezes after editing profiles<br>" +
				"you can set this number greater than -1 (-1 means no timeout)." +
				"</em></small></html>";
			sfTimeoutField.setToolTipText(sfTimeoutHelp);
			sfTimeoutLabel.setToolTipText(sfTimeoutHelp);
//			content.add(new JLabel(sfTimeoutHelp),"growx,wrap");
			sfTimeoutField.setColumns(10);
			sfTimeoutField.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt.getPropertyName() == "value") {
						try {
							Integer v = (Integer)evt.getNewValue();
							if (v == null) return;
							Base.preferences.putInt("replicatorg.skeinforge.timeout", v.intValue());
						} catch (ClassCastException cce) {
							Base.logger.warning("Unexpected value type: "+evt.getNewValue().getClass().toString());
						}
					}
				}
			});
		}
		
		{
			content.add(new JLabel("Debugging level (default INFO):"),"split");
			content.add(makeDebugLevelDropdown(), "wrap");

			final JCheckBox logCb = new JCheckBox("Log to file");
			logCb.setSelected(Base.preferences.getBoolean("replicatorg.useLogFile",false));
			content.add(logCb, "split");
			logCb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Base.preferences.putBoolean("replicatorg.useLogFile",logCb.isSelected());
				}
			});
			
			final JLabel logPathLabel = new JLabel("Log file name: "); 
			content.add(logPathLabel,"split");
			logPathField = new JTextField(34);
			content.add(logPathField,"growx, wrap 10px");
			logPathField.setEnabled(logCb.isSelected());
			logPathLabel.setEnabled(logCb.isSelected());

			logCb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JCheckBox box = (JCheckBox)e.getSource();
					logPathField.setEnabled(box.isSelected());
					logPathLabel.setEnabled(box.isSelected());
				}
			});

		}
		
		{
			final JCheckBox preheatCb = new JCheckBox("Preheat builds");
			content.add(preheatCb, "split");
			
			preheatCb.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent arg0) {
					Base.preferences.putBoolean("build.doPreheat", preheatCb.isSelected());
				}
			});
			preheatCb.setSelected(Base.preferences.getBoolean("build.doPreheat", false));
			
			final JLabel t0Label = new JLabel("Toolhead0:");
			final JLabel t1Label = new JLabel("Toolhead1:");
			final JLabel pLabel = new JLabel("Platform:");
			
			Integer t0Value = Base.preferences.getInt("build.preheatTool0", 75);
			Integer t1Value = Base.preferences.getInt("build.preheatTool1", 75);
			Integer pValue = Base.preferences.getInt("build.preheatPlatform", 75);
			
			final JFormattedTextField t0Field = new JFormattedTextField(Base.getLocalFormat());
			final JFormattedTextField t1Field = new JFormattedTextField(Base.getLocalFormat());
			final JFormattedTextField pField = new JFormattedTextField(Base.getLocalFormat());

			t0Field.setValue(t0Value);
			t1Field.setValue(t1Value);
			pField.setValue(pValue);
			
			// let's avoid creating too many ActionListeners, also is fewer lines (and just as clear)!
			ActionListener a = new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent ae) {
					double target;
					if(ae.getSource() == t0Field)
					{
						// casting to long because that's what it is
						target = (Long)t0Field.getValue();
						target = confirmTemperature(target,"temperature.acceptedLimit",200.0);
						if (target == Double.MIN_VALUE) {
							return;
						}
						Base.preferences.putInt("build.preheatTool0", (int)target);
					}
					else if(ae.getSource() == t1Field)
					{
						// casting to long because that's what it is
						target = (Long)t1Field.getValue();
						target = confirmTemperature(target,"temperature.acceptedLimit",200.0);
						if (target == Double.MIN_VALUE) {
							return;
						}
						Base.preferences.putInt("build.preheatTool1", (int)target);
					}
					else if(ae.getSource() == pField)
					{
						// casting to long because that's what it is
						target = (Long)pField.getValue();
						target = confirmTemperature(target,"temperature.acceptedLimit.bed",90.0);
						if (target == Double.MIN_VALUE) {
							return;
						}
						Base.preferences.putInt("build.preheatPlatform", (int)target);
					}
				}
			};
			t0Field.addActionListener(a);
			t1Field.addActionListener(a);
			pField.addActionListener(a);

			content.add(t0Label, "split, gap 20px");
			content.add(t0Field, "split, growx");
			content.add(t1Label, "split, gap unrelated");
			content.add(t1Field, "split, growx");
			content.add(pLabel, "split, gap unrelated");
			content.add(pField, "split, growx, wrap 10px");
		}
		
		{
			JButton b = new JButton("Select Python interpreter...");
			content.add(b,"spanx,wrap 10px");
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					SwingPythonSelector sps = new SwingPythonSelector(PreferencesWindow.this);
					String path = sps.selectFreeformPath();
					if (path != null) {
						PythonUtils.setPythonPath(path);
					}
				}
			});
		}

		addInitialFilePrefs(content);

		JButton allPrefs = new JButton("View All Prefs");
		content.add(allPrefs, "split");
		allPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFrame advancedPrefs = new AdvancedPrefs();
				advancedPrefs.setVisible(true);
			}
		});

		JButton delPrefs = new JButton("Restore all defaults (includes driver choice, etc.)");
		content.add(delPrefs);
		delPrefs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Base.resetPreferences();
				showCurrentSettings();
			}
		});

		JButton button;
		button = new JButton("Close");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				applyFrame();
				dispose();
			}
		});
		content.add(button, "tag ok");

		basicVSadvanced.add(basic, "Basic");
		basicVSadvanced.add(advanced, "Advanced");
		getContentPane().add(basicVSadvanced);
		
		showCurrentSettings();

		// closing the window is same as hitting cancel button

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		ActionListener disposer = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				dispose();
			}
		};
		Base.registerWindowCloseKeys(getRootPane(), disposer);

		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2,
				(screen.height - getHeight()) / 2);

		// handle window closing commands for ctrl/cmd-W or hitting ESC.

		getContentPane().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				KeyStroke wc = MainWindow.WINDOW_CLOSE_KEYSTROKE;
				if ((e.getKeyCode() == KeyEvent.VK_ESCAPE)
						|| (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
					dispose();
				}
			}
		});
	}

	/**
	 * Change internal settings based on what was chosen in the prefs, then send
	 * a message to the editor saying that it's time to do the same.
	 */
	public void applyFrame() {
		// put each of the settings into the table
		String newSizeText = fontSizeField.getText();
		try {
			int newSize = Integer.parseInt(newSizeText.trim());
			String fontName = Base.preferences.get("editor.font","Monospaced,plain,12");
			if (fontName != null) {
				String pieces[] = fontName.split(",");
				pieces[2] = String.valueOf(newSize);
				StringBuffer buf = new StringBuffer();
				for (String piece : pieces) {
					if (buf.length() > 0) buf.append(",");
					buf.append(piece);
				}
				Base.preferences.put("editor.font", buf.toString());
			}

		} catch (Exception e) {
			Base.logger.warning("ignoring invalid font size " + newSizeText);
		}
		String origUpdateUrl = Base.preferences.get("replicatorg.updates.url", "");
		if (!origUpdateUrl.equals(firmwareUpdateUrlField.getText())) {
			FirmwareUploader.invalidateFirmware();
			Base.preferences.put("replicatorg.updates.url",firmwareUpdateUrlField.getText());
			FirmwareUploader.checkFirmware(); // Initiate a new firmware check
		}

		String logPath = logPathField.getText();
		Base.preferences.put("replicatorg.logpath", logPath);
		Base.setLogFile(logPath);
		
		editor.applyPreferences();
	}

	public void showFrame(MainWindow editor) {
		this.editor = editor;

		// set all settings entry boxes to their actual status
		setVisible(true);
	}

}
