package replicatorg.model;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;

import org.j3d.renderer.java3d.loaders.STLLoader;

import replicatorg.app.Base;

import com.sun.j3d.loaders.Scene;

public class BuildModel implements BuildElement {

	private File file;
	private Transform3D transform = new Transform3D();
	private Shape3D shape = null;
	
	BuildModel(File file) {
		this.file = file;
	}		

	public BuildElement.Type getType() {
		return BuildElement.Type.MODEL;
	}

	public String getPath() {
		try {
			return file.getCanonicalPath();
		} catch (IOException ioe) { return null; }
	}

	public Shape3D getShape() {
		if (shape == null) { 
			loadShape();
		}
		return shape;
	}
	
	private void loadShape() {
		STLLoader loader = new STLLoader();
		Scene scene = null;
		try {
			scene = loader.load(file.getCanonicalPath());
		} catch (Exception e) {
			Base.logger.log(Level.SEVERE,"Error loading model "+file.getPath(),e);
		}
		if (scene == null) { return; }
		shape = (Shape3D)scene.getSceneGroup().getChild(0);
	}

	public Transform3D getTransform() { return transform; }
	public void setTransform(Transform3D t) {
		transform.set(t);
	}
}
