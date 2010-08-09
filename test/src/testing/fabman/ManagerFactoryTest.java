package testing.fabman;

import java.io.File;
import java.util.Vector;

import org.testng.annotations.Test;

import fabman.manager.FabManager;

public class ManagerFactoryTest {
  @Test
  public void createDefaultManager() {
	  FabManager mgr = FabManager.getFabManager();
	  assert mgr != null : "Could not create a default fab manager";
  }
  
  @Test
  public void createTestManager() {
	  Vector<File> paths = new Vector<File>();
	  String descriptorPath = System.getenv("TEST_DESCRIPTORS");
	  assert descriptorPath != null : "TEST_DESCRIPTORS environment variable is unset; needed for test";
	  paths.add(new File(descriptorPath));
	  FabManager mgr = FabManager.getFabManager(paths);
	  assert mgr != null : "Could not create a custom fab manager";
  }
}
