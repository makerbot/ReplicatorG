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
import java.net.URL;
import java.util.Vector;
import java.util.logging.Level;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import replicatorg.app.Base;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.LoaderBase;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.SceneBase;

public class ObjLoader extends LoaderBase {

	@Override
	public Scene load(String filename) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException {
		File file = new File(filename);
		return load(new BufferedReader(new FileReader(file)));
	}

	@Override
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

	@Override
	public Scene load(Reader r) throws FileNotFoundException,
			IncorrectFormatException, ParsingErrorException {
		Vector<Point3d> vertices = new Vector<Point3d>();
		Vector<Vector3f> normals = new Vector<Vector3f>();
		Vector<String[]> tris = new Vector<String[]>();
		BufferedReader br = new BufferedReader(r);
		try {
			for (String rl = br.readLine(); rl != null; rl = br.readLine()) {
				if (rl.length() == 0) continue;
				char first = rl.charAt(0);
				if (first == '#') { continue; } // comment
				String[] parts = rl.split(" ");
				if ("v".equals(parts[0])) {
					vertices.add(new Point3d(Double.parseDouble(parts[1]),
							Double.parseDouble(parts[2]),
							Double.parseDouble(parts[3])));
				} else if ("vn".equals(parts[0])) {
					normals.add(new Vector3f(Float.parseFloat(parts[1]),
							Float.parseFloat(parts[2]),
							Float.parseFloat(parts[3])));
				} else if ("f".equals(parts[0])) {
					// We decompose faces into a simple triangulation:
					// Given 0 1 2 3 ... describing the perimeter in counter-
					// clockwise order, we choose triangles 
					// 0 1 2, 0 2 3, 0 3 4, 0 4 5 ...
					for (int idx = 2; (idx+1) < parts.length; idx++) {
						String verts[] = { parts[1], parts[idx], parts[idx+1] };
						tris.add(verts);
					}
				}
			}
            final SceneBase scene = new SceneBase( );
            final BranchGroup bg = new BranchGroup( );
            final TriangleArray geometry = new TriangleArray
            (
                3 * tris.size(),
                TriangleArray.NORMALS | TriangleArray.COORDINATES
            );
            int idx = 0;
            for (String[] tri : tris) {
            	for (String v : tri) {
            		String[] components = v.split("/");
            		int vertexIdx = Integer.parseInt(components[0]) - 1;
            		// Lookup and populate vertex
            		geometry.setCoordinate(idx, vertices.elementAt(vertexIdx));
            		if (components.length > 2) {
            			int normalIdx = Integer.parseInt(components[2]) - 1;
            			geometry.setNormal(idx, normals.elementAt(normalIdx));
            		} else {
            			// Compute from the triangle vertices
            			// Todo.
            		}
            		idx++;
            	}
            }
            final Shape3D shape = new Shape3D( geometry );
            bg.addChild( shape );
            scene.addNamedObject("Object", shape);
            scene.setSceneGroup(bg);
            return scene;

		} catch (IOException e) {
			Base.logger.log(Level.SEVERE,"I/O error reading .OBJ",e);
		}
		
		return null;
	}

}
