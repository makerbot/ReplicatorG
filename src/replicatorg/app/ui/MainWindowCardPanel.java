package replicatorg.app.ui;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JPanel;

import replicatorg.app.Base;

/**
 * This class is a workaround for a problem with OpenJDK and Java3D on Ubuntu 11.04, 
 * where the preview window wouldn't show up when there are two cards in the JPanel
 * with a CardLayout. To work around that, this will store the widgets in componentMap
 * instead, and only add them to the JPanel when it's time to show them.  
 * 
 * Since this issue only occurs on linux, the add and show methods will just 
 * pass through to cardPanel and cardLayout if Base.isLinux() is false.
 * 
 * @author George Schneeloch
 *
 */
public class MainWindowCardPanel
{
	private final JPanel cardPanel;
	private final CardLayout cardLayout;
	
	private final HashMap<String, Component> componentMap;
	
	public MainWindowCardPanel()
	{
		cardLayout = new CardLayout();
		cardPanel = new JPanel(cardLayout);
		componentMap = new HashMap<String, Component>();
	}

	public void add(Component component, String key) {
		if (Base.isLinux())
		{
			componentMap.put(key, component);
		
			show(key);
		}
		else
		{
			cardPanel.add(component, key);
		}
	}

	/**
	 * Get the JPanel used by this class. This method should not be used to
	 * add or remove components, or to show a card; use add() and show() instead
	 * @return
	 */
	public Component getPanel() {
		return cardPanel;
	}

	public void show(String key)
	{
		if (Base.isLinux())
		{
			if (componentMap.containsKey(key))
			{
				Component component = componentMap.get(key);
				// there should only be one widget in cardPanel at any time, 
				// but just in case, make them all invisible
				for (Component otherComponent : cardPanel.getComponents())
				{
					if (otherComponent.isVisible())
					{
						otherComponent.setVisible(false);
					}
				}
				cardPanel.removeAll();
				cardPanel.add(component, key);

				component.setVisible(true);
				component.validate();
			}
		}
		else
		{
			cardLayout.show(cardPanel, key);
		}
	}
}
