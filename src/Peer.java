import java.io.*;
import java.net.MalformedURLException;
import java.rmi.*;

import java.util.*;

public class Peer extends java.rmi.server.UnicastRemoteObject implements PeerInterface{
	
	private Peers peers;
    private Status status;    
	private String ip;
	private String port;
	private String downloadFolder;
	private byte state;
	final static int chunkSize = 65536;
	
	
	private ArrayList<FileElement> localList = new ArrayList<FileElement>();


	private Queue<FileElement> filesToProcess = new LinkedList<FileElement>();
	
	final byte CONNECTED = 0;
	final byte DISCONNECTED = 1;
	final byte UNKNOWN = 2;
	final byte FULLYSYNCED = 4;
	final byte SYNCING = 8;
	
	public Peer() throws java.rmi.RemoteException {
		this("localhost","10042", "");
	}
	
	public Peer(String ip, String port,String downloadFolder) throws java.rmi.RemoteException {
		super();
		
		this.status = new Status();
		this.peers = new Peers();		
		this.ip = ip;
		this.port = port;
		this.state = CONNECTED;
		this.downloadFolder = downloadFolder;
	}
	
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}
	
	public Peers getPeers() {
		return peers;
	}

	public void setPeers(Peers peers) {
		this.peers = peers;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}
	
	public byte getState() {
		return state;
	}
	
	//event calls this
	public void addFile(File file){
		//TODO: 		
		/* Look into ways of telling other peers that new file has been added by this peer
		 * instead of having to send the peer list every time
		 */
		
		//user has added a local file		
		String filename = file.getName();
		if (!file.exists())
		{
			System.out.println("ERROR: File does not exist!");
			return;
		}                                       
		
		//Create an instance of FileElement class to store the attributes for the new file
		FileElement newElement = new FileElement(filename, file.length(), chunkSize, "rmi://"+this.getIp()+":"+this.getPort()+"/PeerService", false, true,0);
		
		//Fill the block_complete array since the file is local and complete
		Arrays.fill(newElement.block_complete, true);
				
		//If localFiles vector already contains the filename, error out
		if ((localList.contains(newElement)))
		{
			System.out.println("ERROR: File already exists on local host addFile");
			return;
		}
		
		//Insert FileElement object into arraylist
		localList.add(newElement);
		
		if (state != DISCONNECTED){
			System.out.println("Notifying peers of update");
			//notify peers of update
			notifyPeersAdd(newElement);
		}
		
		//System.out.println("New file " + filename + " has been inserted successfully.");
		
		
	}
	
	//event calls this
	public void removeFile(File file){
		//user has removed a local file		
		
		//remove it from peer list
		FileElement fe = null;
		
		for(int i = 0; i < localList.size(); i ++){
			if(localList.get(i).filename.equals(file.getName())){
				fe = localList.get(i);
				localList.remove(i);
			}
		}
		//(alternatively, we could just tell every other peer to remove just this file instead of updating peer list)
		
		//broadcast changes
		if (state != DISCONNECTED){
			System.out.println("Notifying peers of update");
			//notify peers of update
			if(fe != null)
				notifyPeersRemoved(fe);
			else
				System.out.println("No local file to remove");
		}
	}
	
	public void connected(){
		notifyPeersConnected();
		this.state = FULLYSYNCED;
	}
	
	private void notifyPeersConnected(){
		ArrayList<Peer> peerList = peers.getPeers();
		
		//Connect to each peer in peerList
		for (int i = 0; i < peerList.size(); i++)
		{
			Peer p = peerList.get(i);
			
			if(p.getIp().equals(this.getIp()) && p.getPort().equals(this.getPort()) ) break;
			
			
			try {
				//Connect to remote host
				PeerInterface newpeer = null;
				try {
					newpeer = (PeerInterface)Naming.lookup("rmi://"+p.getIp()+":"+p.getPort()+"/PeerService");
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				
				//RMI function call - Other peers update their files
				ArrayList<FileElement> remoteList = newpeer.getFiles();
				System.out.println("Remote list: " + remoteList);
				System.out.println("Getting  " + p.getIp());											
				//get files that i dont have
				for(int j = 0; j < remoteList.size(); j++){
					//downloadFile
					if(!localList.contains(remoteList.get(j))){
						fileAdded(remoteList.get(j));
					}							
				}														
				
				//tell others to get files they dont have
				for(int j = 0; j < localList.size(); j++){
					//downloadFile
					if(!remoteList.contains(localList.get(j))){
						newpeer.fileAdded(localList.get(j));
					}							
				}
			} catch (RemoteException e) {
				//e.printStackTrace();
			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
	}
	
	private void notifyPeersRemoved(FileElement file){
		ArrayList<Peer> peerList = peers.getPeers();
		
		//Connect to each peer in peerList
		for (int i = 0; i < peerList.size(); i++)
		{
			Peer p = peerList.get(i);
			
			if(p.getIp().equals(this.getIp()) && p.getPort().equals(this.getPort()) ) break;
			
			System.out.println("Telling peer " + p.getIp());
			try {
				//Connect to remote host
				PeerInterface newpeer = null;
				try {
					newpeer = (PeerInterface)Naming.lookup("rmi://"+p.getIp()+":"+p.getPort()+"/PeerService");
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				//RMI function call - Other peers update their files
				if(newpeer.getState() != DISCONNECTED)
					newpeer.fileRemoved(file);
			} catch (RemoteException e) {
				//e.printStackTrace();
			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
	}
	
	private void notifyPeersChanged(FileElement file){
		ArrayList<Peer> peerList = peers.getPeers();
		
		//Connect to each peer in peerList
		for (int i = 0; i < peerList.size(); i++)
		{
			Peer p = peerList.get(i);
			
			if(p.getIp().equals(this.getIp()) && p.getPort().equals(this.getPort()) ) break;
			
			System.out.println("Telling peer " + p.getIp());
			try {
				//Connect to remote host
				PeerInterface newpeer = null;
				try {
					newpeer = (PeerInterface)Naming.lookup("rmi://"+p.getIp()+":"+p.getPort()+"/PeerService");
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				//RMI function call - Other peers update their files
				if((newpeer.getState() != DISCONNECTED) && (file.changedRemotely == false))
					newpeer.fileChanged(file);
			} catch (RemoteException e) {
				//e.printStackTrace();
			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
	}
	
	//client method
	private void notifyPeersAdd(FileElement file){
		ArrayList<Peer> peerList = peers.getPeers();
		
		//Connect to each peer in peerList
		for (int i = 0; i < peerList.size(); i++)
		{
			Peer p = peerList.get(i);
			
			if(p.getIp().equals(this.getIp()) && p.getPort().equals(this.getPort()) ) break;
			
			System.out.println("Telling peer " + p.getIp());
			try {
				//Connect to remote host
				PeerInterface newpeer = null;
				try {
					newpeer = (PeerInterface)Naming.lookup("rmi://"+p.getIp()+":"+p.getPort()+"/PeerService");
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				//RMI function call - Other peers update their files
				if(newpeer.getState() != DISCONNECTED)
					newpeer.fileAdded(file);
			} catch (RemoteException e) {
				//e.printStackTrace();
			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
	}
	
	@Override
	//server method
	public void fileAdded(FileElement file) throws RemoteException {		
		if(filesToProcess.isEmpty())
			this.state = SYNCING;
		
		filesToProcess.add(file);
		
		downloadFile(file);
		filesToProcess.remove();
		
		if(filesToProcess.isEmpty())
			this.state = FULLYSYNCED;
		
		//Create an instance of FileElement class to store the attributes for the new file
		FileElement newElement = new FileElement(file.filename, file.filesize, chunkSize, "rmi://"+this.getIp()+":"+this.getPort()+"/PeerService", false, true,0);
		
		//Fill the block_complete array since the file is local and complete
		Arrays.fill(newElement.block_complete, true);
				
		//If localFiles vector already contains the filename, error out
		if ((localList.contains(newElement)))
		{
			System.out.println("ERROR: File already exists on local host fileAdded");
			return;
		}
		
		//Insert FileElement object into arraylist
		localList.add(newElement);
	}
	
	@Override
	//server method
	public void fileRemoved(FileElement file) throws RemoteException {
		//remove local copy
		localList.remove(file);
		
		if(filesToProcess.isEmpty())
			this.state = SYNCING;
		
		filesToProcess.add(file);	
		
		removeLocalFile(file);
		
		filesToProcess.remove();
		
		if(filesToProcess.isEmpty())
			this.state = FULLYSYNCED;
		
		
	}
	
	@Override
	//REMOTE functional call
	//File remotely has been changed, update local copy if necessary
	public void fileChanged(FileElement remotefile) throws RemoteException
	{	
		
		File localFile = new File(downloadFolder + remotefile.filename);
		
		FileElement localFileElement = localList.get(localList.indexOf(remotefile));
		System.out.println("Local File version: "+ localFileElement.version + " & remote file version: " + remotefile.version);
		//Local file dirty bit not set, so can just replace the local file.
		if ((localFileElement.version < remotefile.version))
		{
			System.out.println("RMI: Remote change signaled, local file HAS NOT been changed");
			
			
			//Remove the file from the filesystem
			boolean delsuccess = localFile.delete();
			
			if (delsuccess == false)
			{
				System.out.println("Failed to replace local file with updated version");
			}
			
			//Re-download the file from the host
			if(filesToProcess.isEmpty())
				this.state = SYNCING;
			
			filesToProcess.add(remotefile);
			
			downloadFile(remotefile);
			filesToProcess.remove();
			
			if(filesToProcess.isEmpty())
				this.state = FULLYSYNCED;
			
			//Flag that indicates the file was updated by a remote host
			//localFileElement.version = remotefile.version;
			localFileElement.changedRemotely = true;
		}
		else if (localFileElement.version == remotefile.version)
		{
			System.out.println("RMI: Remote change signaled, local file HAS been changed");
			
			File newfile = new File(downloadFolder + remotefile.filename + ".conflict" + localFile.lastModified());
			localFile.renameTo(newfile);
			
			//Re-download the file from the host
			if(filesToProcess.isEmpty())
				this.state = SYNCING;
			
			filesToProcess.add(remotefile);
			
			downloadFile(remotefile);
			filesToProcess.remove();
			
			if(filesToProcess.isEmpty())
				this.state = FULLYSYNCED;
			
		}
		else
		{
			System.out.println("RMI: Remote change signaled, local file HAS NOT be changed, and file was NOT changed remotely");
			localFileElement.changed = false;
			localFileElement.changedRemotely = false;
		}
	}
	
	//LOCAL file change
	public void changeFile(File file)
	{
		String filename = file.getName();
		
		FileElement fe = null;
		
		for(int i = 0; i < localList.size(); i ++){
			if(localList.get(i).filename.equals(filename)){
				fe = localList.get(i);
			}
		}

		if(!filesToProcess.contains(fe)){
			System.out.println("file has been changed!!!!!!!!!");
			if(fe != null){
				fe.changed = true;
				fe.version++;
				notifyPeersChanged(fe);
				fe.changed = false;
			}
		}
		if (fe != null) fe.changedRemotely = false;
	}
	
	private int removeLocalFile(FileElement file)
	{
		//Why is this done twice?
		localList.remove(file);
			
		System.out.println("Removing local file" + file.filename);
		 
		    // A File object to represent the filename
		    File f = new File(downloadFolder + file.filename);

	    // Make sure the file or directory exists and isn't write protected
	    if (!f.exists())
	    	System.out.println("File does not exist to delete.");

	    if (!f.canWrite())
	      System.out.println("Cant delete, write protected");

	    // If it is a directory, make sure it is empty
	    if (f.isDirectory()) {
	      String[] files = f.list();
	      if (files.length > 0)
	        System.out.println("Directory is not empty");
	    }

	    // Attempt to delete it
	    boolean success = f.delete();
	    


		    if (!success)
		      System.out.println("Deletion failed");
		    
		    return 1;

	}
	
	//client will call this to download a file
	private int downloadFile(FileElement file)
	{
		
		//System.out.println("downloadFile() from " + file.currentServer);
		//if(localList.contains(file)) return 1;
		
		//RandomAccessFile to write chunks to
		File newfile = new File(file.filename);
		//System.out.println("saving file to " + file.filename);
		RandomAccessFile output = null;
		
		try {
			output = new RandomAccessFile(newfile, "rw");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		//Chunk buffer for downloaded data
		byte[] filebuffer = null;
		//FileElement targethost = null;
		FileElement targethost = file;
		//Find a server that has this particular chunk
		/*for (FileElement e : file.remoteList)
		{
			if (e.filecomplete == true)
			{
				targethost = e;
			}
		}*/
		
		
		//For each chunk of the file
		for (int i = 0; i < file.block_complete.length; i++)
		{
//			if (file.block_complete[i] == false)
//			{
			//System.out.println(targethost.currentServer);
				filebuffer = downloadFileChunk(file, i, chunkSize, targethost.currentServer);
				//System.out.println("FileBuffer size: " + filebuffer.length);
				try {
					output.seek(i*chunkSize);
					output.write(filebuffer);
					file.block_complete[i] = true;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
//			}
		}
		
		try {
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
			
		}
		newfile.renameTo(new File(downloadFolder + file.filename));
		//System.out.println("Finished downloadFile()");
		return 0;
	}
	
	//downloadFile calls this
	private byte[] downloadFileChunk(FileElement file, int chunkID, int chunkSize, String server)
	{
		//System.out.println("downloadFileChunk");
		byte[] filebuffer = null;
		
		try
		{
			//Connect to remote host
			PeerInterface newpeer = (PeerInterface)Naming.lookup(server);
			if(newpeer.getState() != DISCONNECTED){
				//	Chunk buffer for downloaded data
				//System.out.println("TRYING TO GET FILE BUFFER");
				filebuffer = newpeer.uploadFileChunk(file.filename, chunkID*chunkSize, chunkSize);
				
			}
		
		}catch(RemoteException e){
			System.out.println(e);
		}catch(MalformedURLException e){
			System.out.println(e);
		}catch(NotBoundException e){
			System.out.println(e);
		}catch (IOException e){
			System.out.println(e);
		}
		
		//Return byte array of size 'chunkSize'
		return filebuffer;
	}
	
	//server receives a download request and uploads the chunk to that
	public byte[] uploadFileChunk(String filename, int offset, int length)
	{
		
		//System.out.println("Upload requested");
		try
		{
			//Create a byte buffer of size: 
			File file = new File(downloadFolder + filename);
			//System.out.println("Uploading file: " + file.getAbsolutePath());
			byte buffer[] = null;
			RandomAccessFile input = new RandomAccessFile(file,"r");
			input.seek(offset);
			if ((offset+length) > file.length()){
				 buffer = new byte[(int)(file.length()-offset)];
				input.readFully(buffer,0,(int)(file.length()-offset));
			}
			else
			{
				buffer = new byte[length];
				input.readFully(buffer, 0, length);
			}
			
			input.close();
			//Return byte array to caller
			return (buffer);
			
		} catch(Exception e){
			System.out.println("error in uploadFileChunk: "+e.getMessage());
		}
		return null;
	}	

	@Override
	public ArrayList<FileElement> getFiles() throws RemoteException {
		// TODO Auto-generated method stub
		return localList;
	}
	
	
	//API functions
}