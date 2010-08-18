package testing.fabman;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fabman.coordinator.FabCoordinator;
import fabman.messages.Coordinator;
import fabman.messages.Coordinator.Type;
import fabman.messages.Coordinator.GetFabResponse.RspCode;

public class FabCoordinatorTest {

	FabCoordinator coordinator = null;
	static final int TESTING_PORT = 5115;
	
	@BeforeClass
	public void createCoordinator() throws IOException {
		assert coordinator == null;
		try {
			coordinator = new FabCoordinator(TESTING_PORT);
			new Thread(coordinator).start();
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
	
	// Get a socket to the fab coordinator
	Socket getConnection() throws IOException {
		Socket socket = new Socket(InetAddress.getLocalHost(), TESTING_PORT);
		return socket;
	}
	
	@Test
	public void requestNonsenseFab() throws IOException {
		Socket socket = getConnection();
		Coordinator.GetFabRequest.Builder gfr = Coordinator.GetFabRequest.newBuilder();
		Coordinator.Request.Builder req = Coordinator.Request.newBuilder();
		req.setType(Type.GET_FAB);
		req.setGetFabReq(gfr.build());
		req.build().writeDelimitedTo(socket.getOutputStream());
		// Get response
		Coordinator.Response rsp = Coordinator.Response.parseDelimitedFrom(socket.getInputStream());
		assert rsp != null;
		assert rsp.getType() == Type.GET_FAB;
		Coordinator.GetFabResponse fabRsp = rsp.getGetFabRsp();
		assert fabRsp != null;
		assert fabRsp.getCode() == RspCode.BAD_DESCRIPTOR;
		socket.close();
	}
}
