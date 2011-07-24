import java.rmi.*;
import java.util.LinkedList;


public interface PeerInterface extends java.rmi.Remote {
	public String getIp() throws RemoteException;
	public String getPort() throws RemoteException;
	public byte getState() throws RemoteException;
	public void updateFileList() throws RemoteException;
	//public byte[] downloadFile(String filename) throws RemoteException;
	//public byte[] downloadFile(String filename) throws RemoteException;
	public byte[] uploadFileChunk(String filename, int offset, int length) throws RemoteException;
	//public int filesize(String filename) throws RemoteException;
	public LinkedList<FileElement> returnList() throws RemoteException;
	//public int queryUploadNumber() throws RemoteException;
	//public void uploadComplete() throws RemoteException;
	/*
	public int open(String filename, char operation) throws RemoteException;
	public int close(String filename) throws RemoteException;
	public int read(String filename, char buf[], int offset, int bufsize) throws RemoteException;
	public int write(String filename, char buf[], int offset, int bufsize) throws RemoteException;
	*/
}
