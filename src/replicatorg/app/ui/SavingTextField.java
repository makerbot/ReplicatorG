package replicatorg.app.ui;

public class SavingTextField extends ActionTextField {
	final String parameterName;
	
	public SavingTextField(String parameterName, String text, int columns) {
		super(text, columns);

		this.parameterName = parameterName;
	}

	@Override
	public void doSaveEvent() {
		String value = getText();
	}
}
