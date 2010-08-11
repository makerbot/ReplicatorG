package testing.fabman;

import java.io.File;
import java.util.Collection;
import java.util.Vector;

import javax.media.j3d.BoundingBox;
import javax.vecmath.Point3d;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fabman.manager.FabDescriptor;
import fabman.manager.FabManager;

public class FabDescriptorTest {
	FabManager mgr;
		
	@BeforeClass
	public void setUp() {
		Vector<File> paths = new Vector<File>();
		String descriptorPath = System.getenv("TEST_DESCRIPTORS");
		assert descriptorPath != null : "TEST_DESCRIPTORS environment variable is unset; needed for test";
		paths.add(new File(descriptorPath));
		mgr = FabManager.getFabManager(paths);
		assert mgr != null : "Could not create a custom fab manager";
	}
	
	@Test
	public void checkDescriptors() {
		// Check that the number of descriptors is valid for this test.
		Collection<FabDescriptor> descriptors = mgr.getAvailableDescriptors();
		assert descriptors.size() == 5 : "Incorrect number of descriptors loaded";
	}
	
	private FabDescriptor getDescriptor(String name) {
		Collection<FabDescriptor> descriptors = mgr.getAvailableDescriptors();
		for (FabDescriptor test : descriptors) {
			if (test.getName().equals(name)) {
				return test;
			}
		}
		return null;
	}
	
	static final String TST_NAME = "Testing Machine";
	
	@Test(dependsOnMethods = {"checkDescriptors"})
	public void getTestDescriptor() {
		FabDescriptor testMachine = getDescriptor(TST_NAME);
		assert testMachine != null;
	}
	
	@Test(dependsOnMethods = {"getTestDescriptor"})
	public void checkBoundsLoading() {
		FabDescriptor descriptor = getDescriptor(TST_NAME);
		BoundingBox envelope = descriptor.getBuildEnvelope(); 
		assert envelope != null;
		BoundingBox expectedBounds = new BoundingBox(new Point3d(-50,-50,0), new Point3d(50,50,120));
		assert envelope.equals(expectedBounds);
	}
	
	@Test
	public void openTestConnection() {
	}
}
