package jrdesktop.viewer.rmi;

import java.io.IOException;
import java.rmi.RemoteException;

import de.root1.simon.Lookup;
import de.root1.simon.Simon;
import jrdesktop.server.rmi.Server;
import jrdesktop.server.rmi.ServerInterface;
import jrdesktop.viewer.main.Recorder;
import jrdesktop.utilities.InetAdrUtility;

import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import jrdesktop.HostProperties;
import jrdesktop.main;
import jrdesktop.utilities.FileUtility;
import jrdesktop.utilities.ZipUtility;
import jrdesktop.viewer.main.ViewerConfig;

/**
 * Viewer.java
 * @author benbac
 */

public class Viewer extends Thread {
    
    private int index = -1;
    private Recorder recorder;
    
    private Lookup lookup;
    private ServerInterface rmiServer;
       
    private String server = "127.0.0.1";
    private int port = 6666;
    private String username = "admin";
    private String password = "admin";
    private boolean ssl_enabled = false;
    
    private boolean connected = false;
    
    private ArrayList<Object> Objects;        
        
    public Viewer () {
        ViewerConfig.loadConfiguration();
        server = ViewerConfig.server_address;
        port = ViewerConfig.server_port;  
        username = ViewerConfig.username;
        password = ViewerConfig.password;
        ssl_enabled = ViewerConfig.ssl_enabled;
        
        if (ssl_enabled) {     
            FileUtility.checkFile(main.KEY_STORE, "keystore");
            FileUtility.checkFile(main.TRUST_STORE, "truststore");
            main.setStoreProperties();
        }
        else
            main.clearStoreProperties();
    }   
    
    public boolean isConnected() {
        return connected;
    }
    
    public void Start() { 
        connect();
        if (connected) {
            recorder = new Recorder(this);        
            recorder.viewerGUI.Start();
        }        
        else Stop();
    }
    
    public void Stop() {
        if (recorder != null) {
            recorder.Stop();
            recorder.interrupt();
        }
        disconnect();    
        interrupt();
    }
    
    public int connect() {  
        connected = false;
        
        try {
            lookup = Simon.createNameLookup(server, port);
            System.out.println("Lookup created for " + server + ":" + port);
            rmiServer = (ServerInterface) lookup.lookup(Server.BIND_NAME);
            System.out.println("Viewer connected to " + rmiServer);
            index = rmiServer.startViewer(InetAdrUtility.getLocalAdr(),
                    username, password);
            if (index == -1) {
                JOptionPane.showMessageDialog(null, 
                        "Wrong username or password !!", "Error !!",
                    JOptionPane.ERROR_MESSAGE);   
                return -1;
            }             
                
            Objects = new ArrayList<Object>();
            Objects.add(HostProperties.getLocalProperties());
            connected = true;
            return index;
       } catch (Exception e) {    
           JOptionPane.showMessageDialog(null, e.getMessage(), "Error !!",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
           return -1;
       }     
    }
    
    public void disconnect() {
        connected = false;
        try {
            if (rmiServer != null) {
                    rmiServer.stopViewer(index);
                    UnicastRemoteObject.unexportObject(rmiServer, true);
            }
            lookup.release(rmiServer);
        }
        catch (Exception e) {
            e.getStackTrace(); 
        } 
      rmiServer = null;
      lookup = null;
    }
    
    public void updateData(Object object) {
        byte[] data;
        try {
            if (recorder.viewerData.isDataCompressionEnabled())
                data = ZipUtility.compressObject(object, 
                        recorder.viewerData.getCompressionLevel());
            else
                data = ZipUtility.objecttoByteArray(object);                  

            recorder.connectionInfos.incSentData(data.length);
            updateData(data);  
        }
        catch (IOException e) {
            e.getStackTrace();
        }
    }
    
    public void updateData(byte[] data) {
        try {rmiServer.updateData(data, index);} 
        catch (Exception re) {
            re.getStackTrace();
        }        
    }
    
    public void AddObjects(ArrayList<Object> objects) {
        Objects.addAll(objects);
    } 
    
    public void AddObject(Object object) {
        Objects.add(object);
    }   
   
    public void updateOptions () {     
        try {rmiServer.updateOptions(
                recorder.getViewerData(), index);} 
        catch (Exception re) {
            re.getStackTrace();
        }          
    }
    
    public void sendData() {
        ArrayList SendObjects;       
        synchronized(Objects){              
            if (recorder.viewerData.isClipboardTransferEnabled())
                Objects.add(recorder.clipbrdUtility.getClipboardContent());              
            
            SendObjects = Objects;  
            Objects = new ArrayList<Object>();
        }
        updateData(SendObjects);
    }     
    
    public void recieveData() {
        Object object = null;
        try {
            byte[] data = rmiServer.updateData(index);

            if (recorder.viewerData.isDataCompressionEnabled())        
                object = ZipUtility.decompressObject(data);
            else
                object = ZipUtility.byteArraytoObject(data);   
           
            recorder.connectionInfos.incReceivedData(data.length);
            recorder.updataData((ArrayList) object);    
        }
        catch (Exception e) {
            e.getStackTrace();
        }           
    }   
    
    public byte[] ReceiveFile(String filename) {
        try {            
            byte[] data = rmiServer.ReceiveFile(filename, index);
            recorder.connectionInfos.incReceivedData(data.length);
            return data;
        } catch(RemoteException re) {
            re.getStackTrace();
            return null;
        }
    }
    
    public void SendFile (byte[] buffer, String filename) {
        try {        
            rmiServer.SendFile(buffer, filename, index);
            recorder.connectionInfos.incSentData(buffer.length);
       } catch(RemoteException re) {
            re.getStackTrace();
       }        
    } 
}
