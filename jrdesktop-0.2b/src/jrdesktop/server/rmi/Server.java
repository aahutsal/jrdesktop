package jrdesktop.server.rmi;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import javax.swing.JOptionPane;

import de.root1.simon.Registry;
import de.root1.simon.Simon;
import jrdesktop.ConnectionInfos;
import jrdesktop.HostProperties;
import jrdesktop.SysTray;
import jrdesktop.main;
import jrdesktop.server.main.ServerConfig;
import jrdesktop.viewer.main.ViewerData;
import jrdesktop.server.main.robot;
import jrdesktop.server.rmi.socketFactory.MultihomeRMIClientSocketFactory;
import jrdesktop.utilities.ClipbrdUtility;
import jrdesktop.utilities.FileUtility;
import jrdesktop.utilities.ZipUtility;

/**
 * Server.java
 * @author benbac
 */

public class Server extends Thread {

    public static String BIND_NAME = "ServerImpl";

    private static boolean running = false;
    
    private static Registry registry;
    private static ServerImpl serverImpl;
    
    private static robot rt = new robot();
    public static ClipbrdUtility clipbrdUtility;
    
    private static ArrayList<Object> Objects = new ArrayList<Object>();         
    private static ArrayList<ViewerData> viewers = new ArrayList<ViewerData>(); 
    private static ArrayList<InetAddress> viewersAds = new ArrayList<InetAddress>();     
    private static ArrayList<ConnectionInfos> connectionsInfos = new ArrayList<ConnectionInfos>(); 
        
    private static String uploadingFolder;   
        
    public static void Start() { 
        running = false;                 
        ServerConfig.loadConfiguration();
        if (ServerConfig.ssl_enabled) {        
            FileUtility.checkFile(main.KEY_STORE, "keystore");
            FileUtility.checkFile(main.TRUST_STORE, "truststore");        
            main.setStoreProperties();            
        }
        else
            main.clearStoreProperties();        
        
        if (ServerConfig.default_address)
            System.setProperty("java.rmi.server.hostname", ServerConfig.server_address);
        else
            System.getProperties().remove("java.rmi.server.hostname");        
         
        try{
            // ServerImpl serverImpl = new ServerImpl();
            // registry = LocateRegistry.createRegistry(Config.server_port);
            
            if (ServerConfig.ssl_enabled && ServerConfig.multihomed_enabled)
                serverImpl = new ServerImpl(
                        new MultihomeRMIClientSocketFactory(
                            new SslRMIClientSocketFactory()),
                        new SslRMIServerSocketFactory(null, null, true));
            else if (ServerConfig.ssl_enabled && !ServerConfig.multihomed_enabled)
                serverImpl = new ServerImpl(
                        new SslRMIClientSocketFactory(), 
                        new SslRMIServerSocketFactory(null, null, true));                    
            else if (!ServerConfig.ssl_enabled && ServerConfig.multihomed_enabled)
                serverImpl = new ServerImpl(
                        new MultihomeRMIClientSocketFactory(null), null);
                            //new clientSocketFactory()),            
                        //new serverSocketFactory());                        
            else if (!ServerConfig.ssl_enabled && !ServerConfig.multihomed_enabled)
                serverImpl = new ServerImpl();
                       // new clientSocketFactory(),  
                      //  new serverSocketFactory());                         

            System.out.println("Simon is creating a registry on port "+ ServerConfig.server_port);
            registry = Simon.createRegistry(ServerConfig.server_port);
            System.out.println("Registry is created. "+ registry.toString());
            registry.start(); // TODO check exception
            System.out.println("Registry started.");
            registry.bind(Server.BIND_NAME, serverImpl);
            System.out.println("ServerImpl is bound.");

        } catch (Exception e) {                  
            e.getStackTrace();
            Stop();          
            
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error !!",
                    JOptionPane.ERROR_MESSAGE); 
            return;
        }   
        
        running = true;
        clipbrdUtility = new ClipbrdUtility();         
        uploadingFolder = FileUtility.getCurrentDirectory(); 
        SysTray.updateServerStatus(SysTray.SERVER_STARTED);        
    }              
    
    public static void Stop() {
        if (running) {
            running = false;
            disconnectAllViewers();
            SysTray.updateServerStatus(SysTray.SERVER_STOPPED);            
        }
        else
            SysTray.updateServerStatus(SysTray.CONNECTION_FAILED);
        try {            
            if (registry != null) {
                registry.stop();
                registry.unbind(Server.BIND_NAME);
            }
        } catch (Exception e) {
            e.getStackTrace();
        }  
        registry = null;
        serverImpl = null;
    }
    
    public static boolean isRunning() {
        return running;
    }
    
    public static void updateOptions(Object data, int index) {
        ArrayList Options = (ArrayList) data;         
        
        viewers.get(index).setScreenScale((Float) Options.get(0));  
        viewers.get(index).setScreenRect(
                rt.getCustomScreenRect((Rectangle) Options.get(1)));  
        viewers.get(index).setCompressionLevel((Integer) Options.get(2)); 
        viewers.get(index).setDataCompression((Boolean) Options.get(3));            
        viewers.get(index).setCompressionQuality((Float) Options.get(4));
        viewers.get(index).setColorQuality((Integer) Options.get(5)); 
        viewers.get(index).setClipboardTransfer((Boolean) Options.get(6));  
        viewers.get(index).setInetAddress((InetAddress) Options.get(7));
    }
        
    public static void updateData(byte[] data, int index) {    
        Object object;
        try {
            if (viewers.get(index).isDataCompressionEnabled())        
                object = ZipUtility.decompressObject(data);
            else
                object = ZipUtility.byteArraytoObject(data);
            
            connectionsInfos.get(index).incReceivedData(data.length);
            rt.updateData(object, viewers.get(index));               
        }
        catch (Exception e) {
            e.getStackTrace();
        }       
    }

    public static byte[] updateData(int index) {  
        byte[] data = null;            
                
        Objects.add(viewers.get(index).getScreenRect());
        Objects.add(rt.CaptureScreenByteArray(viewers.get(index)));             
        if (viewers.get(index).isClipboardTransferEnabled())
            Objects.add(clipbrdUtility.getClipboardContent());

        synchronized(Objects) {
            try {
                if (viewers.get(index).isDataCompressionEnabled())
                    data = ZipUtility.compressObject(Objects,
                        viewers.get(index).getCompressionLevel());
                else
                    data = ZipUtility.objecttoByteArray(Objects);  
            }
            catch (IOException e) {
                e.getStackTrace();
            }
        
            Objects = new ArrayList<Object>();            
        }
        connectionsInfos.get(index).incSentData(data.length);
        return data;   
    }
    
    public static void AddObjects(ArrayList<Object> objects) {
        Objects.addAll(objects);
    }  

    public static void AddObject(Object object) {
        Objects.add(object);
    } 
    
    public static synchronized int addViewer(InetAddress inetAddress,
            String username, String password) {
        if (!ServerConfig.username.equals(username) || 
                !ServerConfig.password.equals(password))
            return -1;
        SysTray.displayMsg("Viewer details", inetAddress + " connected !!");
        int viewerPos = viewersAds.indexOf(inetAddress);  
        if (viewerPos == -1) {
            viewers.add(new ViewerData());            
            viewersAds.add(inetAddress);
            connectionsInfos.add(new ConnectionInfos(true));
            return viewersAds.size() -1;
        }
        return -1;
    }
         
    public static synchronized int removeViewer(int index) {
        SysTray.displayMsg("Viewer details", viewers.get(index).getInetAddress()
                + " disconnected !!");
        viewers.remove(index);
        viewersAds.remove(index);
        connectionsInfos.remove(index);
        return index;
    } 
    
    public static void disconnectAllViewers() {
        for (int i=0; i<viewers.size(); i++)
            removeViewer(i);
    }
    
   public static byte[] ReceiveFile(String fileName, int index){
      try {
         File file = new File(fileName);
         byte buffer[] = new byte[(int)file.length()];
         BufferedInputStream input = new
            BufferedInputStream(new FileInputStream(file));
         input.read(buffer, 0, buffer.length);
         input.close();
               
         connectionsInfos.get(index).incSentData(buffer.length);
         return(buffer);
      } catch(Exception e){
            e.printStackTrace();
         return(null);
      }
   }
      
    public static void SendFile(byte[] filedata, String fileName, int index) {
        try {             
            fileName = uploadingFolder + fileName;
            new File(new File(fileName).getParent()).mkdirs();
            File file = new File(fileName);

            BufferedOutputStream output = new
                BufferedOutputStream(new FileOutputStream(file));
            output.write(filedata, 0, filedata.length);
            output.flush();
            output.close();  
                      
            connectionsInfos.get(index).incReceivedData(filedata.length);
        } catch (Exception e) {
            e.getStackTrace();
        }                 
   }   
    
    public static int SendClipboardFileList() {
        File[] files = clipbrdUtility.getFilesFromClipboard();
        if (files.length == 0) return 0;
        ArrayList<Object> FileSysInfos = new ArrayList<Object>();
        FileSysInfos.add(files[0].getParent() + File.separatorChar);
        FileSysInfos.add(FileUtility.getAllFiles(files)); 
        AddObject(FileSysInfos);       
        return files.length;
    }
    
    public static void setUploadingFolder () {
       uploadingFolder = FileUtility.getCurrentDirectory();
       AddObject(new File("."));        
    }     
    
    public static ArrayList<InetAddress> getViewersAds () {
        return viewersAds;
    }
    
    public static void displayViewerProperties (int index) {
        HostProperties.displayRemoteProperties(
            viewers.get(index).getProperties());
    }
    
    public static void setViewerProperties (InetAddress inetAddress, 
            Hashtable props) {
        int index = viewersAds.indexOf(inetAddress);
        viewers.get(index).setProperties(props);
    }
    
    public static void displayConnectionInfos(int index) {
        connectionsInfos.get(index).display();
    }
}
