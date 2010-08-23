package fabman.coordinator;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

class FabricatorPool {
	HashMap<String, Socket> fabMap = new HashMap<String, Socket>();
	
	public FabricatorPool() {
		discovery();
	}
	
	/*
	 * Perform discovery on running fabricator threads.
	 */
	void discovery() {
	}
	
	public Socket getSocketForName(String name, boolean autostart) {
		Socket socket = fabMap.get(name);
		if (socket != null) {
			try {
				Socket probe = new Socket(socket.getInetAddress(),socket.getPort());
			} catch (IOException e) {
				e.printStackTrace();
				socket = null;
			}
		}
		return null;
	}
}
