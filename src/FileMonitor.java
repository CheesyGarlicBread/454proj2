import java.util.*;
import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Class for monitoring changes in disk files.
 * Usage:
 *
 *    1. Implement the FileListener interface.
 *    2. Create a FileMonitor instance.
 *    3. Add the file(s)/directory(ies) to listen for.
 *
 * fileChanged() will be called when a monitored file is created,
 * deleted or its modified time changes.
 *
 * @author <a href="mailto:info@geosoft.no">GeoSoft</a>
 */   
public class FileMonitor
{
  private Timer       timer_;
  private HashMap     files_;       // File -> Long
  private HashMap	  directories_;
  private Collection  listeners_;   // of WeakReference(FileListener)
   

  /**
   * Create a file monitor instance with specified polling interval.
   * 
   * @param pollingInterval  Polling interval in milli seconds.
   */
  public FileMonitor (long pollingInterval)
  {
    files_     = new HashMap();
    directories_ = new HashMap();
    listeners_ = new ArrayList();

    timer_ = new Timer (true);
    timer_.schedule (new FileMonitorNotifier(), 0, pollingInterval);
  }


  
  /**
   * Stop the file monitor polling.
   */
  public void stop()
  {
    timer_.cancel();
  }
  

  /**
   * Add file to listen for. File may be any java.io.File (including a
   * directory) and may well be a non-existing file in the case where the
   * creating of the file is to be trapped.
   * <p>
   * More than one file can be listened for. When the specified file is
   * created, modified or deleted, listeners are notified.
   * 
   * @param file  File to listen for.
   */
  public void addFile (File file)
  {
    if (!files_.containsKey (file)) {
      //long modifiedTime = file.exists() ? file.lastModified() : -1;
    	long modifiedTime = -2;
    	files_.put (file, new Long (modifiedTime));
    	
    }
  }
  
  /*
   * Adds a directory to listen for
   */
  public void addDirectory(File directory){
	  //listen to folder (for additions)
	  if (!directories_.containsKey (directory)) {
	      long modifiedTime = directory.exists() ? directory.lastModified() : -1;
	      directories_.put (directory, new Long (modifiedTime));
	  }
	  addFilesFromDirectory(directory);
	  
  }
  
  private void addFilesFromDirectory(File directory){
	  //listen to files in folder
	  File[] listOfFiles = directory.listFiles();
	  for (int i = 0; i < listOfFiles.length; i++) {
		  if (listOfFiles[i].isFile()) {			  
			  addFile(listOfFiles[i]);
		  }
	  }	 
	  
  }
  

  /**
   * Remove specified file for listening.
   * 
   * @param file  File to remove.
   */
  public void removeFile (File file)
  {
    files_.remove (file);
  }
  
  public void removeDirectory(File file)
  {
	  directories_.remove(file);
  }


  
  /**
   * Add listener to this file monitor.
   * 
   * @param fileListener  Listener to add.
   */
  public void addListener (FileListener fileListener)
  {
    // Don't add if its already there
    for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
      WeakReference reference = (WeakReference) i.next();
      FileListener listener = (FileListener) reference.get();
      if (listener == fileListener)
        return;
    }

    // Use WeakReference to avoid memory leak if this becomes the
    // sole reference to the object.
    listeners_.add (new WeakReference (fileListener));
  }


  
  /**
   * Remove listener from this file monitor.
   * 
   * @param fileListener  Listener to remove.
   */
  public void removeListener (FileListener fileListener)
  {
    for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
      WeakReference reference = (WeakReference) i.next();
      FileListener listener = (FileListener) reference.get();
      if (listener == fileListener) {
        i.remove();
        break;
      }
    }
  }


  
  /**
   * This is the timer thread which is executed every n milliseconds
   * according to the setting of the file monitor. It investigates the
   * file in question and notify listeners if changed.
   */
  private class FileMonitorNotifier extends TimerTask
  {
    public void run()
    {
      // Loop over the registered files and see which have changed.
      // Use a copy of the list in case listener wants to alter the
      // list within its fileChanged method.

      Collection directories = new ArrayList (directories_.keySet());
      for (Iterator i = directories.iterator(); i.hasNext(); ) {
          File directory = (File) i.next();
          
          
          long lastModifiedTime = ((Long) directories_.get (directory)).longValue();
          long newModifiedTime  = directory.exists() ? directory.lastModified() : -1;

          // Check if file has changed
          if (newModifiedTime != lastModifiedTime) {
            // Register new modified time
            directories_.put (directory, new Long (newModifiedTime));
            addFilesFromDirectory(directory);
          }
      }
      
      Collection files = new ArrayList (files_.keySet());
      
      for (Iterator i = files.iterator(); i.hasNext(); ) {
        File file = (File) i.next();
        
        
        long lastModifiedTime = ((Long) files_.get (file)).longValue();
        long newModifiedTime  = file.exists() ? file.lastModified() : -1;

        // Check if file has changed
        if (newModifiedTime != lastModifiedTime) {

          // Register new modified time
          files_.put (file, new Long (newModifiedTime));

          // Notify listeners
          for (Iterator j = listeners_.iterator(); j.hasNext(); ) {
            WeakReference reference = (WeakReference) j.next();
            FileListener listener = (FileListener) reference.get();

            // Remove from list if the back-end object has been GC'd
            if (listener == null){
              j.remove();
            }else{              
              if(newModifiedTime == -1){
            	  listener.fileRemoved(file);
              }else if(lastModifiedTime == -2){            	  
            	  listener.fileAdded(file);            
              }else{
            	  listener.fileChanged(file);
              }
            }
          }
        }
      }
    }
  }
}

