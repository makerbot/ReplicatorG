package replicatorg.app.ui.modeling;

import javax.swing.Icon;
import javax.swing.JPanel;

public interface Tool {
	String getTitle();
	String getButtonName();
	Icon getButtonIcon();
	String getInstructions();
	JPanel getControls();
}
