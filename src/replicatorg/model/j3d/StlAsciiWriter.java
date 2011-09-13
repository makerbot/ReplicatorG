package replicatorg.model.j3d;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import replicatorg.app.Base;

public class StlAsciiWriter extends ModelWriter {
	public StlAsciiWriter(OutputStream ostream) {
		super(ostream);
	}
	
	Locale l = Locale.US;
	
	@Override
	public void writeShape(Shape3D shape, Transform3D transform) {
		PrintWriter w = new PrintWriter(ostream);
		TriangleArray g = getGeometry(shape);
		if (g == null) {
			Base.logger.info("Couldn't find valid geometry during save.");
			return;
		}
		// Oops-- this is part of the v1.4 API.  Until we ship a new J3D w/ the 
		// Mac release, fall back to a default.
		// String name = shape.getName();
		// if (name == null) { name = "Default"; }
		String name = "Default";
		
		w.printf(l,"solid %s\n", name);
		int faces = g.getVertexCount()/3;
		float[] norm = new float[3];
		double[] coord = new double[3];
		for (int faceIdx = 0; faceIdx < faces; faceIdx++) {
			g.getNormal(faceIdx*3, norm);
			Vector3f norm3f = new Vector3f(norm);
			transform.transform(norm3f);
			norm3f.normalize();
			w.printf(l,"  facet normal %e %e %e\n", norm3f.x,norm3f.y,norm3f.z);
			w.printf(l,"    outer loop\n");
			Point3d face3d;
			g.getCoordinate(faceIdx*3, coord);
			face3d = new Point3d(coord);
			transform.transform(face3d);
			w.printf(l,"      vertex %e %e %e\n", face3d.x,face3d.y,face3d.z);
			g.getCoordinate((faceIdx*3)+1, coord);
			face3d = new Point3d(coord);
			transform.transform(face3d);
			w.printf(l,"      vertex %e %e %e\n", face3d.x,face3d.y,face3d.z);
			g.getCoordinate((faceIdx*3)+2, coord);
			face3d = new Point3d(coord);
			transform.transform(face3d);
			w.printf(l,"      vertex %e %e %e\n", face3d.x,face3d.y,face3d.z);
			w.printf(l,"    endloop\n");
			w.printf(l,"  endfacet\n");
		}
		w.printf(l,"endsolid %s\n", name);
		w.close();
	}

}
