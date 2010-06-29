package org.j3d.renderer.java3d.loaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.j3d.loaders.stl.STLFileReader;

import replicatorg.app.Base;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.LoaderBase;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;

public class GTSLoader extends LoaderBase {

	public Scene load(String filename) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException {
		File file = new File(filename);
		return load(new BufferedReader(new FileReader(file)));
	}

	public Scene load(URL url) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException {
		assert(url != null);
        try
        {
			InputStream is = url.openStream();
			return load(new BufferedReader(new InputStreamReader(is)));
        }
        catch( InterruptedIOException ie )
        {
            // user cancelled loading
            return null;
        }
        catch( IOException e )
        {
        	Base.logger.log(Level.SEVERE,"Could not open URL "+url.toString(),e);
        	return null;
        }
    }

	public Scene load(Reader reader) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException {
		// TODO
		return null;
	}

}
