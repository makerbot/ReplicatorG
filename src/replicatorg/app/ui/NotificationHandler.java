package replicatorg.app.ui;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import replicatorg.app.Base;

/**
 * Abstraction over notifications (messages). Can either use JOptionPane popup
 * windows or use System Tray notifications. Configured by config property
 * "ReplicatorG.preferSystemTrayNotifications". If system tray is requested but
 * not available falls back to JOptionPane.
 * 
 * Note - Errors <em>always</em> use a popup, since user probably doesn't want a
 * fleeting notification
 * 
 * @author sw1nn
 * 
 */
public interface NotificationHandler {

	void showMessage(String title, String message);
	
	void showWarning(String title, String message, Throwable e);
	
	void showError(String title, String message, Throwable e);
	
	public static class Factory {
		private Factory() { throw new AssertionError(); }
		
		public static NotificationHandler getHandler(MainWindow editor, boolean preferSystray) {
			if (preferSystray && SystemTray.isSupported()) {
				return new SystemTrayNotifactionHandler(editor);
			} else {
				return new JOptionPaneNotificationHandler();
			}
		}
	}

	static abstract class BaseNotificationHandler implements
			NotificationHandler {

		private BaseNotificationHandler() {
		}

		@Override
		public void showMessage(String title, String message) {
			if (title == null)
				title = "Message";

			showMessage0(title, message);
		}

		@Override
		public void showWarning(String title, String message, Throwable e) {
			if (title == null)
				title = "Warning";
			
			showWarning0(title, message, e);
		}

		@Override
		public void showError(String title, String message, Throwable t) {
			if (title == null)
				title = "Error";

			JOptionPane.showMessageDialog(new Frame(), message, title,
					JOptionPane.ERROR_MESSAGE);

			if (t != null)
				t.printStackTrace();

		}

		protected abstract void showMessage0(String title, String message);

		protected abstract void showWarning0(String title, String message,
				Throwable e);

	}

	static class JOptionPaneNotificationHandler extends BaseNotificationHandler {

		private JOptionPaneNotificationHandler() {
		}

		@Override
		protected void showMessage0(String title, String message) {
			JOptionPane.showMessageDialog(new Frame(), message, title,
					JOptionPane.INFORMATION_MESSAGE);
		}

		@Override
		protected void showWarning0(String title, String message, Throwable t) {
			JOptionPane.showMessageDialog(new Frame(), message, title,
					JOptionPane.WARNING_MESSAGE);
		}

	}

	static class SystemTrayNotifactionHandler extends BaseNotificationHandler {

		private TrayIcon trayIcon;

		private SystemTrayNotifactionHandler(final MainWindow editor) {
			SystemTray systemTray = SystemTray.getSystemTray();

			PopupMenu popup = new PopupMenu();
			MenuItem defaultItem = new MenuItem("Exit");
			defaultItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					editor.handleQuit();
				}
			});

			popup.add(defaultItem);
			// set the window icon
			Image icon = Base.getImage("images/icon.gif", editor);

			trayIcon = new TrayIcon(icon, editor.getTitle(), popup);
			trayIcon.setImageAutoSize(true);
			try {
				systemTray.add(trayIcon);
			} catch (AWTException e1) {
				e1.printStackTrace();
			}
		}

		@Override
		protected void showMessage0(String title, String message) {
			trayIcon.displayMessage(title, message, MessageType.INFO);
		}

		@Override
		protected void showWarning0(String title, String message, Throwable e) {
			trayIcon.displayMessage(title, message, MessageType.WARNING);
		}
	}
}
