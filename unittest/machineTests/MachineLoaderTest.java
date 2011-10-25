package machineTests;

import static org.junit.Assert.*;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import replicatorg.app.Base;
import replicatorg.machine.MachineListener;
import replicatorg.machine.MachineLoader;
import replicatorg.machine.MachineProgressEvent;
import replicatorg.machine.MachineState;
import replicatorg.machine.MachineStateChangeEvent;
import replicatorg.machine.MachineToolStatusEvent;

public class MachineLoaderTest {
	// Simple class to collect Machine events so they can be read back later.
	public class TestMachineListener implements MachineListener {
		// We have to receive these kinds of messages.
		// TODO: Make this abstract.
		LinkedBlockingQueue<MachineStateChangeEvent> machineStateChangeEventQueue;
//		LinkedBlockingQueue<MachineProgressEvent> machineProgressEventQueue;
//		LinkedBlockingQueue<MachineToolStatusEvent> machineToolStatusEventQueue;

		TestMachineListener() {
			machineStateChangeEventQueue = new LinkedBlockingQueue<MachineStateChangeEvent>();
		}
		
		@Override
		public void machineStateChanged(MachineStateChangeEvent evt) {
			machineStateChangeEventQueue.add(evt);
		}

		@Override
		public void machineProgress(MachineProgressEvent event) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void toolStatusChanged(MachineToolStatusEvent event) {
			// TODO Auto-generated method stub
			
		}
		
		synchronized MachineStateChangeEvent getMachineStateChangedEvent(int timeout) {
			try {
				return machineStateChangeEventQueue.poll(timeout, TimeUnit.MILLISECONDS) ;
			} catch (InterruptedException e) {
			}
			return null;
		}
	}
	
	MachineLoader loader;
	TestMachineListener listener;
	
	/** Amount of time we should wait for a serial message to complete **/
	final int commTimeout = 5000;
	
	@Before
	public void setUp() throws Exception {
		loader = new MachineLoader();
		listener = new TestMachineListener();
		
		loader.addMachineListener(listener);
	}

	@After
	public void tearDown() throws Exception {
		loader.disconnect();
		loader = null;
	}

	@Test
	public void testGetMachine() {
		// Without initializing the machine loader, this should return null.
		assertNull(loader.getMachine());
	}

	@Test
	public void testIsLoaded() {
		// Without initializing the machine loader, this should return false.
		assertFalse(loader.isLoaded());
	}

	@Test
	public void testIsConnected() {
		// Without initializing the machine loader, this should return false.
		assertFalse(loader.isConnected());
	}

	@Test
	public void testGetDriver() {
		// Without initializing the machine loader, this should return null.
		assertNull(loader.getDriver());
	}

	@Test
	public void testLoad() {
		// Try loading a garbage machine
		assertFalse(loader.load("fake machine"));
		
		// Now try loading a known good machine
		assertTrue(loader.load("Cupcake Basic"));
		
		// Test that the isLoaded() function actually works 
		assertTrue(loader.isLoaded());
		
		// Now, try loading another garbage machine on top of the known good one.
		assertFalse(loader.load("fake machine"));
		
		// Finally, load the good one again to make sure it can recover.
		assertTrue(loader.load("Cupcake Basic"));
	}

	@Test
	public void testUnload() {
		// See if we can bring up and then dispose of a machine
		assertTrue(loader.load("Cupcake Basic"));
		
		loader.unload();
		
		assertFalse(loader.isLoaded());
	}
	
	@Test
	public void testConnectionInvalidPort() {
		MachineStateChangeEvent event;
		
		// Try connecting to a machine with a garbage port. 
		// This should give us a couple of messages: CONNECTING ERROR
		// Make sure that we get them both back in order, within a reasonable time.
		// Finally, unload the machine to receive NOT_ATTACHED.
		assertTrue(loader.load("Cupcake Basic"));
		loader.connect("");
		
		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		Base.logger.info(event.getState().getState().toString());
		assertTrue(event.getState().getState() == MachineState.State.CONNECTING);

		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		assertTrue(event.getState().getState() == MachineState.State.NOT_ATTACHED);
		
		// Finally, make sure that the query interface agrees we aren't connected
		assertFalse(loader.isConnected());
		
		// Unload the machine, 
		loader.unload();
		
		// Make sure that it actually unloaded
		assertFalse(loader.isLoaded());
		
		// And make sure we don't get any more errors from it.
		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNull(event);
	}

	// TODO: This requires a Thing-O-Matic on a specific serial port.
	// We don't have any mocks :-(
	@Test
	public void testConnectionKnownGoodPort() {
		MachineStateChangeEvent event;
		
		// Try connecting to a machine with a known good port. 
		// This should give us a couple of messages: CONNECTING READY
		// Make sure that we get them both back in order, within a reasonable time.
		// Finally, unload the machine to receive NOT_ATTACHED.
		for(int i = 0; i < 1; i++) {
			Base.logger.info("i = " + i );
			assertTrue(loader.load("Thingomatic w/ HBP and Stepstruder MK6"));
			loader.connect("/dev/ttyUSB0");
			
			event = listener.getMachineStateChangedEvent(commTimeout);
			assertNotNull(event);
			Base.logger.info(event.getState().getState().toString());
			assertTrue(event.getState().getState() == MachineState.State.CONNECTING);
	
			event = listener.getMachineStateChangedEvent(commTimeout);
			assertNotNull(event);
			assertTrue(event.getState().getState() == MachineState.State.READY);
			
			// Finally, make sure that the query interface agrees we aren't connected
			assertTrue(loader.isConnected());
			
			// Unload the machine, 
			loader.unload();
			event = listener.getMachineStateChangedEvent(commTimeout);
			assertNotNull(event);
			assertTrue(event.getState().getState() == MachineState.State.NOT_ATTACHED);
		}
	}
	
	
	// TODO: This requires a Thing-O-Matic on a specific serial port.
	// We don't have any mocks :-(
	@Test
	public void testDisconnect() {
		MachineStateChangeEvent event;
		
		assertTrue(loader.load("Thingomatic w/ HBP and Stepstruder MK6"));
		loader.connect("/dev/ttyUSB0");
		
		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		Base.logger.info(event.getState().getState().toString());
		assertTrue(event.getState().getState() == MachineState.State.CONNECTING);

		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		assertTrue(event.getState().getState() == MachineState.State.READY);
		
		// Finally, make sure that the query interface agrees we aren't connected
		assertTrue(loader.isConnected());
		
		// Now, disconnect the machine.
		loader.disconnect();
		
		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		assertTrue(event.getState().getState() == MachineState.State.NOT_ATTACHED);
		
		assertFalse(loader.isConnected());
		
		// Unload the machine. Since we're not attached, there shouldn't be another message. 
		loader.unload();
		event = listener.getMachineStateChangedEvent(500);
		assertNull(event);
	}
	
	
	// TODO: This requires a Thing-O-Matic on a specific serial port.
	// We don't have any mocks :-(
	@Test
	public void testUserDisconnect() {
		MachineStateChangeEvent event;
		
		assertTrue(loader.load("Thingomatic w/ HBP and Stepstruder MK6"));
		loader.connect("/dev/ttyUSB0");
		
		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		Base.logger.info(event.getState().getState().toString());
		assertTrue(event.getState().getState() == MachineState.State.CONNECTING);

		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		assertTrue(event.getState().getState() == MachineState.State.READY);
		
		// Finally, make sure that the query interface agrees we aren't connected
		assertTrue(loader.isConnected());
		
		Base.logger.severe("Unplug the USB port!");
		
		// Now, wait for the user to disconnect the machine
		event = listener.getMachineStateChangedEvent(commTimeout);
		assertNotNull(event);
		assertTrue(event.getState().getState() == MachineState.State.NOT_ATTACHED);
		
		assertFalse(loader.isConnected());
		
		// Unload the machine. Since we're not attached, there shouldn't be another message. 
		loader.unload();
		
		event = listener.getMachineStateChangedEvent(500);
		assertNull(event);
	}

}
