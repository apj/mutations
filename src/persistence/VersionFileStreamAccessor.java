package persistence;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import config.ConfigKeys;
import config.ConfigManager;

/**
 * Provides a means for access to FileInputStreams containing data for versions
 * 
 * @author Allan Jones
 */
public class VersionFileStreamAccessor
{
	private static VersionFileStreamAccessor instance;
	
	private VersionFileStreamAccessor()
	{
		
	}
	
	public static VersionFileStreamAccessor getInstance()
	{
		if (instance == null) instance = new VersionFileStreamAccessor();
		return instance;
	}
	
	/**
	 * Loads an InputStream for a version of a specified system and RSN
	 * @param system The system that the version belongs to
	 * @param rsn The RSN for the version (Release Sequence Number)
	 * @return The InputStream to the version with the specified system and RSN
	 */
	public InputStream getVersionStream(String system, int rsn) throws IOException
	{
		//Get directory path
		File historyDir = new File(ConfigManager.getStringProperty(ConfigKeys.VERSION_PERSISTENCE_DIRECTORY) + system);
		
		//Verify that the directory exists
		if(!historyDir.exists())
			return null;
//			throw new FileNotFoundException("Could not find system directory at "
//											+ historyDir.getAbsolutePath()
//											+ ". Please ensure that the correct path is set.");
		//Verify that the path is a directory
		if(!historyDir.isDirectory())
			return null;
//			throw new FileNotFoundException("Cannot load Version from "
//											+ historyDir.getAbsolutePath()
//											+ ", specified path is not a directory.");
		
		//TODO: Make this configurable for different file types 
		File versionFile = new File(historyDir + "/" + system + "-" + rsn + ".ver.json");
		
		if(!versionFile.exists())
			return null;
//			throw new FileNotFoundException("Cannot load Version from "
//					+ versionFile.getPath()
//					+ ", file not found.");
			 
		 return new BufferedInputStream(new FileInputStream(versionFile));
	}
}