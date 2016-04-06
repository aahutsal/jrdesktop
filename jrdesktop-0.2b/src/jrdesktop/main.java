package jrdesktop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import jrdesktop.server.main.ServerConfig;
import jrdesktop.server.rmi.Server;
import jrdesktop.server.rmi.socketFactory.ice.Tunnel;
import jrdesktop.utilities.FileUtility;
import jrdesktop.viewer.main.ViewerConfig;
import jrdesktop.viewer.rmi.Viewer;

/**
 * main.java
 * @author benbac
 */

public class main {

    public static final URL IDLE_ICON = main.class.getResource("images/net_idle_small.png");
    public static final URL ALIVE_ICON = main.class.getResource("images/net_alive_small.png");     
    
    public static final String SERVER_CONFIG_FILE = 
            FileUtility.getCurrentDirectory() + "server.config";
    public static final String VIEWER_CONFIG_FILE = 
            FileUtility.getCurrentDirectory() + "viewer.config"; 
    
    public static final String KEY_STORE = 
            FileUtility.getCurrentDirectory() + "keystore";   
    public static final String TRUST_STORE = 
            FileUtility.getCurrentDirectory() + "truststore";

    public static Tunnel tunnel = new Tunnel();

    public static void main (String args[]) {
        
         if (System.getSecurityManager() == null)
	    System.setSecurityManager(new SecurityMng());       
            
        System.getProperties().remove("java.rmi.server.hostname");        
               
        SysTray.Show();
       
        if (args.length > 0) {                                     
            if (args[0].equals("-server"))
                startServer(Integer.parseInt(args[1]), args[2], args[3], 
                        Boolean.valueOf(args[4]), Boolean.valueOf(args[5]));
            else if (args[0].equals("-viewer"))
                startViewer(args[1], Integer.parseInt(
                        args[2]), args[3], args[4], Boolean.valueOf(args[5]));                  
        }
    }      
       
    public static void startServer(int port, 
            String username, String password, 
            boolean ssl_enabled, boolean multihomed_enabled) {
        
        ServerConfig.SetConfiguration(port, username, password, 
                ssl_enabled, multihomed_enabled);

        Server.Start();
    }
    
    public static void startViewer(String server, int port, 
            String username, String password, boolean ssl_enabled) {
        
        ViewerConfig.SetConfiguration(server, port, 
                username, password, ssl_enabled);
        
        new Viewer().Start();     
    }            
    
   public static void setStoreProperties() {     
        System.setProperty("javax.net.ssl.trustStore", TRUST_STORE); 
        System.setProperty("javax.net.ssl.trustStorePassword", "trustword"); 
        System.setProperty("javax.net.ssl.keyStore", KEY_STORE); 
        System.setProperty("javax.net.ssl.keyStorePassword", "password");   
   }
    
   public static void clearStoreProperties() {
        System.getProperties().remove("javax.net.ssl.trustStore"); 
        System.getProperties().remove("javax.net.ssl.trustStorePassword");         
        System.getProperties().remove("javax.net.ssl.keyStore"); 
        System.getProperties().remove("javax.net.ssl.keyStorePassword");               
    }      
    
    public static void exit() {
        if (Server.isRunning()) {
            Server.Stop();
            tunnel.stop();
        }
        clearStoreProperties();
        System.setSecurityManager(null);
        System.exit(0);
    }
}
