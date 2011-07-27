import java.io.*;

import java.net.*;
import java.net.UnknownHostException;
import java.rmi.*;

public class Driver{

	private static Peer peer;
	private static PeerServer peerServer;
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if(args.length < 3){
			System.out.println("Didn't specify peers file or port");
			System.out.println("Arguments specification: <peersfile> <port #> <download folder>");
			System.exit(1);
		}
		String peersFile = args[0];
		String localPort = args[1];
		String downloadFolder = args[2];
		System.out.println("Started Client");		
		try{
			String address = InetAddress.getLocalHost().getHostAddress();
			peer = new Peer(address,localPort,downloadFolder);
			//initialize peers list
			peer.getPeers().initialize(peersFile, localPort, downloadFolder);
			
			peerServer = new PeerServer(peer);
			Thread t = new Thread(peerServer);
			t.start();
			
			//check folder for changes
			System.out.println("Watching for file changes in " + downloadFolder);
			 // Create the monitor
		    FileMonitor monitor = new FileMonitor (2000);
		    File folder = new File(downloadFolder);
		    
		    // Add folder to listen for
		    monitor.addDirectory(folder);
		    
		    // Add a dummy listener
		    
		    monitor.addListener (new FileListenerImpl(peer));

			while(true){

			}
		}catch(RemoteException e){
			System.out.println(e);
			System.out.println("Remote connection issue. EXITING");			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	    

		
		
		
	}
	



}
