import java.io.*;

import java.net.*;
import java.net.UnknownHostException;
import java.rmi.*;
import java.util.Arrays;

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
		    
		    File hashMapFiles = new File("files.hckc");
		    File hashMapFolders = new File("folders.hckc");
		    
		    if(hashMapFiles.exists() && hashMapFolders.exists()){
		    	System.out.println("Hash maps exist!");
		    	monitor.restoreHashMaps();
		    	createFilesInPeer();
		    }else{
		    	System.out.println("Hash maps dont exist!");
		    	// Add folder to listen for
		    	monitor.addDirectory(folder);		    
		    	// Add a dummy listener		    
		    	monitor.addListener (new FileListenerImpl(peer));
		    }
		    monitor.start();
		    
		    peer.connected();
		    
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
	public static void createFilesInPeer(){
		
		try {
			  FileInputStream fstream = new FileInputStream("files.hckc");
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;		
			  while ((strLine = br.readLine()) != null)   {

				  File f = new File(strLine.substring(0, strLine.indexOf("|")));
				  Long l = Long.parseLong(strLine.substring(strLine.indexOf("|") + 1,strLine.length())); 

				//Create an instance of FileElement class to store the attributes for the new file
				FileElement newElement = new FileElement(f.getName(), f.length(), peer.chunkSize, "rmi://"+peer.getIp()+":"+peer.getPort()+"/PeerService", false, true);
					
				//Fill the block_complete array since the file is local and complete
				Arrays.fill(newElement.block_complete, true);
							
				//If localFiles vector already contains the filename, error out
				if ((peer.getFiles().contains(newElement)))
				{
					System.out.println("ERROR: File already exists on local host addFile");
					break;
				}
					
				//Insert FileElement object into arraylist
				peer.getFiles().add(newElement);
			  }
			  //Close the input stream
			  in.close();			  
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
  }



}
