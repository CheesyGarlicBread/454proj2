import java.io.File;
public interface FileListener
{
  
  void fileChanged (File file);
  void fileAdded(File file);
  void fileRemoved(File file);
}

