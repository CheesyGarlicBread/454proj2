import java.io.*;
import java.net.MalformedURLException;
import java.rmi.*;

import java.util.*;

public class Peer extends java.rmi.server.UnicastRemoteObject implements PeerInterface{
	
	// Return codes
	// This is a defined set of return codes.
	// Using enum here is a bad idea, unless you spell out the values, since it is
	// shared with code you don't write.
	final int errOK             =  0; // Everything good
	final int errUnknownWarning =  1; // Unknown warning
	final int errUnknownFatal   = -2; // Unknown error
	final int errCannotConnect  = -3; // Cannot connect to anything; fatal error
	final int errNoPeersFound   = -4; // Cannot find any peer (e.g., no peers in a peer file); fatal
	final int errPeerNotFound   =  5; // Cannot find some peer; warning, since others may be connectable
	
	//final static int chunkSize = 65536;
	final static int chunkSize = 65536;
	final static int maxPeers = 6;
	final static int maxFiles = 100;
	
    private Peers peers;
    private Status status;    
	private String ip;
	private String port;
	private String downloadFolder;
	private Vector<String> localFiles = new Vector<String>();
	private boolean isDownloading = false;
	
	private int isUploading = 0;
	
	private LinkedList<FileElement> localList = new LinkedList<FileElement>();
	
	final byte CONNECTED = 0;
	final byte DISCONNECTED = 2;
	final byte UNKNOWN = 4;
	
	private byte state; 
	  
	public Peer() throws java.rmi.RemoteException {
		this("localhost","10042", "");
	}
	
	public Peer(String ip, String port,String downloadFolder) throws java.rmi.RemoteException {
		super();
		this.status = new Status();
		this.peers = new Peers();		
		this.ip = ip;
		this.port = port;
		this.state = DISCONNECTED;
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
	
	public int insert(String filename)
	{	
		File file = new File(filename);
		filename = file.getName();
		if (!file.exists())
		{
			System.out.println("ERROR: File does not exist!");
			return -1;
		}
		
		//Create an instance of FileElement class to store the attributes for the new file
		FileElement newElement = new FileElement(filename, file.length(), chunkSize, "rmi://"+this.getIp()+":"+this.getPort()+"/PeerService");
		
		//Fill the block_complete array since the file is local and complete
		Arrays.fill(newElement.block_complete, true);
		
		//If localFiles vector already contains the filename, error out
		if ((localFiles.contains(filename)) || (localList.contains(newElement)))
		{
			System.out.println("ERROR: File already exists on local host");
			return 1;
		}
		
		//Add new filename into localFiles vector
		localFiles.add(filename);
		
		//Insert FileElelment object into linkedlist
		localList.add(newElement);
		
		if (state == CONNECTED){
			System.out.println("Notifying peers of update");
			notifyPeersFileUpdate();
		}
		
		System.out.println("New file " + filename + " has been inserted successfully.");
		
		return 0;
	}
	
	private void notifyPeersFileUpdate()
	{
		//Notify all other peers in peerlist that a new file has been added
		//List of existings peers
		
		Vector<Peer> peerList = peers.getPeers();
		
		
		for (int i = 0; i < peerList.size(); i++)
		{
			Peer p = peerList.get(i);
			try {
				//Connect to remote host
				PeerInterface newpeer = null;
				try {
					newpeer = (PeerInterface)Naming.lookup("rmi://"+p.getIp()+":"+p.getPort()+"/PeerService");
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//RMI function call - Other peers update their files
				if(newpeer.getState() == CONNECTED)
					newpeer.updateFileList();
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
	}
	
	private void downloadFiles()
	{
		if(isDownloading)
		{
			return;
		}
		//If there is at least one missing chunk from the file 'e', attempt to download file 'e'
		for (int j = 0; j < localList.size(); j++)
		{
			FileElement e = localList.get(j);
			e.remoteList = searchPeersForFile(e.filename);
			
			//System.out.println("Checking " + e.filename + " file for local completeness");
			for (int i = 0; i < e.block_complete.length; i++)
			{
				if (e.block_complete[i] == false)
				{
					//System.out.println(e.currentServer);
					isDownloading = true;
					downloadFile(e);
					isDownloading = false;
					break;
				}
			}
		}
	}

	private int downloadFile(FileElement file)
	{
		//System.out.println("downloadFile()");
		//RandomAccessFile to write chunks to
		File newfile = new File(downloadFolder + file.filename);
		//System.out.println("saving file to " + file.filename);
		RandomAccessFile output = null;
		
		try {
			output = new RandomAccessFile(newfile, "rw");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		//Chunk buffer for downloaded data
		byte[] filebuffer = null;
		
		int lowestnum = 1;
		
		//Number of copies available can range between 0 and 6
		for (int j = 0; j <= maxPeers; j++)
		{
			//For each chunk of the file
			for (int i = 0; i < file.block_available.length; i++)
			{
				//Download chunk if the chunk has a low availability and is not complete locally
				if((file.block_available[i] == lowestnum) && (file.block_complete[i] == false))
				{
					FileElement minNumUploadsFileElement = null;
					
					int minNumUploads = 1000;
					//Find a server that has this particular chunk
					for (FileElement e : file.remoteList)
					{
						if (e.block_complete[i] == true)
						{
							try
							{
								//Connect to peer to determine how many files it is currently uploading
								PeerInterface peer = (PeerInterface)Naming.lookup(e.currentServer);
								int numUploads  = peer.queryUploadNumber();
								
								//Store the peer who is currently uploading the fewest number of file chunks
								if (numUploads < minNumUploads)
								{
									minNumUploads = numUploads;
									minNumUploadsFileElement = e;
								}
								
							}catch(RemoteException f){
								System.out.println(f);
							}catch(MalformedURLException f){
								System.out.println(f);
							}catch(NotBoundException f){
								System.out.println(f);
							}
						}
					}

					

					//System.out.println("Downloading file from: " + e.currentServer);
					//System.out.println("writing to " + i*chunkSize);
					
					//Download this chunk from minNumUploadsFileElement
					filebuffer = downloadFileChunk(file, i, chunkSize, minNumUploadsFileElement.currentServer);
					file.block_complete[i] = true;
					
					//System.out.println(filebuffer);
					//System.out.println("Downloading Chunk: " + i);
					
					try {
						output.seek(i*chunkSize);
						output.write(filebuffer);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			//Get the next least available chunk
			lowestnum++;
		}
		
		
		try {
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.println("Finished downloadFile()");
		return 0;
	}

	//Returns a link list of the FileElements from all peers
	private LinkedList<FileElement> searchPeersForFile(String filename)
	{
		//System.out.println("searchPeersForFile()");
		LinkedList<FileElement> remoteList = new LinkedList<FileElement>();
		
		//List of existings peers
		Vector<Peer> peerList = peers.getPeers();
		
		//Search through each peer in peerList
		
		for (int i = 0; i < peerList.size(); i++)
		{
			Peer p = peerList.get(i);
			try
			{
				PeerInterface peer = (PeerInterface)Naming.lookup("rmi://"+p.getIp()+":"+p.getPort()+"/PeerService");
				if(peer.getState() == CONNECTED){
					//Get each peer's local file list
					LinkedList<FileElement> tmpList = peer.returnList();
					
					//For each file in the remote file list, search for files with the same name as filename
					
					for(int j = 0; j < tmpList.size(); j++)
					{
						FileElement e = tmpList.get(j);
						if (e.filename.equals(filename))
						{
							//Add all files with matching filename to the new LinkedList object
							remoteList.add(e);
						}
					}
				}
			}catch(RemoteException e){
				System.out.println(e);
			}catch(MalformedURLException e){
				System.out.println(e);
			}catch(NotBoundException e){
				System.out.println(e);
			}
		}
		
		//Return a linked list object containing all of the FileElements for the matching filenames
		return remoteList;
	}
	
	private byte[] downloadFileChunk(FileElement file, int chunkID, int chunkSize, String server)
	{
		//System.out.println("downloadFileChunk");
		byte[] filebuffer = null;
		
		try
		{
			//Connect to remote host
			PeerInterface newpeer = (PeerInterface)Naming.lookup(server);
			if(newpeer.getState() == CONNECTED){
				//	Chunk buffer for downloaded data
				filebuffer = newpeer.uploadFileChunk(file.filename, chunkID*chunkSize, chunkSize);
				
				//Notify peer that file transfer is complete
				newpeer.uploadComplete();
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
	
	public byte[] uploadFileChunk(String filename, int offset, int length)
	{
		
		//System.out.println("Upload requested");
		try
		{
			//Create a byte buffer of size: 
			File file = new File(downloadFolder + filename);
			
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
			
			//Keep track of how many chunks are being uploaded at a time
			isUploading++;
			
			input.close();
			//Return byte array to caller
			return (buffer);
			
		} catch(Exception e){
			//System.out.println("FileImpl: "+e);
		}
		return null;
	}
	
	//TODO change source of filename size - This won't work when an incomplete file exists
	public int filesize(String filename)
	{
		//Return size of local filename
		File file = new File(downloadFolder + filename);
		return (int)file.length();
	}
	
	public int queryUploadNumber()
	{
		return isUploading;
	}
	
	public void uploadComplete()
	{
		isUploading--;
	}
	
	public int query(Status status)
	{
		try {
			updateFileList();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		//Store the localList LinkedList global into the Status class instance
		//This LinkedList stores all the onformation for all files locally
		status.setLocalList(localList);
		
		//Populate parameter status with details for each file
		//1. The fraction of the file that is available locally
		//2. The fraction of the file that is available in the system
		//3. The least replication level
		//4. The weighted least-replication level
		System.out.println("QUERY STATUS REQUEST");
		System.out.println("Number of Local Files: " + status.numberOfFiles());
		System.out.println("Number of Current Chunk Uploads: " + isUploading);
		
		System.out.println("File Details:");
		System.out.println("=========================================");
		
		for(int i = 0; i < localFiles.size(); i++)
		{
			String file = localFiles.get(i);
			System.out.println("Filename: " + file);
			System.out.println("Fraction of file available locally: " + status.fractionPresentLocally(file));
			System.out.println("Fraction of file available in system: " + status.fractionPresent(file));
			System.out.println("Least Replication Level: " + status.minimumReplicationLevel(file));
			System.out.println("Average Replication Level: " + status.averageReplicationLevel(file));
			System.out.println("=========================================");
		}
		
		return 0;
	}
	
	public LinkedList<FileElement> returnList()
	{
		return localList;
	}
	
	public int join()
	{
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					updateFileList();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
		
		//set state to connected
		this.state = CONNECTED;
		
		//Notify all peers to check for new files
		notifyPeersFileUpdate();
		
		
		return 0;
	}
	
	private void updateCurrentFileFrames()
	{
		
		for (int i = 0; i < localList.size(); i++ )
		{
			FileElement e = localList.get(i);
			//Find servers that have this file
			e.remoteList = searchPeersForFile(e.filename);
			
			//Find availability of block on the network
			findBlockAvailability(e);
		}
	}
	
	private void getNewFileFrames(LinkedList<FileElement> tmpList)
	{
		//Files from new servers
		
		for (int i = 0; i < tmpList.size(); i++)
		{
			FileElement e = tmpList.get(i);
			if (!localFiles.contains(e.filename))
			{
				//Create an instance of FileElement class to store the attributes for the new file
				FileElement newElement = new FileElement(e.filename, e.filesize, chunkSize, "rmi://"+this.getIp()+":"+this.getPort()+"/PeerService");
				
				//Find servers that have this file
				newElement.remoteList = searchPeersForFile(newElement.filename);
				
				//Insert FileElement object into linkedlist and filename vector
				Arrays.fill(newElement.block_complete, false);
				Arrays.fill(newElement.block_available, 0);
				localList.add(newElement);
				localFiles.add(newElement.filename);
				findBlockAvailability(newElement);			
			}
		}
	}
	
	private void findBlockAvailability(FileElement newElement)
	{
		//Reset Block Count
		Arrays.fill(newElement.block_available, 0);
		
		for (int i = 0; i < newElement.block_available.length; i++)
		{
			//For each copy of the file on a remote peer
			
			for (int j = 0; j < newElement.remoteList.size(); j++)
			{
				FileElement f = newElement.remoteList.get(j);
				//Incriment block_availability on local file
				if(f.block_complete[i] == true)
				{
					newElement.block_available[i]++;
				}
			}
		}
	}
	
	public int leave()
	{
		//set state
		this.state = DISCONNECTED;
		
		notifyPeersFileUpdate();
		
		//let everyone know you're leaving
		
		
		//Leave set of peers
		//Close socket connections
		//Inform peers that it is leaving
		//Ideally, unique blocks are pushed before disconnection
		//Only push if the number of blocks is low
		return 0;
	}

	public void updateFileList() throws RemoteException
	{
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				//System.out.println("updateFileList()");
				Vector<Peer> peerList = peers.getPeers();
				try
				{
					//Search through all peers
					//Add all external file frames to localList and localFiles
					for(int i = 0; i < peerList.size(); i++)
					{
						Peer p = peerList.get(i);
						PeerInterface newpeer = null;
						try{
							newpeer = (PeerInterface)Naming.lookup("rmi://"+p.getIp()+":"+p.getPort()+"/PeerService");
							if(newpeer.getState() == CONNECTED){
								System.out.println("Updating IP: " + newpeer.getIp());
							
								LinkedList<FileElement> tmpList = newpeer.returnList();
								updateCurrentFileFrames();
								getNewFileFrames(tmpList);
								
							}
						}catch(RemoteException e){
							
						}
						
					}

					downloadFiles();
				
				}catch(MalformedURLException e){
					
				}catch(NotBoundException e){
					
				}
				
			}
		});
		t.start();
		
	}

}
