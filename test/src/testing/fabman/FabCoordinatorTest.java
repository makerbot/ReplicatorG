package testing.fabman;

import java.io.IOException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fabman.coordinator.FabCoordinator;

public class FabCoordinatorTest {

	FabCoordinator coordinator = null;
	static final int TESTING_PORT = 5115;
	
	@BeforeClass
	public void createCoordinator() throws IOException {
		assert coordinator == null;
		try {
			coordinator = new FabCoordinator(TESTING_PORT);
		} catch (java.net.BindException be) {
			assert false : "Problem binding test port; ensure that no other test is running";
		}
	}
	
	@AfterClass
	public void shutdownCoordinator() throws IOException {
		assert coordinator != null;
		coordinator.shutdown();
	}
	
	@Test
	public void createDuplicate() throws IOException {
		try {
			new FabCoordinator(TESTING_PORT);
		} catch (java.net.BindException be) {
			// Success; the already-running server should have the port.
			return;
		}
		assert false : "Multiple coordinators were permitted to start";
	}
}
