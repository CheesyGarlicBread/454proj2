import java.io.*;
import java.net.InetAddress;
import java.util.*;

public class Peers {
	    
    private Vector<Peer> peers = new Vector<Peer>();
    
    // The peersFile is the name of a file that contains a list of the peers
    // Its format is as follows: in plaintext there are up to maxPeers lines,
    // where each line is of the form: <IP address> <port number>
    // This file should be available on every machine on which a peer is started,
    // though you should exit gracefully if it is absent or incorrectly formatted.
    // After execution of this method, the _peers should be present.
    public int initialize(String peersFile, String localport, String downloadFolder)
    {
    	try{
			BufferedReader br = new BufferedReader(new FileReader(peersFile));
			String line;
			String localaddress = InetAddress.getLocalHost().getHostAddress();
			String localhostname = InetAddress.getLocalHost().getHostName();		
			while((line = br.readLine()) != null){
				StringTokenizer st = new StringTokenizer(line, " ");
				String ip = st.nextToken();
				String port = st.nextToken();
				if(!((localaddress.equals(ip) || localhostname.equals(ip)) && localport.equals(port))){
					System.out.println("added peer to peer list: " + ip + " with port " + port);
					Peer p = new Peer(ip, port, downloadFolder, peersFile);
					peers.add(p);
				}
			}
			
			br.close();			
		}catch(FileNotFoundException e){
			System.out.println("File could not be found!");
			System.exit(1);
		}catch(IOException e){
			System.out.println("An IOException has occurred.");	
			System.exit(1);
		}catch(NoSuchElementException e){
			System.out.println("Incorrectly formatted file");
			System.exit(1);
		}
    	return 0;
    }

	public Vector<Peer> getPeers() {
		return peers;
	}

}
