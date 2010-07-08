/**
 * 
 */
package replicatorg.app.ui.modeling;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.util.logging.Level;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import net.miginfocom.swing.MigLayout;
import replicatorg.app.Base;
import replicatorg.app.ui.MainWindow;
import replicatorg.model.BuildModel;

import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * @author phooky
 *
 */
public class PreviewPanel extends JPanel {

	BoundingSphere bounds =
		new BoundingSphere(new Point3d(0.0,0.0,0.0), 1000.0);

	EditingModel model = null;
	
	EditingModel getModel() { return model; }
	
	public void setModel(BuildModel buildModel) {
		if (model == null || buildModel != model.getBuildModel()) {
			if (buildModel != null) {
				model = new EditingModel(buildModel);
				setScene(model);
			} else {
				model = null;
			}
		}
	}

	private void setScene(EditingModel model) {
		Base.logger.info(model.model.getPath());
		if (objectBranch != null) {
			sceneGroup.removeChild(objectBranch);
		}
		objectBranch = model.getGroup();
		sceneGroup.addChild(objectBranch);
	}
	
	MainWindow mainWindow;

	enum DragMode {
		NONE,
		ROTATE_VIEW,
		TRANSLATE_VIEW,
		ROTATE_OBJECT,
		TRANSLATE_OBJECT
	};

	ToolPanel toolPanel;
	
	Tool currentTool = null; // The tool currently in use.
	
	void setTool(Tool tool) {
		if (currentTool == tool) { return; }
		if (currentTool != null) {
			if (currentTool instanceof MouseListener) {
				canvas.removeMouseListener((MouseListener)currentTool);
			}
			if (currentTool instanceof MouseMotionListener) {
				canvas.removeMouseMotionListener((MouseMotionListener)currentTool);
			}
			if (currentTool instanceof MouseWheelListener) {
				canvas.removeMouseWheelListener((MouseWheelListener)currentTool);
			}
			if (currentTool instanceof KeyListener) {
				canvas.removeKeyListener((KeyListener)currentTool);
			}
		}
		currentTool = tool;
		if (currentTool != null) {
			if (currentTool instanceof MouseListener) {
				canvas.addMouseListener((MouseListener)currentTool);
			}
			if (currentTool instanceof MouseMotionListener) {
				canvas.addMouseMotionListener((MouseMotionListener)currentTool);
			}
			if (currentTool instanceof MouseWheelListener) {
				canvas.addMouseWheelListener((MouseWheelListener)currentTool);
			}
			if (currentTool instanceof KeyListener) {
				canvas.addKeyListener((KeyListener)currentTool);
			}
		}
	}
		
	Canvas3D canvas;
	
	void adjustViewAngle(double deltaYaw, double deltaPitch) {
		turntableAngle += deltaYaw;
		elevationAngle += deltaPitch;
		updateVP();
	}
	
	void adjustViewTranslation(double deltaX, double deltaY) {
		cameraTranslation.x += deltaX;
		cameraTranslation.y += deltaY;
		updateVP();
	}
	
	void adjustZoom(double deltaZoom) {
		cameraTranslation.z += deltaZoom;
		updateVP();
	}
	
	public PreviewPanel(final MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		//setLayout(new MigLayout()); 
		setLayout(new MigLayout("fill,ins 0,gap 0"));
		// Create Canvas3D and SimpleUniverse; add canvas to drawing panel
		canvas = createUniverse();
		add(canvas, "growx,growy");
		toolPanel = new ToolPanel(this);
		add(toolPanel,"dock east");
		// Create the content branch and add it to the universe
		BranchGroup scene = createSTLScene();
		univ.addBranchGraph(scene);
		
		canvas.addKeyListener( new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyChar() == 'a') {
					cameraTranslation.x += 0.05;
				} else if (e.getKeyChar() == 'z') {
					cameraTranslation.x -= 0.05;
				} else if (e.getKeyChar() == 's') {
					cameraTranslation.y += 0.05;
				} else if (e.getKeyChar() == 'x') {
					cameraTranslation.y -= 0.05;
				} else if (e.getKeyChar() == 'd') {
					cameraTranslation.z += 0.05;
				} else if (e.getKeyChar() == 'c') {
					cameraTranslation.z -= 0.05;
				} else if (e.getKeyChar() == '[') {
					elevationAngle += 0.05;
				} else if (e.getKeyChar() == ']') {
					elevationAngle -= 0.05;
				} else if (e.getKeyChar() == '{') {
					turntableAngle += 0.05;
				} else if (e.getKeyChar() == '}') {
					turntableAngle -= 0.05;
				} else if (e.getKeyChar() == 'e') {
					showEdges = !showEdges;
					model.showEdges(showEdges);
				} else {
					return;
				}
				updateVP();
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
			}
		});

	}		


	private SimpleUniverse univ = null;

	/**
	 * Indicates whether we're in edge (wireframe) mode.  False indicates a solid view. 
	 */
	private boolean showEdges = false;

	public Node makeAmbientLight() {
		AmbientLight ambient = new AmbientLight();
		ambient.setColor(new Color3f(0.3f,0.3f,0.9f));
		ambient.setInfluencingBounds(bounds);
		return ambient;
	}

	public Node makeDirectedLight1() {
		Color3f color = new Color3f(0.7f,0.7f,0.7f);
		Vector3f direction = new Vector3f(1f,0.7f,-0.2f);
		DirectionalLight light = new DirectionalLight(color,direction);
		light.setInfluencingBounds(bounds);
		return light;
	}

	public Node makeDirectedLight2() {
		Color3f color = new Color3f(0.5f,0.5f,0.5f);
		Vector3f direction = new Vector3f(-1f,-0.7f,0.2f);
		DirectionalLight light = new DirectionalLight(color,direction);
		light.setInfluencingBounds(bounds);
		return light;
	}

	final double wireBoxCoordinates[] = {
			0,  0,  0,    0,  0,  1,
			0,  1,  0,    0,  1,  1,
			1,  1,  0,    1,  1,  1,
			1,  0,  0,    1,  0,  1,

			0,  0,  0,    0,  1,  0,
			0,  0,  1,    0,  1,  1,
			1,  0,  1,    1,  1,  1,
			1,  0,  0,    1,  1,  0,

			0,  0,  0,    1,  0,  0,
			0,  0,  1,    1,  0,  1,
			0,  1,  1,    1,  1,  1,
			0,  1,  0,    1,  1,  0,
	};

	public Shape3D makeBoxFrame(Point3d ll, Vector3d dim) {
		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_DOT,true));
		edges.setPolygonAttributes(new PolygonAttributes(PolygonAttributes.POLYGON_LINE,
				PolygonAttributes.CULL_NONE,0));
		double[] coords = new double[wireBoxCoordinates.length];
		for (int i = 0; i < wireBoxCoordinates.length;) {
			coords[i] = (wireBoxCoordinates[i] * dim.x) + ll.x; i++;
			coords[i] = (wireBoxCoordinates[i] * dim.y) + ll.y; i++;
			coords[i] = (wireBoxCoordinates[i] * dim.z) + ll.z; i++;
		}
		LineArray wires = new LineArray(wireBoxCoordinates.length/3,GeometryArray.COORDINATES);
		wires.setCoordinates(0, coords);

		return new Shape3D(wires,edges); 
	}

	public Node makeBoundingBox() {

		Shape3D boxframe = makeBoxFrame(new Point3d(-50,-50,0), new Vector3d(100,100,100));	

		/*
		Appearance sides = new Appearance();
		sides.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.NICEST,0.9f));
		Color3f color = new Color3f(0.05f,0.05f,1.0f); 
		Material m = new Material(color,color,color,color,64.0f);
		sides.setMaterial(m);

		Box box = new Box(50,50,50,sides);
		Transform3D tf = new Transform3D();
		tf.setTranslation(new Vector3d(0,0,50));
		TransformGroup tg = new TransformGroup(tf);
		tg.addChild(box);
		tg.addChild(boxframe);
		*/
		return boxframe;
	}

	public Node makeBackground() {
		Background bg = new Background(0.5f,0.5f,0.6f);
		bg.setApplicationBounds(bounds);
		return bg;
	}

	public Node makeBaseGrid() {
		Appearance edges = new Appearance();
		edges.setLineAttributes(new LineAttributes(1,LineAttributes.PATTERN_DOT,true));
		edges.setColoringAttributes(new ColoringAttributes(0.7f,0.7f,1f,ColoringAttributes.FASTEST));
		final int LINES = 11;
		LineArray grid = new LineArray(4*LINES,GeometryArray.COORDINATES);
		for (int i = 0; i < LINES; i++) {
			double offset = -50 + (100/(LINES-1))*i;
			int idx = i*4;
			// Along x axis
			grid.setCoordinate(idx++, new Point3d(offset,-50,0));
			grid.setCoordinate(idx++, new Point3d(offset,50,0));
			// Along y axis
			grid.setCoordinate(idx++, new Point3d(-50,offset,0));
			grid.setCoordinate(idx++, new Point3d(50,offset,0));
		}
		return new Shape3D(grid,edges); 
	}
	
	BranchGroup sceneGroup;
	BranchGroup objectBranch;
			
	/**
	 * Center the object and flatten the bottommost poly.  (A more thorough version would
	 * be able to correctly center a tripod or other spiky object.)
	 */
	public void align() {
		model.center();
	}
	
	
	public BranchGroup createSTLScene() {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();

		sceneGroup = new BranchGroup();
		sceneGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		sceneGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		sceneGroup.addChild(makeAmbientLight());
		sceneGroup.addChild(makeDirectedLight1());
		sceneGroup.addChild(makeDirectedLight2());
		sceneGroup.addChild(makeBoundingBox());
		sceneGroup.addChild(makeBackground());
		sceneGroup.addChild(makeBaseGrid());

		objRoot.addChild(sceneGroup);

		// Create a new Behavior object that will perform the
		// desired operation on the specified transform and add
		// it into the scene graph.
		//	Transform3D yAxis = new Transform3D();
		//	Alpha rotationAlpha = new Alpha(-1, 4000);

		//	RotationInterpolator rotator =
		//	    new RotationInterpolator(rotationAlpha, objTrans, yAxis,
		//				     0.0f, (float) Math.PI*2.0f);
		//	BoundingSphere bounds =
		//	    new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0);
		//	rotator.setSchedulingBounds(bounds);
		objRoot.compile();

		return objRoot;
	}

	// These values were determined experimentally to look pretty dang good.
	final static Vector3d CAMERA_TRANSLATION_DEFAULT = new Vector3d(0,40,290);
	final static double ELEVATION_ANGLE_DEFAULT = 1.278;
	final static double TURNTABLE_ANGLE_DEFAULT = 0.214;
	
	final static double CAMERA_DISTANCE_DEFAULT = 300d; // 30cm
	
	Vector3d cameraTranslation = new Vector3d(CAMERA_TRANSLATION_DEFAULT);
	double elevationAngle = ELEVATION_ANGLE_DEFAULT;
	double turntableAngle = TURNTABLE_ANGLE_DEFAULT;

	Transform3D getViewTransform() {
		TransformGroup viewTG = univ.getViewingPlatform().getViewPlatformTransform();
		Transform3D t = new Transform3D();
		viewTG.getTransform(t);
		return t;
	}
	
//	double VIEW_SCALE = 100d;
	private void updateVP() {
		TransformGroup viewTG = univ.getViewingPlatform().getViewPlatformTransform();
		Transform3D t3d = new Transform3D();
		Transform3D trans = new Transform3D();
		Transform3D rotZ = new Transform3D();
		Transform3D rotX = new Transform3D();
		trans.setTranslation(cameraTranslation);
		rotX.rotX(elevationAngle);
		rotZ.rotZ(turntableAngle);
		t3d.mul(rotZ);
		t3d.mul(rotX);
		t3d.mul(trans);
//		Transform3D scale = new Transform3D();
//		scale.setScale(VIEW_SCALE);
//		t3d.mul(scale);
		viewTG.setTransform(t3d);

		if (Base.logger.isLoggable(Level.FINE)) {
			Base.logger.fine("Camera Translation: "+cameraTranslation.toString());
			Base.logger.fine("Elevation "+Double.toString(elevationAngle)+", turntable "+Double.toString(turntableAngle));
		}
	}

	private Canvas3D createUniverse() {
		// Get the preferred graphics configuration for the default screen
		GraphicsConfiguration config =
			SimpleUniverse.getPreferredConfiguration();

		// Create a Canvas3D using the preferred configuration
		Canvas3D c = new Canvas3D(config) {
			public Dimension getMinimumSize()
		    {
		        return new Dimension(0, 0);
		    }
		};

		// Create simple universe with view branch
		univ = new SimpleUniverse(c);
		univ.getViewer().getView().setSceneAntialiasingEnable(true);
		univ.getViewer().getView().setFrontClipDistance(10d);
		univ.getViewer().getView().setBackClipDistance(1000d);
		updateVP();

		// Ensure at least 5 msec per frame (i.e., < 200Hz)
		univ.getViewer().getView().setMinimumFrameCycleTime(5);

		return c;
	}

	void resetView() {
		cameraTranslation = new Vector3d(CAMERA_TRANSLATION_DEFAULT);
		elevationAngle = ELEVATION_ANGLE_DEFAULT;
		turntableAngle = TURNTABLE_ANGLE_DEFAULT;
//		usePerspective(true);
		updateVP();
	}

	public void viewXY() {
		cameraTranslation = new Vector3d(0d,0d,CAMERA_DISTANCE_DEFAULT);
		turntableAngle = 0d;
		elevationAngle = 0d;
//		usePerspective(false);
		updateVP();	
	}
	
	public void viewYZ() {
		cameraTranslation = new Vector3d(0d,50d,CAMERA_DISTANCE_DEFAULT);
		turntableAngle = 0d;
		elevationAngle = Math.PI/2;
//		usePerspective(false);
		updateVP();	
	}
	public void viewXZ() {
		cameraTranslation = new Vector3d(0d,50d,CAMERA_DISTANCE_DEFAULT);
		elevationAngle = Math.PI/2;
		turntableAngle = Math.PI/2;
//		usePerspective(false);
		updateVP();	
	}

	public void usePerspective(boolean perspective) {
		univ.getViewer().getView().setProjectionPolicy(perspective?View.PERSPECTIVE_PROJECTION:View.PARALLEL_PROJECTION);
	}


}
