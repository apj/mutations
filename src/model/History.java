package model;

import java.util.HashMap;
import java.util.Map;

import model.vocab.EProcessingStatus;

/**
 * Represents a container for the evolution of a software system, including a history of versions
 * and general information about the software system
 * 
 * @author Allan Jones
 */
public class History
{
	//TODO: This will need to be revised, as bigger software systems will consume
	//too much memory to store entire version history
	private Map<Integer, String> versions = new HashMap<Integer, String>();

	private Map<String, String> metaData = new HashMap<String, String>();
	
	//The extent to which this class has been processed (TODO: Add explanation of possible values and when they are set)
	private EProcessingStatus processingStatus = EProcessingStatus.UNPROCESSED;
	
	public History(Map<Integer, String> versions, Map<String, String> metaData)
	{
		this.versions = versions;
		this.metaData = metaData;
	}
	
	public Map<Integer, String> getVersions()
	{
		return versions;
	}
	
	public String getMetaDataValue(String key)
	{
		if(key == null)
			throw new NullPointerException("Could not fetch metadata, given key value was null.");
		
		return metaData.get(key);
	}
	
	public String getName()
	{
		return metaData.get("name");
	}

	public String getShortName()
	{
		return metaData.get("short-name");
	}

	public String getSystemType()
	{
		return metaData.get("type");
	}

	public String getAppDescription()
	{
		return metaData.get("desc");
	}
	
	public boolean isCommercial()
	{
		return metaData.get("commercial").equalsIgnoreCase("Y");
	}
	
	public int getReleaseCount()
	{
		return versions.size();
	}
	
	public EProcessingStatus getProcessingStatus()
	{
		return processingStatus;
	}

	public void setProcessingStatus(EProcessingStatus processingStatus)
	{
		if(processingStatus == null)
			throw new NullPointerException("Could not set processing status, the status passed was null.");
		
		this.processingStatus = processingStatus;
	}
}
