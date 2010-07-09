package replicatorg.app.ui.modeling;

import java.util.Enumeration;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

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
	 * The switch object that allows us to toggle between wireframe and solid modes.
	 */
	private Switch objectSwitch = null;

	/** The group which represents the displayable subtree.
	 */
	private BranchGroup group = null;

	/**
	 * The transform group for the shape.  The enclosed transform should be applied to the shape before:
	 * * bounding box calculation
	 * * saving out the STL for skeining
	 */
	private TransformGroup shapeTransform = new TransformGroup();
	
	public EditingModel(BuildModel model) {
		this.model = model;
	}
	
	/**
	 * Create the branchgroup that will display the object.
	 */
	private BranchGroup makeShape(BuildModel model) {
		objectSwitch = new Switch();
		Shape3D originalShape = model.getShape();

		Shape3D shape = (Shape3D)originalShape.cloneTree();
		Shape3D edgeClone = (Shape3D)originalShape.cloneTree();
		objectSwitch.addChild(shape);
		objectSwitch.addChild(edgeClone);
		objectSwitch.setWhichChild(0);
		objectSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);

		Color3f color = new Color3f(1.0f,1.0f,1.0f); 
		Material m = new Material();
		m.setAmbientColor(color);
		m.setDiffuseColor(color);
		Appearance solid = new Appearance();
		solid.setMaterial(m);
		shape.setAppearance(solid);

		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_SOLID,true));
		edges.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				PolygonAttributes.CULL_NONE,0));
		edgeClone.setAppearance(edges);

		BranchGroup wrapper = new BranchGroup();

		shapeTransform = new TransformGroup();
		shapeTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		wrapper.addChild(shapeTransform);

		shapeTransform.addChild(objectSwitch);
		wrapper.setCapability(BranchGroup.ALLOW_DETACH);
		wrapper.compile();
		return wrapper;
	}

	BuildModel getBuildModel() { return model; }
	
	public BranchGroup getGroup() {
		if (group == null) {
			group = makeShape(model); 
		}
		return group;
	}
	
	public void showEdges(boolean showEdges) {
		if (showEdges) {
			objectSwitch.setWhichChild(1);
		} else {
			objectSwitch.setWhichChild(1);
		}
	}
	
	public ReferenceFrame getReferenceFrame() {
		Transform3D translate = new Transform3D();
		shapeTransform.getTransform(translate);
		ReferenceFrame rf = new ReferenceFrame();
		translate.transform(rf.origin);
		translate.transform(rf.zAxis);
		return rf;
	}

	public void rotateObject(double turntable, double elevation) {
		// Skip identity translations
		if (turntable == 0.0 && elevation == 0.0) { return; }
		Transform3D t1 = new Transform3D();
		Transform3D r1 = new Transform3D();
		Transform3D r2 = new Transform3D();
		Transform3D t2 = new Transform3D();
		Vector3d t1v = new Vector3d(getCentroid());
		t1v.negate();
		Vector3d t2v = new Vector3d(getCentroid());
		t1.setTranslation(t1v);
		t2.setTranslation(t2v);
		r1.rotX(elevation);
		r2.rotZ(turntable);
		Transform3D old = new Transform3D();
		shapeTransform.getTransform(old);
		
		Transform3D composite = new Transform3D();
		composite.mul(t2);
		composite.mul(r2);
		composite.mul(r1);
		composite.mul(t1);
		composite.mul(old);
		shapeTransform.setTransform(composite);
		model.setTransform(composite,"rotation");
	}
	
	public void translateObject(double x, double y, double z) {
		// Skip identity translations
		if (x == 0.0 && y == 0.0 && z == 0.0) { return; }
		centroid = null;
		Transform3D translate = new Transform3D();
		translate.setZero();
		translate.setTranslation(new Vector3d(x,y,z));
		Transform3D old = new Transform3D();
		shapeTransform.getTransform(old);
		old.add(translate);
		shapeTransform.setTransform(old);
		model.setTransform(old,"move");
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
		Transform3D old = new Transform3D();
		shapeTransform.getTransform(old);
		flipZ.rotY(Math.PI);
		flipZ.mul(old);
		shapeTransform.setTransform(flipZ);
		model.setTransform(flipZ,"flip");
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
	
	public Point3d getCentroid() {
		if (centroid == null) {
			BoundingBox bb = getBoundingBox();
			Point3d p1 = new Point3d();
			Point3d p2 = new Point3d();
			bb.getLower(p1);
			bb.getUpper(p2);
			p1.interpolate(p2,0.5d);
			centroid = p1;
			System.err.println("Centroid is "+centroid.toString());
		}
		return centroid;
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

}
