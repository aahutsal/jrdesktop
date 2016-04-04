package jrdesktop;

import jrdesktop.server.rmi.socketFactory.ice.Tunnel;
import jrdesktop.viewer.main.ConnectionDialog;
import jrdesktop.server.main.ActiveConnectionsGUI;
import jrdesktop.server.main.ConfigGui;
import jrdesktop.server.rmi.Server;
import jrdesktop.server.main.ServerConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 * SysTray.java
 * @author benbac
 */

public class SysTray {
    
    final static public int SERVER_STARTED = 0;
    final static public int SERVER_STOPPED = 1;
    final static public int CONNECTION_FAILED = 2;
    
    private static MenuItem serverItem;
    private static TrayIcon trayIcon;
    
    public static void updateServerStatus(int msgType) {
        if (!SystemTray.isSupported()) return;
        switch (msgType) {
            case SERVER_STARTED: 
                serverItem.setLabel("Stop Server");      
                trayIcon.displayMessage("Connection status", "Server Started !!", 
                    TrayIcon.MessageType.INFO);
                trayIcon.setImage(new ImageIcon(main.ALIVE_ICON).getImage());
                trayIcon.setToolTip("jrdesktop [running]\n" + ServerConfig.server_address);
                break;        
            case CONNECTION_FAILED:            
                trayIcon.displayMessage("Connection status", "Connection Failed !!", 
                    TrayIcon.MessageType.ERROR);  
                break;
            case SERVER_STOPPED:
                serverItem.setLabel("Start Server");               
                trayIcon.displayMessage("Connection status", "Server Stopped !!", 
                    TrayIcon.MessageType.INFO);            
                trayIcon.setImage(new ImageIcon(main.IDLE_ICON).getImage());
                trayIcon.setToolTip("jrdesktop [stopped]\n" + ServerConfig.server_address);
                break;                
        }
        serverItem.setEnabled(true);
    }
    
    public static void displayMsg(String Title, String Msg) {
        trayIcon.displayMessage(Title, Msg, TrayIcon.MessageType.INFO);
    }
    
    public static boolean isServerRunning() {
        boolean bool = Server.isRunning();
        if (!bool)
            JOptionPane.showMessageDialog(null, 
                "Server is not running !!", 
                "Information", JOptionPane.INFORMATION_MESSAGE);            
        return bool;
    }
    
    public static void Show() {
        Runnable runner = new Runnable() {
            public void run() {
                if (SystemTray.isSupported()) {
                    final SystemTray tray = SystemTray.getSystemTray();
                    PopupMenu popup = new PopupMenu();
                    trayIcon = new TrayIcon(new ImageIcon(main.IDLE_ICON).getImage(), 
                            "jrdesktop", popup);
                    MenuItem item = new MenuItem("-");
                    popup.add(item);
                    
                    serverItem = new MenuItem("Start Server");
                    serverItem.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            serverItem.setEnabled(false);
                            if (Server.isRunning())
                                Server.Stop();            
                            else {
//                                new Thread(new Tunnel()).start();
//
                                Server.Start();
                            }
                        }
                    });
                    popup.add(serverItem);
                    
                    item = new MenuItem("Active Connections");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (isServerRunning())
                                ActiveConnectionsGUI.main(null);                          
                        }
                    });
                    popup.add(item);   
                    
                    item = new MenuItem("Server configuration");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            ConfigGui.main(null);
                        }
                    });
                    popup.add(item);
                    
                    Menu menu = new Menu("File transfer");
                    item = new MenuItem("Send Files");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (isServerRunning())
                                if (Server.SendClipboardFileList() == 0) {
                                    // no files in clipboard, exit
                                    JOptionPane.showMessageDialog(null, 
                                    "No file in clipboard !!", 
                                    "Information", JOptionPane.INFORMATION_MESSAGE);                                        
                                }                                                                                   
                        }
                    });
                    menu.add(item);
                    
                    item = new MenuItem("Receive Files");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (isServerRunning())
                                Server.setUploadingFolder();                                                          
                        }
                    });
                    menu.add(item);
                    popup.add(menu);
                    
                    item = new MenuItem("-");
                    popup.add(item);   
                    
                    item = new MenuItem("Connect to Server ...");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            ConnectionDialog.main(null);                            
                        }
                    });
                    popup.add(item);                                                           
                    
                    item = new MenuItem("-");
                    popup.add(item);
                    
                    item = new MenuItem("About");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            AboutGui.main(null);
                        }
                    });
                    popup.add(item);
                    
                    item = new MenuItem("Exit");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (JOptionPane.showConfirmDialog(null, 
                                    "Exit application ?") == 
                                    JOptionPane.YES_OPTION) {                          
                                tray.remove(SysTray.trayIcon);
                                main.exit();
                            }
                        }
                    });
                    popup.add(item);

                    try {
                        tray.add(trayIcon);
                    } catch (AWTException e) {
                        System.err.println("Can't add to tray");
                    }
                } else
                    System.err.println("Tray unavailable");
            }
        };
        EventQueue.invokeLater(runner);
    }
}
