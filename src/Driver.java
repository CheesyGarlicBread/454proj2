import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
public class Driver {
	
	
	
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
		System.out.println("Started Client");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try{
			String address = InetAddress.getLocalHost().getHostAddress();
			peer = new Peer(address,args[1],args[2]);
			peer.getPeers().initialize(args[0], args[1],args[2]);
			peerServer = new PeerServer(peer);
			Thread t = new Thread(peerServer);
			t.start();
		}catch(RemoteException e){
			System.out.println("Remote connection issue.");			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//share folder
		try{
			System.out.println("looking for " + args[2]);
			File folder = new File(args[2]);
			File[] listOfFiles = folder.listFiles();

		    for (int i = 0; i < listOfFiles.length; i++) {
		      if (listOfFiles[i].isFile()) {
		    	  System.out.println(args[2]+listOfFiles[i].getName());
		        peer.insert(args[2]+listOfFiles[i].getName());
		      }
		    }
		}catch(NullPointerException e){
			System.out.println("Couldnt find folder");
			
		}
	    
		
		//keep reading input
		while(true){
			System.out.println("\nPlease enter a command:");
			System.out.println("\"join\" - Joins the set of configured peers.");
			System.out.println("\"leave\" - Leaves the set of configured peers.");
			System.out.println("\"insert <filepath>\" - Adds file to be shared across network.");
			System.out.println("\"query\" - Returns the state of the files for local peer.");
			System.out.print("> ");
			try{
				String input = br.readLine();
				parseCommand(input);
			}catch (IOException e){
				System.out.println("IOException encountered.");
				try {
					UnicastRemoteObject.unexportObject(peerServer.getRegistry(), true);
				} catch (NoSuchObjectException e1) {				
				}
				System.exit(1);
			}
			
		}
		
	}
	
	public static void parseCommand(String command){		
		//tokenize string
		StringTokenizer st = new StringTokenizer(command, " ");
		int returnCode = 0;
		
		
		if(st.hasMoreTokens()){
			//get command
			String c = st.nextToken().toLowerCase();			
			if(c.equals("insert")){
				try{
					//call insert
					returnCode = peer.insert(st.nextToken());
				}catch(NoSuchElementException e){
					System.out.println("No filename specified");
				}
			}else if(c.equals("query")){
				Status status = new Status();
				//returnCode = peer.query(status);			
			}else if(c.equals("join")){
				//returnCode = peer.join();
			}else if(c.equals("leave")){
				//returnCode = peer.leave();
			}else{
				returnCode = 500;
			}
		}
		
		//check return code
		if(returnCode == 0){
			System.out.println("Command Success");
		}else if(returnCode > 0){
			System.out.print("Ran into a warning: ");
			switch(returnCode){
				case 1:
					System.out.println("Unknown Warning.");
					break;
				case 5:
					System.out.println("Peer not found.");
					break;
				case 500:
					System.out.println("Command not found.");
					break;
				default:
					break;
			}
			
		}else if (returnCode < 0){
			System.out.print("Ran into an error: ");
			switch(returnCode){				
				case -2:
					System.out.println("Unknown Error.");
					break;
				case -3:
					System.out.println("Cannot connect.");
					break;
				case -4:
					System.out.println("Cannot find peer.");
					break;
				default:
					break;
			}
		}
	}
	

}
