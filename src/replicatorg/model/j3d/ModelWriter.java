package replicatorg.model.j3d;

import java.io.IOException;
import java.io.OutputStream;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;

public abstract class ModelWriter {
	protected OutputStream ostream;
	
	public ModelWriter(OutputStream ostream) {
		this.ostream = ostream;
	}
	
	public void close() throws IOException {
		ostream.close();
	}
	
	/**
	 * Write the given shape to the output stream, applying the given transform to all points.
	 * @param shape
	 * @param transform
	 */
	abstract public void writeShape(Shape3D shape, Transform3D transform);
}