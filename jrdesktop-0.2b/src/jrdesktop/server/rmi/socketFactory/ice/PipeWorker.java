package jrdesktop.server.rmi.socketFactory.ice;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public class PipeWorker implements Runnable {
	private List queue = new LinkedList();
	
	public void processData(NioProxy server, SocketChannel socket, byte[] data, int count) {
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		synchronized(queue) {
			queue.add(new ServerDataEvent(server, socket, dataCopy));
			queue.notify();
		}
	}
	
	public void run() {
		ServerDataEvent dataEvent;
		
		while(true) {
			// Wait for data to become available
			synchronized(queue) {
				while(queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
					}
				}
				dataEvent = (ServerDataEvent) queue.remove(0);
			}
			
			// determining receiver, if our index is odd, then next, if even, then previous
			int myIdx = NioProxy.socketChannels.indexOf(dataEvent.socket);
			boolean odd = myIdx % 2 == 0;
			int receiverIdx = myIdx + (odd?1:-1);
			// sending data only if receiver exists
			if(receiverIdx < NioProxy.socketChannels.size()) {
				SocketChannel receiver = NioProxy.socketChannels.get(receiverIdx);
				dataEvent.server.send(receiver, dataEvent.data);
			}
		}
	}
}
