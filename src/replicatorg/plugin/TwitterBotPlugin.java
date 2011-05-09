package replicatorg.plugin;

import java.lang.reflect.Method;

import replicatorg.app.GCode;

// Example plug-in that handles custom m-codes
public class TwitterBotPlugin implements MCodePlugin {

	// TwitterBot extension variables
    protected Class<?> extClass;
    protected Object objExtClass;
	public static final char TB_CODE = 'M';
	public static final int TB_INIT = 997;
	public static final int TB_MESSAGE = 998;
	public static final int TB_CLEANUP = 999;
	
	public static final int codes[] = {TB_INIT, TB_MESSAGE, TB_CLEANUP};
	
	@Override
	public int[] getAcceptedMCodes() {
		// TODO Auto-generated method stub
		return codes;
	}

	@Override
	public void processMCode(GCode mcode) {
		if (!mcode.hasCode(TB_CODE)) {
			return;
		}

		switch ((int) mcode.getCodeValue(TB_CODE)) {
		case TB_INIT:
			// initialize extension
			// Syntax: M997 ClassName param,param,param,etc
			// your class needs to know how to process the params as it will just
			//receive a string.
			//This code uses a space delimiter
			//To do: should be more general purpose
			try {
				String params[] = mcode.getCommand().split(" "); //method is param[1]
				extClass = Class.forName(params[1]); //class name
				objExtClass = extClass.newInstance();
				String methParam = params[2];  //initialization params
				Method extMethod = extClass.getMethod("initialize", new Class[] {String.class});
				extMethod.invoke(objExtClass, new Object[] {methParam});
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case TB_MESSAGE:
			// call extension
			// Syntax: M998 Method_name 'Content goes here'
			// passed content is single quote delimited
			// To do: clean up, should be more flexible

			try {
				String params[] = mcode.getCommand().split(" "); //method is param[1]
				String params2[] = mcode.getCommand().split("\\'"); //params to pass are params2[1]

				String methParam = params2[1]; //params to pass
				Method extMethod = extClass.getMethod(params[1], new Class[] {String.class});  //method to call
				extMethod.invoke(objExtClass, new Object[] {methParam});
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;

		case TB_CLEANUP:
			// cleanup extension
			// are explicit nulls needed?
			extClass = null;
			objExtClass = null;
			break;
		}
	}
}
