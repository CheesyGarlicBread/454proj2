import java.util.LinkedList;


public class Status
{
	final static int chunkSize = 65536;
	final static int maxPeers = 6;
	final static int maxFiles = 100;

	private LinkedList<FileElement> localList = new LinkedList<FileElement>();
	
	public LinkedList<FileElement> getLocalList() {
		return localList;
	}
	public void setLocalList(LinkedList<FileElement> localList) {
		this.localList = localList;
	}
	
	//Return number of files stored locally
	public int numberOfFiles()
	{
		return localList.size();
	}
	
	public double fractionPresentLocally(String filename)
	{
		int count = 0;
		int size = 0;
		for (FileElement e : localList)
		{
			if (e.filename.equals(filename))
			{
				size = e.block_complete.length;
				for (boolean i : e.block_complete)
				{
					if (i == true)
					{
						count++;
					}
				}
				break;
			}
		}
		double fraction = (double)count/(double)size;
		return fraction;
	}
	
	public double fractionPresent(String filename)
	{
		int count = 0;
		int size = 0;
		for (FileElement e : localList)
		{
			if (e.filename.equals(filename))
			{
				size = e.block_available.length;
				for (int i : e.block_available)
				{
					if (i > 0)
					{
						count++;
					}
				}
				break;
			}
		}
		return (double)count/(double)size;
	}
	
	public int minimumReplicationLevel(String filename)
	{
		int minVal = maxPeers;
		for (FileElement e : localList)
		{
			if (e.filename.equals(filename))
			{
				for (int i : e.block_available)
				{
					if (i < minVal)
					{
						minVal = i;
					}
				}
				break;
			}
		}
		return minVal;
	}
	
	public double averageReplicationLevel(String filename)
	{
		int sumVals = 0;
		int count = 0;
		for (FileElement e : localList)
		{
			if (e.filename.equals(filename))
			{
				for (int i : e.block_available)
				{
					sumVals += i;
					count++;
				}
				break;
			}
		}
		return (double)sumVals / (double)count;
	}
	
	public String toString()
	{
		String temp = "";
		return temp;
	}
	
}
