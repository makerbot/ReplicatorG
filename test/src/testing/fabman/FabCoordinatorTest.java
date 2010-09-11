package testing.fabman;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import fabman.coordinator.FabCoordinator;
import fabman.messages.Coordinator;
import fabman.messages.Coordinator.GetFabRequest;
import fabman.messages.Coordinator.GetFabResponse;
import fabman.messages.Coordinator.Request;
import fabman.messages.Coordinator.Response;
import fabman.messages.Coordinator.Type;
import fabman.messages.Coordinator.GetFabResponse.RspCode;

public class FabCoordinatorTest {

	FabCoordinator coordinator = null;
	static final int TESTING_PORT = 5115;
	
	@BeforeClass
	public void createCoordinator() throws IOException {
		assert coordinator == null;
		try {
			Vector<File> paths = new Vector<File>();
			String descriptorPath = System.getenv("TEST_DESCRIPTORS");
			assert descriptorPath != null : "TEST_DESCRIPTORS environment variable is unset; needed for test";
			paths.add(new File(descriptorPath));
			coordinator = new FabCoordinator(TESTING_PORT,paths);
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
		GetFabRequest.Builder gfr = GetFabRequest.newBuilder();
		Request.Builder req = Request.newBuilder();
		gfr.setName("NON-EXTANT FABRICATOR");
		req.setType(Type.GET_FAB);
		req.setGetFabReq(gfr.build());
		req.build().writeDelimitedTo(socket.getOutputStream());
		// Get response
		Response rsp = Response.parseDelimitedFrom(socket.getInputStream());
		assert rsp != null;
		assert rsp.getType() == Type.GET_FAB;
		GetFabResponse fabRsp = rsp.getGetFabRsp();
		assert fabRsp != null;
		assert fabRsp.getCode() == RspCode.BAD_DESCRIPTOR;
		socket.close();
	}

	@Test
	public void requestFabList() throws IOException {
		Socket socket = getConnection();
		Coordinator.ListFabsRequest.Builder gfr = Coordinator.ListFabsRequest.newBuilder();
		Coordinator.Request.Builder req = Coordinator.Request.newBuilder();
		req.setType(Type.LIST_FABS);
		req.setListFabsReq(gfr.build());
		req.build().writeDelimitedTo(socket.getOutputStream());
		// Get response
		Coordinator.Response rsp = Coordinator.Response.parseDelimitedFrom(socket.getInputStream());
		assert rsp != null;
		assert rsp.getType() == Type.LIST_FABS;
		Coordinator.ListFabsResponse fabRsp = rsp.getListFabsRsp();
		assert fabRsp != null;
		assert fabRsp.getFabsCount() == 5 : "Expected fab count 5, got "+Integer.toString(fabRsp.getFabsCount());
		socket.close();
	}
	
	@Test
	public void getTestFab() throws IOException {
		Socket socket = getConnection();
		GetFabRequest.Builder gfr = GetFabRequest.newBuilder();
		Request.Builder req = Request.newBuilder();
		gfr.setName("No Driver Test");
		req.setType(Type.GET_FAB);
		req.setGetFabReq(gfr.build());
		req.build().writeDelimitedTo(socket.getOutputStream());
		// Get response
		Response rsp = Response.parseDelimitedFrom(socket.getInputStream());
		assert rsp != null;
		assert rsp.getType() == Type.GET_FAB;
		GetFabResponse fabRsp = rsp.getGetFabRsp();
		assert fabRsp != null;
		assert fabRsp.getCode() == RspCode.ERROR;
		System.err.println("No driver error message: "+fabRsp.getErrorMessage());
		socket.close();
	}
}
