package replicatorg.app.ui;

public interface GuiConstants {
	static public final int BUTTON_HEIGHT = 24;
	/**
	 * Standardized width for buttons. Mac OS X 10.3 wants 70 as its default,
	 * Windows XP needs 66, and Linux needs 76, so 76 seems proper.
	 */
	static public final int BUTTON_WIDTH = 76;

	// indents and spacing standards. these probably need to be modified
	// per platform as well, since macosx is so huge, windows is smaller,
	// and linux is all over the map

	static public final int GUI_BIG = 13;

	static public final int GUI_BETWEEN = 10;

	static public final int GUI_SMALL = 6;

	// value for the size bars, buttons, etc
	static public final int GRID_SIZE = 33;
	
}
