package jrdesktop.server.rmi.socketFactory.ice;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by archer on 4/2/16.
 */
public class Tunnel implements Runnable {

    protected class InfiniteStreamCopier implements Runnable {
        private final InputStream iStream;
        private final OutputStream oStream;

        public InfiniteStreamCopier(InputStream is, OutputStream os){
            this.iStream = is;
            this.oStream = os;
        }

        @Override
        public void run() {
            int b;
            try {
                while ((b = iStream.read()) > -1) {
                    oStream.write(b);
                }
                iStream.close();
                oStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void run() {
        /** this is a tunnel between our server and Gateway **/
        final InputStream inBis, outBis;
        final OutputStream inBos, outBos;

        final Socket inSock = new Socket();
        final Socket outSock = new Socket();

        try {
            System.out.println("Letting main.startServer() complete");
            Thread.sleep(2000); // letting server main.startServer() complete
            final String address = System.getenv("GW_ADDRESS");
            final int port = Integer.parseInt(System.getenv("GW_PORT"));
            System.out.println("Connecting... to " + address + " on " + port);
            inSock.connect(new InetSocketAddress(address, port));
            System.out.println("Connected to gateway");
            outSock.connect(new InetSocketAddress("localhost", NioProxy.INTERNAL_PORT));
            System.out.println("Connected to local");

            inBis = inSock.getInputStream();
            inBos = inSock.getOutputStream();

            outBis = outSock.getInputStream();
            outBos = outSock.getOutputStream();


            System.out.println("Tunnel established");
            // in --> out
            Thread th1 = new Thread(new InfiniteStreamCopier(inBis, outBos));
            Thread th2 = new Thread(new InfiniteStreamCopier(outBis, inBos));

            th1.start();
            th2.start();

            th1.join();
            th2.join();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
