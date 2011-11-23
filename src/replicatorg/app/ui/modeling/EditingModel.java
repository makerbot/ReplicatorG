package replicatorg.app.ui.modeling;

import java.awt.Color;
import java.util.Enumeration;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import replicatorg.app.Base;
import replicatorg.app.ui.MainWindow;
import replicatorg.model.BuildModel;

/**
 * A wrapper for displaying and editing an underlying model object.
 * @author phooky
 *
 */
public class EditingModel {
	public class ReferenceFrame {
		public Point3d origin;
		public Vector3d zAxis;
		
		public ReferenceFrame() {
			origin = new Point3d();
			zAxis = new Vector3d(0d,0d,1d);
		}
	}
	
	/**
	 * The underlying model being edited.
	 */
	final protected BuildModel model;

	/**
	 * Material definition for the model, maintained so that we can update the color without reloading.
	 */
	Material objectMaterial = null;

	/** The group which represents the displayable subtree.
	 */
	private BranchGroup group = null;

	/**
	 * The transform group for the shape.  The enclosed transform should be applied to the shape before:
	 * * bounding box calculation
	 * * saving out the STL for skeining
	 */
	private TransformGroup shapeTransform = new TransformGroup();
	
	/** We maintain a link to the main window to update the undo/redo buttons.  Kind of silly, but
	 * there it is.
	 */
	private final MainWindow mainWindow;
	
	public EditingModel(BuildModel model, final MainWindow mainWindow) {
		this.model = model;
		this.mainWindow = mainWindow;
		model.setEditListener(this);
	}
	
	/**
	 * Cache of the original shape from the model.
	 */
	Shape3D originalShape;

	
	/**
	 * Create the branchgroup that will display the object.
	 */
	private BranchGroup makeShape(BuildModel model) {
		originalShape = model.getShape();
		if (originalShape.getGeometry() == null) {
			BranchGroup wrapper = new BranchGroup();
			wrapper.setCapability(BranchGroup.ALLOW_DETACH);
			wrapper.compile();
			return wrapper;
		}

		Shape3D solidShape = (Shape3D)originalShape.cloneTree();
		solidShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		solidShape.getGeometry().setCapability(GeometryArray.ALLOW_COUNT_READ);
		solidShape.getGeometry().setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		solidShape.getGeometry().setCapability(GeometryArray.ALLOW_NORMAL_READ);
		
		objectMaterial = new Material();
		objectMaterial.setCapability(Material.ALLOW_COMPONENT_WRITE);
		
		updateModelColor();
		Appearance solid = new Appearance();
		solid.setMaterial(objectMaterial);
		PolygonAttributes pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setBackFaceNormalFlip(true);
		solid.setPolygonAttributes(pa);
		solidShape.setAppearance(solid);

		BranchGroup wrapper = new BranchGroup();

		shapeTransform = new TransformGroup();
		shapeTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		shapeTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		shapeTransform.setCapability(TransformGroup.ALLOW_CHILDREN_READ);

		wrapper.addChild(shapeTransform);

		shapeTransform.addChild(solidShape);
		wrapper.setCapability(BranchGroup.ALLOW_DETACH);
		wrapper.compile();
		return wrapper;
	}

	public BuildModel getBuildModel() { return model; }
	
	public void updateModelColor() {
		if (objectMaterial != null) {
			Color modelColor = new Color(Base.preferences.getInt("ui.modelColor",-19635));
			
			objectMaterial.setAmbientColor(new Color3f(modelColor));
			objectMaterial.setDiffuseColor(new Color3f(modelColor));
		}
	}
	
	public BranchGroup getGroup() {
		if (group == null) {
			group = makeShape(model);
		}
		
		return group;
	}
		
	public ReferenceFrame getReferenceFrame() {
		Transform3D translate = new Transform3D();
		shapeTransform.getTransform(translate);
		ReferenceFrame rf = new ReferenceFrame();
		translate.transform(rf.origin);
		translate.transform(rf.zAxis);
		return rf;
	}

	/**
	 * Transform the given transform to one that operates on the centroid of the object.
	 * @param transform
	 * @param name
	 * @return
	 */
	public Transform3D transformOnCentroid(Transform3D transform) {
		Transform3D old = new Transform3D();
		Transform3D t1 = new Transform3D();
		Transform3D t2 = new Transform3D();

		Vector3d t1v = new Vector3d(getCentroid());
		t1v.negate();
		t1.setTranslation(t1v);
		Vector3d t2v = new Vector3d(getCentroid());
		t2.setTranslation(t2v);
		shapeTransform.getTransform(old);
		
		Transform3D composite = new Transform3D();
		composite.mul(t2);
		composite.mul(transform);
		composite.mul(t1);
		composite.mul(old);
		return composite;
	}

	/**
	 * Transform the given transform to one that operates on the centroid of the object.
	 * @param transform
	 * @param name
	 * @return
	 */
	public Transform3D transformOnBottom(Transform3D transform) {
		Transform3D old = new Transform3D();
		Transform3D t1 = new Transform3D();
		Transform3D t2 = new Transform3D();

		Vector3d t1v = new Vector3d(getBottom());
		t1v.negate();
		t1.setTranslation(t1v);
		Vector3d t2v = new Vector3d(getBottom());
		t2.setTranslation(t2v);
		shapeTransform.getTransform(old);
		
		Transform3D composite = new Transform3D();
		composite.mul(t2);
		composite.mul(transform);
		composite.mul(t1);
		composite.mul(old);
		return composite;
	}

	public void rotateObject(double turntable, double elevation) {
		// Skip identity translations
		if (turntable == 0.0 && elevation == 0.0) { return; }
		Transform3D r1 = new Transform3D();
		Transform3D r2 = new Transform3D();
		r1.rotX(elevation);
		r2.rotZ(turntable);
		r2.mul(r1);
		r2 = transformOnCentroid(r2);
		model.setTransform(r2,"rotation",isNewOp());
	}
	
	public void rotateObject(AxisAngle4d angle) {
		Transform3D t = new Transform3D();
		t.setRotation(angle);
		t = transformOnCentroid(t);
		model.setTransform(t, "rotation",isNewOp());
	}

	public void modelTransformChanged() {
		shapeTransform.setTransform(model.getTransform());
		mainWindow.updateUndo();
	}
	
	public void translateObject(double x, double y, double z) {
		// Skip identity translations
		if (x == 0.0 && y == 0.0 && z == 0.0) { return; }
		invalidateBounds();
		Transform3D translate = new Transform3D();
		translate.setZero();
		translate.setTranslation(new Vector3d(x,y,z));
		Transform3D old = new Transform3D();
		shapeTransform.getTransform(old);
		old.add(translate);
		model.setTransform(old,"move",isNewOp());
	}

	private BoundingBox getBoundingBox(Group group) {
		return getBoundingBox(group, new Transform3D());
	}
	
	private BoundingBox getBoundingBox(Shape3D shape, Transform3D transformation) {
		BoundingBox bb = null;
		Enumeration<?> geometries = shape.getAllGeometries();
		while (geometries.hasMoreElements()) {
			Geometry g = (Geometry)geometries.nextElement();
			if (g instanceof GeometryArray) {
				GeometryArray ga = (GeometryArray)g;
				Point3d p = new Point3d();
				for (int i = 0; i < ga.getVertexCount(); i++) {
					ga.getCoordinate(i,p);
					transformation.transform(p);
					if (bb == null) { bb = new BoundingBox(p,p); }
					bb.combine(p);
				}
			}
		}
		return bb;
	}

	/**
	 * Flip the object tree around the Z axis.  This is particularly useful when
	 * breaking a print into two parts.
	 */
	public void flipZ() {
		Transform3D flipZ = new Transform3D();
		flipZ.rotY(Math.PI);
		flipZ = transformOnCentroid(flipZ);
		model.setTransform(flipZ,"flip",isNewOp());
	}

	public void mirrorX() {
		Transform3D t = new Transform3D();
		Vector3d v = new Vector3d(-1d,1d,1d);
		t.setScale(v);
		t = transformOnCentroid(t);
		model.setTransform(t,"mirror X",isNewOp());
	}

	public void mirrorY() {
		Transform3D t = new Transform3D();
		Vector3d v = new Vector3d(1d,-1d,1d);
		t.setScale(v);
		t = transformOnCentroid(t);
		model.setTransform(t,"mirror Y",isNewOp());
	}

	public void mirrorZ() {
		Transform3D t = new Transform3D();
		Vector3d v = new Vector3d(1d,1d,-1d);
		t.setScale(v);
		t = transformOnCentroid(t);
		model.setTransform(t,"mirror Z",isNewOp());
	}
		
	public boolean isOnPlatform() {
		BoundingBox bb = getBoundingBox();
		Point3d lower = new Point3d();
		bb.getLower(lower);
		return lower.z < 0.001d && lower.z > -0.001d;
	}

	public void scale(double scale, boolean isOnPlatform) {
		Transform3D t = new Transform3D();
		t.setScale(scale);
		if (isOnPlatform) {
			t = transformOnBottom(t);
		} else {
			t = transformOnCentroid(t);			
		}
		shapeTransform.setTransform(t);
		model.setTransform(t,"resize",isNewOp());		
	}
	

	private BoundingBox getBoundingBox(Group group, Transform3D transformation) {
		BoundingBox bb = new BoundingBox(new Point3d(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE),
				new Point3d(Double.MIN_VALUE,Double.MIN_VALUE,Double.MIN_VALUE));
		transformation = new Transform3D(transformation);
		if (group instanceof TransformGroup) {
			Transform3D nextTransform = new Transform3D();
			((TransformGroup)group).getTransform(nextTransform);
			transformation.mul(nextTransform);
		}
		for (int i = 0; i < group.numChildren(); i++) {
			Node n = group.getChild(i);
			if (n instanceof Shape3D) {
				bb.combine(getBoundingBox((Shape3D)n, transformation));
			} else if (n instanceof Group) {
				bb.combine(getBoundingBox((Group)n,transformation));
			}
		}
		return bb;
	}

	public BoundingBox getBoundingBox() {
		return getBoundingBox(shapeTransform);
	}
	
	
	private Point3d centroid = null;
	private Point3d bottom = null;
	
	private void invalidateBounds() {
		centroid = null;
		bottom = null;
	}
	
	private void validateBounds() {
		if (centroid == null) {
			BoundingBox bb = getBoundingBox();
			Point3d p1 = new Point3d();
			Point3d p2 = new Point3d();
			bb.getLower(p1);
			bb.getUpper(p2);
			p2.interpolate(p1,0.5d);
			centroid = p2;
			bottom = new Point3d(centroid.x, centroid.y, p1.z);
		}
	}
	
	public Point3d getCentroid() {
		validateBounds();
		return centroid;
	}
	
	public Point3d getBottom() {
		validateBounds();
		return bottom;
	}
	
	/**
	 * Center the object tree and raise its lowest point to Z=0.
	 */
	public void center() {
		BoundingBox bb = getBoundingBox(shapeTransform);
		Point3d lower = new Point3d();
		Point3d upper = new Point3d();
		bb.getLower(lower);
		bb.getUpper(upper);
		double zoff = -lower.z;
		double xoff = -(upper.x + lower.x)/2.0d;
		double yoff = -(upper.y + lower.y)/2.0d;
		translateObject(xoff, yoff, zoff);
	}

	/**
	 * Raise the object's lowest point to Z=0.
	 */
	public void putOnPlatform() {
		BoundingBox bb = getBoundingBox(shapeTransform);
		Point3d lower = new Point3d();
		bb.getLower(lower);
		double zoff = -lower.z;
		translateObject(0d, 0d, zoff);
	}

	/**
	 * Lay the object flat with the Z object.  It computes this by finding the bottommost
	 * point, and then rotating the object to make the surface with the lowest angle to
	 * the Z plane parallel to it.
	 * 
	 * In the future, we will want to add a convex hull pass to this.
	 * 
	 */
	public void layFlat() {
		// Compute transformation
		Transform3D t = new Transform3D();
		shapeTransform.getTransform(t);
		Enumeration<?> geometries = originalShape.getAllGeometries();
		while (geometries.hasMoreElements()) {
			Geometry g = (Geometry)geometries.nextElement();
			double lowest = Double.MAX_VALUE;
			Vector3d flattest = new Vector3d(1d,0d,0d);
			if (g instanceof GeometryArray) {
				GeometryArray ga = (GeometryArray)g;
				Point3d p1 = new Point3d();
				Point3d p2 = new Point3d();
				Point3d p3 = new Point3d();
				for (int i = 0; i < ga.getVertexCount();) {
					ga.getCoordinate(i++,p1);
					ga.getCoordinate(i++,p2);
					ga.getCoordinate(i++,p3);
					t.transform(p1);
					t.transform(p2);
					t.transform(p3);
					double triLowest = Math.min(p1.z, Math.min(p2.z, p3.z));
					if (triLowest < lowest) {
						// Clear any prior triangles
						flattest = new Vector3d(1d,0d,0d);
						lowest = triLowest;
					}
					if (triLowest == lowest) {
						// This triangle is a candidate!
						Vector3d v1 = new Vector3d(p2);
						v1.sub(p1);
						Vector3d v2 = new Vector3d(p3);
						v2.sub(p2);
						Vector3d v = new Vector3d();
						v.cross(v1,v2);
						v.normalize();
						if (v.z < flattest.z) { flattest = v; }
					}
				}
			}
			Transform3D flattenTransform = new Transform3D();
			Vector3d downZ = new Vector3d(0d,0d,-1d);
			double angle = Math.acos(flattest.dot(downZ));
			Vector3d cross = new Vector3d();
			cross.cross(flattest, downZ);
			flattenTransform.setRotation(new AxisAngle4d(cross,angle));
			flattenTransform = transformOnCentroid(flattenTransform);
			shapeTransform.setTransform(flattenTransform);
			model.setTransform(flattenTransform,"Lay flat", isNewOp());
			invalidateBounds(); 
		}
	}
	
	boolean inDrag = false;
	boolean firstDrag = false;
	
	private boolean isNewOp() {
		if (!inDrag) { return true; }
		if (firstDrag) {
			firstDrag = false;
			return true;
		}
		return false;
	}
	
	public void startDrag() {
		inDrag = true;
		firstDrag = true;
	}
	
	public void endDrag() {
		inDrag = false;
	}
}
