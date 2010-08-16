package fabman.coordinator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import fabman.messages.Coordinator;
import fabman.messages.Coordinator.GetFabRequest;
import fabman.messages.Coordinator.ListFabsRequest;

public class FabCoordinator {
	// For now, let's pick something from http://www.iana.org/assignments/port-numbers
	// that is marked as Unassigned.  4499 is easy to remember.
	static final int DEFAULT_COORDINATOR_PORT = 4499;
	ServerSocket listenSocket;
	boolean running;
	
	public FabCoordinator() throws IOException {
		init(DEFAULT_COORDINATOR_PORT);
	}

	public FabCoordinator(int portNumber) throws IOException {
		init(portNumber);
	}

	private void init(int portNumber) throws IOException {
		listenSocket = new ServerSocket(portNumber);
	}
	
	public void run() {
		running = true;
		while (running) {
			try {
				Socket incomingSocket = listenSocket.accept();
				while (! (incomingSocket.isInputShutdown() || incomingSocket.isClosed())) {
					handleRequest(incomingSocket);
				}
			} catch (SocketException e) {
				// Socket may have been closed; if so, terminate.
				running = !listenSocket.isClosed();
			} catch (IOException e) {
				// TODO: log exception
				e.printStackTrace();
			}
		}
	}
	
	public void shutdown() throws IOException {
		listenSocket.close();
	}
	
	void handleRequest(Socket socket) throws IOException {
		Coordinator.Request req = Coordinator.Request.parseFrom(socket.getInputStream());
		Coordinator.Response.Builder rsp = Coordinator.Response.newBuilder();
		rsp.setType(req.getType());
		if (req != null) {
			switch (req.getType()) {
			case LIST_FABS:
				rsp.setListFabsRsp(handleListFabs(socket,req.getListFabsReq()));
				break;
			case GET_FAB:
				rsp.setGetFabRsp(handleGetFab(socket,req.getGetFabReq()));
				break;
			}
		}
		rsp.build().writeTo(socket.getOutputStream());
	}

	private Coordinator.GetFabResponse handleGetFab(Socket socket, GetFabRequest getFabReq) {
		Coordinator.GetFabResponse.Builder rsp = Coordinator.GetFabResponse.newBuilder();
		rsp.setCode(Coordinator.GetFabResponse.RspCode.BAD_DESCRIPTOR);
		return rsp.build();
	}

	private Coordinator.ListFabsResponse handleListFabs(Socket socket, ListFabsRequest listFabsReq) {
		Coordinator.ListFabsResponse.Builder rsp = Coordinator.ListFabsResponse.newBuilder();
		return rsp.build();
	}
}
