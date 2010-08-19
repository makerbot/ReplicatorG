package fabman.coordinator;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Vector;

import com.google.protobuf.InvalidProtocolBufferException;

import fabman.manager.FabDescriptor;
import fabman.messages.Coordinator;
import fabman.messages.Coordinator.GetFabRequest;
import fabman.messages.Coordinator.GetFabResponse;
import fabman.messages.Coordinator.ListFabsRequest;
import fabman.messages.Coordinator.ListFabsResponse;

public class FabCoordinatorThread implements Runnable {

	private Socket socket;
	private FabCoordinator coordinator;
	
	FabCoordinatorThread(FabCoordinator coordinator, Socket socket) {
		this.coordinator = coordinator;
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

	private GetFabResponse handleGetFab(Socket socket, GetFabRequest getFabReq) {
		GetFabResponse.Builder rsp = Coordinator.GetFabResponse.newBuilder();
		rsp.setCode(GetFabResponse.RspCode.BAD_DESCRIPTOR);
		return rsp.build();
	}

	private ListFabsResponse handleListFabs(Socket socket, ListFabsRequest listFabsReq) {
		ListFabsResponse.Builder rsp = ListFabsResponse.newBuilder();
		Vector<ListFabsResponse.FabDescription> responses = new Vector<ListFabsResponse.FabDescription>();
		ListFabsResponse.FabDescription.Builder fabBuilder;
		Collection<FabDescriptor> descriptors = coordinator.getFabDescriptorList();
		for (FabDescriptor fd : descriptors) {
			fabBuilder = ListFabsResponse.FabDescription.newBuilder();
			fabBuilder.setName(fd.getName());
			fabBuilder.setFabDescriptor(fd.getDescriptorString());
			responses.add(fabBuilder.build());
		}
		rsp.addAllFabs(responses);
		return rsp.build();
	}

}
;