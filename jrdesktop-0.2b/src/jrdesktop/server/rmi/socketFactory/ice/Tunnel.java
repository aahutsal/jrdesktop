package jrdesktop.server.rmi.socketFactory.ice;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by archer on 4/2/16.
 *
 * this is a tunnel between our server and Gateway
 */
public class Tunnel {
    public static final int INTERNAL_PORT = 59999;

    private Socket remoteSock;
    private Socket localSock;

    private Thread th1;
    private Thread th2;
    private volatile boolean stopFlag = false;

    protected class InfiniteStreamCopier implements Runnable {
        private InputStream iStream;
        private OutputStream oStream;

        public InfiniteStreamCopier(InputStream is, OutputStream os){
            this.iStream = is;
            this.oStream = os;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " started work");
            int b;
            try {
                while (!stopFlag && ((b = iStream.read()) > -1)) {
                    oStream.write(b);
                }
            } catch (IOException e) {
                stopFlag = true;
                e.printStackTrace();
            } finally {
                System.out.println(Thread.currentThread().getName() + " finished work");
            }
        }
    }

    public void start() {
        stopFlag = false;

        try {
            Thread.sleep(2000); // wait for server to start
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            String address = System.getenv("GW_ADDRESS");
            int port = Integer.parseInt(System.getenv("GW_PORT"));
            System.out.print("Connecting to NioProxy on " + address + ":" + port + " ... ");
            remoteSock = new Socket();
            remoteSock.connect(new InetSocketAddress(address, port));
            System.out.println("connected");

            System.out.print("Connecting to LocalServer on localhost:" + INTERNAL_PORT + " ... ");
            localSock = new Socket();
            localSock.connect(new InetSocketAddress("localhost", INTERNAL_PORT));
            System.out.println("connected");

            th1 = new Thread(new InfiniteStreamCopier(remoteSock.getInputStream(), localSock.getOutputStream()));
            th1.setName("Local -> Remote Thread");
            th2 = new Thread(new InfiniteStreamCopier(localSock.getInputStream(), remoteSock.getOutputStream()));
            th2.setName("Local <- Remote Thread");
            System.out.println("Tunnel configured");

            th1.start();
            th2.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        System.out.println("Stopping Tunnel threads and sockets");
        stopFlag = true;

        try {
            th1.join();
            th2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            remoteSock.close();
            localSock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
