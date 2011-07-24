import java.io.File;


public class FileListenerImpl implements FileListener{

	@Override
	public void fileChanged(File file) {
		// TODO Auto-generated method stub
		System.out.println ("File changed: " + file.getAbsolutePath());
	}
	
	public void fileAdded(File file){
		System.out.println("File added: " + file.getAbsolutePath());
	}
	
	public void fileRemoved(File file){
		System.out.println("File removed: " + file.getAbsolutePath());
	}

}
