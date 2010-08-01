package replicatorg.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.j3d.renderer.java3d.loaders.ObjLoader;
import org.j3d.renderer.java3d.loaders.STLLoader;

import replicatorg.app.Base;
import replicatorg.app.ui.modeling.EditingModel;
import replicatorg.model.j3d.StlAsciiWriter;

import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.LoaderBase;
import com.sun.j3d.loaders.Scene;

public class BuildModel extends BuildElement {

	private File file;
	private Transform3D transform = new Transform3D();
	private Shape3D shape = null;
	private EditingModel editListener = null;
	
	public void setEditListener(EditingModel eModel) {
		editListener = eModel;
	}
	
	BuildModel(Build build, File file) {
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

	// Attempt to load the file with the given loader.  Should return
	// null if the given loader can't identify the file as being of
	// the correct type.
	private Shape3D loadShape(Loader loader) {
		Scene scene = null;
		try {
			scene = loader.load(file.getCanonicalPath());
		} catch (Exception e) {
			Base.logger.log(Level.FINE,
					"Could not load "+file.getPath()+
					" with "+ loader.getClass().getSimpleName(),e);
			return null;
		}
		if (scene == null) { return null; }
		return (Shape3D)scene.getSceneGroup().getChild(0);
	}

	Map<String,Loader> loaderExtensionMap = new HashMap<String,Loader>();
	{
		loaderExtensionMap.put("stl",new STLLoader());
		loaderExtensionMap.put("obj",new ObjLoader());
	}
	
	private void loadShape() {
		STLLoader loader = new STLLoader();
		Shape3D candidate = loadShape(loader);
		if (candidate != null) { shape = candidate; }
	}

	public Transform3D getTransform() { return transform; }
	
	class UndoEntry implements UndoableEdit {
		Transform3D before;
		Transform3D after;
		String description;
		boolean newOp;
		
		// The newOp flag is set at the start of every drag or every button operation.  NewOps will never
		// be merged into the undo op at the top of the stack.
		public UndoEntry(Transform3D before, Transform3D after, String description, boolean newOp) {
			this.before = new Transform3D(before);
			this.after= new Transform3D(after);
			this.description = description;
			this.newOp = newOp;
		}
		
		@Override
		public boolean addEdit(UndoableEdit edit) {
			if (edit instanceof UndoEntry) {
				UndoEntry ue = (UndoEntry)edit;
				if (!ue.newOp && description == ue.description) {
					after = ue.after;
					return true;
				}
			}
			return false;
		}
		
		@Override
		public boolean canRedo() {
			return true;
		}
		@Override
		public boolean canUndo() {
			return true;
		}
		@Override
		public void die() {
			
		}
		@Override
		public String getPresentationName() {
			return description;
		}
		@Override
		public String getRedoPresentationName() {
			return "Redo "+getPresentationName();
		}
		@Override
		public String getUndoPresentationName() {
			return "Undo "+getPresentationName();
		}
		@Override
		public boolean isSignificant() {
			return true;
		}
		@Override
		public void redo() throws CannotRedoException {
			doEdit(after);
		}
		@Override
		public boolean replaceEdit(UndoableEdit edit) {
			return false;
		}
		@Override
		public void undo() throws CannotUndoException {
			doEdit(before);
		}
	}
		
	public void setTransform(Transform3D t, String description, boolean newOp) {
		if (transform.equals(t)) return;
		undo.addEdit(new UndoEntry(transform,t,description, newOp));
		transform.set(t);
		setModified(true);
		if (editListener != null) {
			editListener.modelTransformChanged();
		}
	}

	public void doEdit(Transform3D edit) {
		transform.set(edit);
		setModified(undo.canUndo());
		editListener.modelTransformChanged();
	}

	public void save() {
		saveInternal(file);
	}

	public void saveAs(File f) {
		if (saveInternal(f)) {
			file = f;
		}
	}

	private boolean saveInternal(File f) {
		try {
			FileOutputStream ostream = new FileOutputStream(f);
			Base.logger.info("Writing to "+f.getCanonicalPath()+".");
			StlAsciiWriter saw = new StlAsciiWriter(ostream);
			saw.writeShape(getShape(), getTransform());
			ostream.close();
			undo = new UndoManager();
			setModified(false);
			return true;
		} catch (FileNotFoundException fnfe) {
			Base.logger.log(Level.SEVERE,"Error during save",fnfe);
		} catch (IOException ioe) {
			Base.logger.log(Level.SEVERE,"Error during save",ioe);
		}
		return false;
	}

	@Override
	void writeToStream(OutputStream ostream) {
		// TODO Auto-generated method stub
		
	}
}
