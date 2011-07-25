import java.io.File;


public class FileListenerImpl implements FileListener{
	private Peer localPeer;
	public FileListenerImpl(Peer peer){
		this.localPeer = peer;
	}
	@Override
	public void fileChanged(File file) {
		// TODO Auto-generated method stub
		System.out.println ("File changed: " + file.getAbsolutePath());
		localPeer.changeFile(file);
	}
	
	public void fileAdded(File file){
		System.out.println("HALLELUJIAH");
		System.out.println("File added: " + file.getAbsolutePath());
		localPeer.addFile(file);
	}
	
	public void fileRemoved(File file){
		System.out.println("File removed: " + file.getAbsolutePath());
		localPeer.removeFile(file);
	}

}
