import java.rmi.*;
import java.util.ArrayList;


public interface PeerInterface extends java.rmi.Remote {
	public String getIp() throws RemoteException;
	public String getPort() throws RemoteException;
	public byte getState() throws RemoteException;
	
	public void fileAdded(FileElement file) throws RemoteException;
	public byte[] uploadFileChunk(String filename, int offset, int length) throws RemoteException;
	/*
	public int open(String filename, char operation) throws RemoteException;
	public int close(String filename) throws RemoteException;
	public int read(String filename, char buf[], int offset, int bufsize) throws RemoteException;
	public int write(String filename, char buf[], int offset, int bufsize) throws RemoteException;
	*/
	void fileRemoved(FileElement file) throws RemoteException;
	void fileChanged(FileElement file) throws RemoteException;
}
