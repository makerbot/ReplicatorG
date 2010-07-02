package replicatorg.model.j3d;

import java.io.OutputStream;
import java.io.PrintWriter;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TriangleArray;

import replicatorg.app.Base;

public class StlAsciiWriter extends ModelWriter {
	public StlAsciiWriter(OutputStream ostream) {
		super(ostream);
	}
	
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
		
		w.printf("solid %s\n", name);
		int faces = g.getVertexCount()/3;
		float[] norm = new float[3];
		double[] coord = new double[3];
		for (int faceIdx = 0; faceIdx < faces; faceIdx++) {
			g.getNormal(faceIdx*3, norm);
			w.printf("  facet normal %e %e %e\n", norm[0],norm[1],norm[2]);
			w.printf("    outer loop\n");
			g.getCoordinate(faceIdx*3, coord);
			w.printf("      vertex %e %e %e\n", coord[0],coord[1],coord[2]);
			g.getCoordinate((faceIdx*3)+1, coord);
			w.printf("      vertex %e %e %e\n", coord[0],coord[1],coord[2]);
			g.getCoordinate((faceIdx*3)+2, coord);
			w.printf("      vertex %e %e %e\n", coord[0],coord[1],coord[2]);
			w.printf("    endloop\n");
			w.printf("  endfacet\n");
		}
		w.printf("endsolid %s\n", name);
		w.close();
	}

}
