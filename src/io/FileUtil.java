package io;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for File based operations
 */
public class FileUtil
{
	/**
	 * Determines whether the specified filename belongs to an archive (i.e., .jar or .zip file)
	 * @param filename The filename that is being checked to see if it is an archive
	 * @return Whether or not the filename belongs to an archive file
	 */
	public static boolean isArchive(String filename)
	{
		//Return false if no filename specified
		if (filename == null) return false;
		
		//Return true if .jar or .zip file
		return filename.endsWith(".jar") || filename.endsWith(".zip");
	}
	
	/**
	 * Determines whether the specified filename belongs to a Java class file (i.e., has a .class extension)
	 * @param filename The filename that is being checked to see if it is a Java class
	 * @return Whether or not the filename belongs to a Java class file
	 */
	public static boolean isClassFile(String filename)
	{
		//Return false if no filename specified
		if (filename == null) return false;
		
		//Return true if .class file
		return filename.endsWith(".class");
	}
	
	/**
	 * Determines whether the specified filename belongs to an inner class file (i.e., it has a '$' symbol indicating
	 * that it is an inner class)
	 * @param filename The filename that is being checked to see if it belongs to an inner class
	 * @return
	 */
	public static boolean isInnerClassFile(String filename)
	{
		//Return false if no filename specified
		if (filename == null) return false;
		
		//Return true if the filename contains a '$' symbol (i.e., it is an inner class)
		return (filename.indexOf("$") > 0);
	}
	
	/**
	 * Gets the name of the outer class that the inner class with the specified name is defined within.
	 * This is accomplished by extracting the classes name up until the first occurence of a '$' symbol
	 * @param innerClassName The name inner class being used to determine the outer class
	 * @return The outer class name for the class that the specified inner class is defined within or null
	 * if no associated outer class name is found
	 */
	public static String getOuterClassName(String innerClassName)
	{
		//TODO: Log error
		if(innerClassName.indexOf("$") < 0) return null;
		
		//Return a substring of the classes name to the first occurence of a '$' symbol
		return innerClassName.substring(0,innerClassName.indexOf("$"));
	}
	
	
	/**
	 * /** 
	 * Creates a set of the files that are present within the directory with the specified name.
	 * If specified, the set of files will be recursively generated
	 * @param directoryName The name of the directory containing the files
	 * @param recursive when true will process directory tree recursively   
	 * @return The set of files contained within the specified directory, or an empty set of files if the
	 * directory with the name specified could not be found or was not a directory.
	 * @throws IOException
	 */
	public static Set<File> getFiles(String directoryName, boolean recursive) 
	throws IOException
	{
		//Create the empty set of files 
		Set<File> fileSet = new HashSet<File>();
		
		//Get the file handle corresponding to the file for the directory name specified 
		File directory = getFileHandle(directoryName);
		
		//Return the empty set if the file handle that was created is not for a directory
		if (!directory.isDirectory()) return fileSet;

		//Get the list of files within the directory
		File[] files = directory.listFiles();
		
		for (File file : files)
		{
			//If current file is a directory and recursion is specified, recursively add the all of the files
			//contained within the directory, else add the file
			if (file.isDirectory() && file.canRead() && recursive) fileSet.addAll(getFiles(file.getPath(), recursive));
			else fileSet.add(file);
		}
		
		return fileSet;
	}	
	
	/**
	 * Creates a file handle corresponding to the specified filename 
	 * @param filename The filename corresponding to the file to create a handle for 
	 * @return The file handle corresponding to the specified filename
	 * @throws IOException - if the specified filename was not a file that could be read from
	 */
	public static File getFileHandle(String filename) throws IOException
	{
		//Create the handle
		File file = new File(filename.trim());
		
		//TODO: Log error
		//Throw an exception if the file cannot be read from
		if (!file.canRead()) throw new FileNotFoundException(filename);
		
		return file;
	}
}
