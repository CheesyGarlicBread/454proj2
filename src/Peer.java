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
	
	private ArrayList<String> localFiles = new ArrayList<String>();
	private ArrayList<FileElement> localList = new ArrayList<FileElement>();
	
	final byte CONNECTED = 0;
	final byte DISCONNECTED = 1;
	final byte UNKNOWN = 2;
	
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
		FileElement newElement = new FileElement(filename, file.length(), chunkSize, "rmi://"+this.getIp()+":"+this.getPort()+"/PeerService", 1, true);
		
		//Fill the block_complete array since the file is local and complete
		Arrays.fill(newElement.block_complete, true);
				
		//If localFiles vector already contains the filename, error out
		if ((localFiles.contains(filename)) || (localList.contains(newElement)))
		{
			System.out.println("ERROR: File already exists on local host");
			return;
		}
		
		//Add new filename into localFiles arraylist
		localFiles.add(filename);
		
		//Insert FileElement object into arraylist
		localList.add(newElement);
		
		if (state == CONNECTED){
			System.out.println("Notifying peers of update");
			//notify peers of update
			notifyPeersAdd(newElement);
		}
		
		System.out.println("New file " + filename + " has been inserted successfully.");
		
		
	}
	
	//event calls this
	public void removeFile(File file){
		//user has removed a local file		
		
		//remove it from peer list
		//(alternatively, we could just tell every other peer to remove just this file instead of updating peer list)
		
		//broadcast changes
	}
	
	//event calls this
	public void changeFile(File file){
		
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
					e.printStackTrace();
				} catch (NotBoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//RMI function call - Other peers update their files
				if(newpeer.getState() == CONNECTED)
					newpeer.fileAdded(file);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e){
				e.printStackTrace();
			}
		}
	}

	@Override
	//server method
	public void fileAdded(FileElement file) throws RemoteException {
		downloadFile(file);
		
	}
	
	//client will call this dto download a file
	private int downloadFile(FileElement file)
	{
		System.out.println("downloadFile() from " + file.currentServer);
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
				filebuffer = downloadFileChunk(file, i, chunkSize, targethost.currentServer);
				
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
		//System.out.println("Finished downloadFile()");
		return 0;
	}
	
	//downloadFile calls this
	private byte[] downloadFileChunk(FileElement file, int chunkID, int chunkSize, String server)
	{
		System.out.println("downloadFileChunk");
		byte[] filebuffer = null;
		
		try
		{
			//Connect to remote host
			PeerInterface newpeer = (PeerInterface)Naming.lookup(server);
			if(newpeer.getState() == CONNECTED){
				//	Chunk buffer for downloaded data
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
		
		System.out.println("Upload requested");
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
			
			input.close();
			//Return byte array to caller
			return (buffer);
			
		} catch(Exception e){
			//System.out.println("FileImpl: "+e);
		}
		return null;
	}
	
	
}