package replicatorg.app.ui;

import replicatorg.app.Base;

public class SavingTextField extends ActionTextField {
	final String parameterName;
	
	public SavingTextField(String parameterName, String text, int columns) {
		super(text, columns);

		this.parameterName = parameterName;
	}

	@Override
	public void doSaveEvent() {
		String value = getText();
		Base.logger.fine("here: " + parameterName + "=" + value);
		Base.preferences.put(parameterName, value);
	}

}
