package fabman.coordinator;

import java.io.IOException;
import java.net.Socket;

import com.google.protobuf.InvalidProtocolBufferException;

import fabman.messages.Coordinator;
import fabman.messages.Coordinator.GetFabRequest;
import fabman.messages.Coordinator.ListFabsRequest;

public class FabCoordinatorThread implements Runnable {

	private Socket socket;
	
	FabCoordinatorThread(Socket socket) {
		this.socket = socket;
	}
	
	public void run() {
		try {
			while (! (socket.isInputShutdown() || socket.isClosed())) {
				handleRequest(socket);
			}
		} catch (InvalidProtocolBufferException ipbe) {
			// This is a peculiarity of using delimited protocol buffer
			// streams; a buffer exception gets thrown on end-of-stream.
			// We can let this pass and drop the connection.
		} catch (IOException e) {
			// TODO: log exception.
			e.printStackTrace();
		}
	}

	void handleRequest(Socket socket) throws IOException {
		Coordinator.Request req = Coordinator.Request.parseDelimitedFrom(socket.getInputStream());
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
		rsp.build().writeDelimitedTo(socket.getOutputStream());
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
