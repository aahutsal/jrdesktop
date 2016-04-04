package jrdesktop.server.rmi.socketFactory.ice;
import java.nio.channels.SocketChannel;

class ServerDataEvent {
	public NioProxy server;
	public SocketChannel socket;
	public byte[] data;
	
	public ServerDataEvent(NioProxy server, SocketChannel socket, byte[] data) {
		this.server = server;
		this.socket = socket;
		this.data = data;
	}
}