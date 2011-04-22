package machineTests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import replicatorg.machine.MachineLoader;

public class MachineLoaderTest {
	MachineLoader machineLoader;
	
	@Before
	public void setUp() throws Exception {
		machineLoader = new MachineLoader();
	}

	@After
	public void tearDown() throws Exception {
		machineLoader.disconnect();
		machineLoader = null;
	}

	@Test
	public void testGetMachine() {
		// Without initializing the machine loader, this should return null.
		assertNull(machineLoader.getMachine());
	}

	@Test
	public void testIsLoaded() {
		// Without initializing the machine loader, this should return false.
		assertFalse(machineLoader.isLoaded());
	}

	@Test
	public void testIsConnected() {
		// Without initializing the machine loader, this should return false.
		assertFalse(machineLoader.isConnected());
	}

	@Test
	public void testGetDriver() {
		// Without initializing the machine loader, this should return null.
		assertNull(machineLoader.getDriver());
	}

	@Test
	public void testLoad() {
		// Try loading a garbage machine
		assertFalse(machineLoader.load("fake machine"));
		
		// Now try loading a known good machine
		assertTrue(machineLoader.load("3-Axis Simulator"));
		
		// Test that the isLoaded() function actually works 
		assertTrue(machineLoader.isLoaded());
		
		// Now, try loading another garbage machine on top of the known good one.
		assertFalse(machineLoader.load("fake machine"));
		
		// Finally, load the good one again to make sure it can recover.
		assertTrue(machineLoader.load("3-Axis Simulator"));
	}

	@Test
	public void testUnload() {
		// See if we can bring up and then dispose of a machine
		assertTrue(machineLoader.load("3-Axis Simulator"));
		
		machineLoader.unload();
		
		assertFalse(machineLoader.isLoaded());
	}

	@Test
	public void testConnect() {
		// First, load a machine that doesn't implement connect.
		assertTrue(machineLoader.load("3-Axis Simulator"));
		machineLoader.connect("");
		assertFalse(machineLoader.isConnected());
		
		// Now, try a machine that could, but with a garbage port.
		// Grr, guess we can't if we don't have the XML 
		assertTrue(machineLoader.load("sanguino3g"));
		machineLoader.connect("");
		assertTrue(machineLoader.isConnected());
	}

	@Test
	public void testDisconnect() {
		fail("Not yet implemented");
	}

}
