import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

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
  private HashMap<File, Long>     files_;       // File -> Long
  private HashMap<File, Long>	  directories_;
  private Collection<FileListener>  listeners_;   // of WeakReference(FileListener)
  private long pollingInterval;
  private Peer localPeer;

  /**
   * Create a file monitor instance with specified polling interval.
   * 
   * @param pollingInterval  Polling interval in milli seconds.
   */
  public FileMonitor (long pollingInterval, Peer localPeer)
  {
    files_     = new HashMap<File, Long>();
    directories_ = new HashMap<File, Long>();
    listeners_ = new ArrayList<FileListener>();

    timer_ = new Timer (true);
    this.pollingInterval = pollingInterval;
    this.localPeer = localPeer;
    
  }

  public void start(){
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
      
      FileListener listener = (FileListener) i.next();
      if (listener == fileListener)
        return;
    }

    // Use WeakReference to avoid memory leak if this becomes the
    // sole reference to the object.
    listeners_.add (fileListener);
  }


  
  /**
   * Remove listener from this file monitor.
   * 
   * @param fileListener  Listener to remove.
   */
  public void removeListener (FileListener fileListener)
  {
    for (Iterator i = listeners_.iterator(); i.hasNext(); ) {
     
      FileListener listener = (FileListener) i.next();
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

      Collection<File> directories = new ArrayList (directories_.keySet());
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
      
      Collection<File> files = new ArrayList<File> (files_.keySet());
      
      for (Iterator i = files.iterator(); i.hasNext(); ) {
        File file = (File) i.next();
        
        
        long lastModifiedTime = ((Long) files_.get (file)).longValue();
        long newModifiedTime  = file.exists() ? file.lastModified() : -1;

        // Check if file has changed
        if (newModifiedTime != lastModifiedTime || newModifiedTime == -1 || lastModifiedTime == -2) {
        	System.out.println("File has been changed");
          // Register new modified time        
          files_.put (file, new Long (newModifiedTime));
          
          //remove file from list
          if(newModifiedTime == -1){
        	  files_.remove(file);
          }

          // Notify listeners
          for (Iterator j = listeners_.iterator(); j.hasNext(); ) {
        	 System.out.println("Iterating through listeners");
            
            FileListener listener = (FileListener) j.next();

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
      
      saveHashMaps();
    }
    
    
  }
  
  public void saveHashMaps(){
		
		try {
			
			FileWriter fstream = new FileWriter("files.hckc");
			BufferedWriter out = new BufferedWriter(fstream);
			Collection<File> files = new ArrayList (files_.keySet());
		    for (Iterator i = files.iterator(); i.hasNext(); ) {
		    	File file = (File) i.next();
		    	int version = 0;
		    	if(!localPeer.getFiles().isEmpty()){		    				    		
		    		version = localPeer.getFiles().get(localPeer.getFiles().indexOf(new FileElement(file.getName()))).version;
		    	}
		    	out.write(file.getAbsolutePath() + "|" + files_.get(file) + ">"+version+"\n");
		    }	
			out.close();
			
			FileWriter fstream2 = new FileWriter("folders.hckc");
			BufferedWriter out2 = new BufferedWriter(fstream2);
			Collection<File> directories = new ArrayList (directories_.keySet());
		    for (Iterator i = directories.iterator(); i.hasNext(); ) {
		    	File directory = (File) i.next();
		    	out2.write(directory.getAbsolutePath() + "|" + directories_.get(directory) + "\n");
		    }	
			out2.close();
			
			
			System.out.println(files_);
			System.out.println(directories_);
			System.out.println(localPeer.getFiles());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
  }
  
  public void restoreHashMaps(){
		
		try {
			FileInputStream fstream = new FileInputStream("files.hckc");
			  // Get the object of DataInputStream
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  //Read File Line By Line
			  while ((strLine = br.readLine()) != null)   {
			  // Print the content on the console
				  File f = new File(strLine.substring(0, strLine.indexOf("|")));
				  Long l = Long.parseLong(strLine.substring(strLine.indexOf("|") + 1,strLine.indexOf(">")));				  
				//  System.out.println(strLine.substring(strLine.indexOf("|") + 1,strLine.length()));
				//  System.out.println(l);
				  files_.put(f,l);
			  }
			  //Close the input stream
			  in.close();
			
			  FileInputStream fstream1 = new FileInputStream("folders.hckc");
			  // Get the object of DataInputStream
			  DataInputStream in1 = new DataInputStream(fstream1);
			  BufferedReader br1 = new BufferedReader(new InputStreamReader(in1));
			  String strLine1;
			  //Read File Line By Line
			  while ((strLine1 = br1.readLine()) != null)   {
			  // Print the content on the console
				  File f = new File(strLine1.substring(0, strLine1.indexOf("|")));
				  Long l =  Long.parseLong(strLine1.substring(strLine1.indexOf("|") + 1,strLine1.length()));
				//  System.out.println(l);
				  directories_.put(f,l);
			  }
			  //Close the input stream
			  in1.close();
			
			System.out.println(files_);
			System.out.println(directories_);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
  }
}

