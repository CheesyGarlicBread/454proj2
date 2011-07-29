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
	//public ArrayList<FileElement> remoteList = new ArrayList<FileElement>();
	public boolean changed;
	public boolean changedRemotely;
	
	public int version = 0;
	public FileElement(String filename) {
		this(filename,1,1,null,false,false,0);
	}
	public FileElement(String filename, long length, int chunkSize, String server, boolean changed, boolean complete, int version) {
		this.filename = filename;
		this.filesize = length;
		this.block_complete = new boolean[(int) (Math.ceil(length / chunkSize) + 1)];
		this.block_available = new int[(int) (Math.ceil(length / chunkSize) + 1)];

		this.currentServer = server;
		//this.remoteList = null;
		
		this.changed = changed;
		this.filecomplete = complete;
		this.changedRemotely = false;
		this.version = version;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub	
		if(((FileElement)obj).filename.equals(this.filename))
			return true;
		else
			return false;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return filename + "v" + version;
	}
	
	
}
