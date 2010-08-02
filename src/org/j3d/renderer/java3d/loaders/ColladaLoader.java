package org.j3d.renderer.java3d.loaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.net.URL;
import java.util.logging.Level;

import org.j3d.loaders.collada.ColladaParser;
import org.xml.sax.InputSource;

import replicatorg.app.Base;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.LoaderBase;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;

public class ColladaLoader extends LoaderBase {

	public Scene load(String filename) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException {
		System.err.println("Loading COLLADA from "+filename);
		File file = new File(filename);
		return loadInternal(new InputSource(new FileInputStream(file)));
	}

	public Scene load(URL url) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException {
		assert(url != null);
        try
        {
			InputStream is = url.openStream();
			return loadInternal(new InputSource(is));
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
		return loadInternal(new InputSource(reader));
	}

	public Scene loadInternal(InputSource is) {
		System.err.println("LOADING COLLADA");
		ColladaParser parser = new ColladaParser();
		parser.parse(is);
		return null;
	}

}
