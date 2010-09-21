package io;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ZipFileIterator iterates over a Zip/Jar archive giving access to the 
 * InputStream of the entries in the archive via an iterator
 * 
 * @author rvasa
 */
public class ZipFileIterator implements Iterator<InputStream>
{
	//The zip file
	ZipFile zipFile;
	//An enumeration of the entries within the zip file
	Enumeration<? extends ZipEntry> zipIterator = null;
	//The InputStream for the current entry
	InputStream entry = null;
	//The time that the zip file was last modified
    public long lastModTime;

	public ZipFileIterator(ZipFile zipFile)
	{
        this.zipFile = zipFile;
		
        //Initialise the enumeration with the entries contained in the zip file
        zipIterator = zipFile.entries();
		
		//Find the initial entry
		entry = findNext();
	}

	public boolean hasNext()
	{
		return (entry != null);
	}

	/**
	 * Finds the next entry within the zip file 
	 * @return The InputStream for the next entry within the zip file
	 */
	private InputStream findNext()
	{
		while (zipIterator.hasMoreElements())
		{
            try
            {
            	//Get the next entry in the zip file
                ZipEntry nextEntry = zipIterator.nextElement();
                
                //Get the entries last modified time
                long modifiedTime = nextEntry.getTime();
                
                //If the last modified time is set
                if (modifiedTime > -1)
                {
                	//If the entries last modified time is more recent than the current
                	//last modified time, set the current last modified time to be that
                	//of the entry
                	if (modifiedTime > lastModTime) lastModTime = modifiedTime;
                }
                
                //If the next entry is a class file, return it's corresponding InputStream 
    			if (FileUtil.isClassFile(nextEntry.getName())) 
                    return zipFile.getInputStream(nextEntry);
    			
            }
            catch (IOException ioex)
            {
            	//TODO: Log error
            	//Go to the next element if there is an exception
                continue; 
            }
		}
		return null;
	}

	public InputStream next()
	{
		InputStream nextEntry = entry;
		
		//Prepare the next entry for future calls
		entry = findNext();
		
		return nextEntry;
	}

	public void remove()
	{
		//TODO: Log error
		throw new UnsupportedOperationException("Cannot remove elements from the zip file");
	}
}