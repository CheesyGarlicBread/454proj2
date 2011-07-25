import java.io.Serializable;
import java.util.ArrayList;

public class FileElement implements Serializable
{
	private static final long serialVersionUID = 1L;
	public String filename;
	public long filesize;
	public boolean[] block_complete;
	public boolean filecomplete;
	public int[] block_available;
	public String currentServer;
	public ArrayList<FileElement> remoteList = new ArrayList<FileElement>();
	public int version;
	
	
	public FileElement(String filename, long length, int chunkSize, String server, int version, boolean complete) {
		this.filename = filename;
		this.filesize = length;
		this.block_complete = new boolean[(int) (Math.ceil(length / chunkSize) + 1)];
		this.block_available = new int[(int) (Math.ceil(length / chunkSize) + 1)];

		this.currentServer = server;
		this.remoteList = null;
		
		this.version = version;
		this.filecomplete = complete;
	}
}
