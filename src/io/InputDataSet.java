package io;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Represents a set of files that can be iterated over. This is achieved
 * via the implementation of the Iterable interface, which provides access
 * to the InputStream objects associated with each of the files
 * @author rvasa
 */
public class InputDataSet implements Iterable<InputStream>
{
	//The set of files that are part of the input data set
	private Set<File> files = new HashSet<File>();
	//Flag indicating whether the current element being processed is an archive
	private boolean processingArchive = false;
	//The cumulative total file size of the input files 
    private long fileSize = 0;
    //The time at which the input data set was last modified
    public long lastModTime = -1;
    
    /**
     * Calculates the cumulative total file size of the files in the input data set
     * @return The cumulative total file size
     */
    public long sizeInBytes()
    {
        return fileSize;
    }
	
    /**
     * Adds a file to the input data set. The only files added to the set will be class files
     * and archives
     * @param file The file to add to the input data set 
     */
	public void addInputFile(File file)
	{
		//Return if no file specified
		if (file == null) return;
		
		//If the file is a class or archive
		if (FileUtil.isClassFile(file.toString()) || FileUtil.isArchive(file.toString()))
        {
			//TODO: Log file being added to input data set
			//Add the file to the input data set
			files.add(file);
			//Increase the cumulative total file size by the size of the file that was added
            fileSize += file.length();
        }
	}
	
	/**
	 * Adds a file corresponding to the specified filename to the input data set 
	 * @param filename The filename corresponding to the file to be added to the input data set
	 * @throws IOException - if a file handle could not be established for the given filename
	 */
	public void addInputFile(String filename) throws IOException
	{
		addInputFile(FileUtil.getFileHandle(filename));
	}
	
	/**
	 * Adds each of the files within the directory corresponding to the specified filename to the input data set
	 * @param filename The filename corresponding to the directory to be added to the input data set
	 * @param recursive Whether files should be added recursively
	 * @throws IOException - if the set of files could not be retrieved for the directory filename specified
	 */
	public void addInputDir(String filename, boolean recursive) throws IOException
	{
		Set<File> dirFiles = FileUtil.getFiles(filename, recursive);
		for (File f : dirFiles) addInputFile(f);
	}
	
	/**
	 * Calculates the size (i.e. no. of elements) within the input data set
	 * @return The total number of elements within the input data set
	 */
	public int size()
	{
		return files.size();
	}
	
	/**
	 * Creates an iterator allowing iteration over the files contained within the input data set
	 */
	public Iterator<InputStream> iterator()
	{		
		return new DataSetIterator();
	}
	
	/**
	 * Iterator implementation allowing iteration over a set of files, exposing their associated InputStream objects
	 * @author rvasa
	 */
	class DataSetIterator implements Iterator<InputStream>
	{
		File nextFile;
		ZipFile zipFile = null;
		ZipFileIterator archiveIterator = null;
		Iterator<File> fileSetIterator;
		
		public DataSetIterator()
		{
			fileSetIterator = files.iterator();
			if (!fileSetIterator.hasNext()) throw new IllegalArgumentException();
		}	
		
		/**
		 * Determines whether their is a next element. This is determine first by checking whether the archive
		 * iterator has a next element (if an archive is currently being processed) and then by checking whether
		 * the file iterator has a next element
		 */
		public boolean hasNext()
		{
			if (processingArchive && (archiveIterator != null) && (archiveIterator.hasNext())) return true;
			processingArchive = false;
			return fileSetIterator.hasNext();
		}
		
		/** 
		 * Returns the InputStream object for the next file to be iterated over
		 */
		public InputStream next()
		{
			InputStream inputStream = null;
			
			//If an archive is being processed
			if (processingArchive)
			{
				//Get the next files InputStream
				inputStream = archiveIterator.next();
				
				//If the archive has been modified later than the current latest modified time, set the last modified time
				//to that of the archive
				if (archiveIterator.lastModTime > lastModTime) lastModTime = archiveIterator.lastModTime;
				
				return inputStream;
			}

            try
            {
            	//Get the next files handle
    			nextFile = fileSetIterator.next();
    			
    			//If the file is an archive
    			if (FileUtil.isArchive(nextFile.toString()))
    			{
    				//Create a new zip file using the next files file handles
    				zipFile = new ZipFile(nextFile);
    				//Create a new ZipFileIterator for the archive
    				archiveIterator = new ZipFileIterator(zipFile);
    				
    				//Get the next input stream in the archive
    				inputStream = archiveIterator.next();
    				
    				//If the archive has been modified later than the current latest modified time, set the last modified time
    				//to that of the archive
    				if (archiveIterator.lastModTime > lastModTime) lastModTime = archiveIterator.lastModTime;
    				
    				//Flag that an archive is being processed
    				processingArchive = true;
    			}
    			else
    			{
    				//Get the next files InputStream
    				inputStream = new BufferedInputStream(new FileInputStream(nextFile));
    				//Set the last modified time to that of the file
    				lastModTime = nextFile.lastModified();
    				//Flag that the file being processed in not an archive
    				processingArchive = false;
    			}
			}
			catch (Exception e)
			{
				// TODO Log error
				e.printStackTrace();
			}
			
			return inputStream;
		}

		public void remove()
		{
			throw new UnsupportedOperationException("Cannot remove file from the DataSet");
		}
	}

	/** Test harness 
	 * @throws FileNotFoundException */
	public static void main(String[] args) throws IOException
	{
		InputDataSet input = new InputDataSet();
		input.addInputDir("/Users/rvasa/data/Builds/maven/maven-2.0.5", true);
		System.out.println("Files in input data set: "+input.size());
		int items = 0;
		for (InputStream s : input)
		{
			if (s != null) System.out.println(s);
			items++;			
		}
		System.out.println("Items processed: "+items);
	}
}
