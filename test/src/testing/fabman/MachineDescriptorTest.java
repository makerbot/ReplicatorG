package testing.fabman;

import java.io.File;
import java.util.Collection;
import java.util.Vector;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fabman.manager.FabDescriptor;
import fabman.manager.FabManager;

public class MachineDescriptorTest {
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
		assert descriptors.size() == 1 : "Incorrect number of descriptors loaded";
	}
}
