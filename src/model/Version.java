package model;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import model.vocab.EProcessingStatus;
import model.vocab.EVersionMetricName;

/**
 * Represents a container for a single version of a software system, holding the collection of classes that
 * are part of the version, as well as metrics and meta-data associated with the version
 * 
 * @author Allan Jones
 */
public class Version
{
	//Meta-data for the version (TODO: May be able to get rid of this...only holds ID atm)
	private Map<EVersionMetricName, String> metaData = new HashMap<EVersionMetricName, String>(5);
	//Metric values for the version (TODO: May be able to get rid of this...only holds RSN atm)
	private Map<EVersionMetricName, Integer> metrics = new HashMap<EVersionMetricName, Integer>(15);	

	//The classes that make up the version
	private Map<String, ClassMetricData> classes;
	
	//The day at which the version was last modified
	private long lastModifiedTime;
	//The date at which the version was last modified
	private Date lastModifiedDate;
	
	//Whether the version has deleted classes
	private boolean hasDeletedClasses = false;
	
	//The extent to which this class has been processed (TODO: Add explanation of possible values and when they are set)
	private EProcessingStatus processingStatus = EProcessingStatus.UNPROCESSED;

	/**
	 * Creates a new Version object with a set of classes, RSN (Release Sequence Number) and ID and the time that the version was last modified
	 * @param classes The set of classes that the version is composed of
	 * @param rsn The versions RSN
	 * @param id The versions ID
	 * @param lastModifiedTime The timestamp for the time at which the version was last modified
	 */
	public Version(Map<String, ClassMetricData> classes, int rsn, String id, long lastModifiedTime)
	{
		this.classes = classes;
		
		//Determine the number of days since the version was last modified
		long min = (lastModifiedTime / 1000) / 60;
		long days = (min / 60) / 24;
		this.lastModifiedTime = days;
		
		this.lastModifiedDate = new Date(lastModifiedTime);
		
		setRSN(rsn);
		setId(id);
		
		setMetricValue(EVersionMetricName.DAYS_SINCE_BIRTH, -1);
		setMetricValue(EVersionMetricName.DAYS_SINCE_LAST_VERSION, -1);
	}

	/**
	 * Creates a new Version object with extracted meta-data and metrics, it's set of classes and the days since it was last modified and the time at which it was last modified
	 * @param metaData The set of meta data extracted from the version
	 * @param metrics The set of metrics extracted from the version
	 * @param classes The set of classes that the version is composed of
	 * @param lastModifiedDay The days since the version was last modified
	 * @param lastModifiedTime The timestamp for the time at which the version was last modified
	 * @param hasDeletedClasses Flag indicating the version has deleted classes
	 */
	public Version(Map<EVersionMetricName, String> metaData, Map<EVersionMetricName, Integer> metrics, Map<String, ClassMetricData> classes,
					long lastModifiedDay, long lastModifiedTime, boolean hasDeletedClasses)
	{
		this.metaData = metaData;
		this.metrics = metrics;
		this.classes = classes;
		this.lastModifiedTime = lastModifiedDay;
		this.lastModifiedDate = new Date(lastModifiedTime);
		this.hasDeletedClasses = hasDeletedClasses;
	}
	
	public Map<EVersionMetricName, String> getMetaData()
	{
		return metaData;
	}
	
	public String getMetaDataValue(EVersionMetricName metric)
	{
		if(metric == null)
			throw new NullPointerException("Could not get meta data value, specified metric name was null.");
		
		return metaData.get(metric);
	}
	
	public void setMetaDataValue(EVersionMetricName metric, String value)
	{
		if(metric == null)
			throw new NullPointerException("Could not set meta data value, specified metric name was null.");
		
		metaData.put(metric, value);
	}
	
	public Map<EVersionMetricName, Integer> getMetrics()
	{
		return metrics;
	}
	
	public Integer getMetricValue(EVersionMetricName metric)
	{
		if(metric == null)
			throw new NullPointerException("Could not get metric value, specified metric name was null.");
		
		return metrics.get(metric);
	}
	
	public void setMetricValue(EVersionMetricName metric, int value)
	{
		if(metric == null)
			throw new NullPointerException("Could not set metric value, specified metric name was null.");
		
		metrics.put(metric, value);
	}
	
	public int getRSN()
	{
		return getMetricValue(EVersionMetricName.RSN);  
	}
	
	public void setRSN(int rsn)
	{
		if(rsn < 1)
			throw new IllegalArgumentException("Could not set RSN for version, specified value of " + rsn + " was not valid (value must be greater than or equal to 1).");
		
		setMetricValue(EVersionMetricName.RSN, rsn);
	}
	
	public String getId()
	{
		return getMetaDataValue(EVersionMetricName.ID);
	}
	
	public void setId(String id)
	{
		if(id == null)
			throw new NullPointerException("Could not set ID for version, specified value was null.");
		
		setMetaDataValue(EVersionMetricName.ID, id);
	}

	public Map<String, ClassMetricData> getClasses()
	{
		return classes;
	}

	public void setLastModifiedDate(Date lastModifiedDate)
	{
		if(lastModifiedDate == null)
			throw new NullPointerException("Could not set last modified date for version, specified value was null.");
		
		this.lastModifiedDate = lastModifiedDate;
	}
	
	public long getLastModifiedTime()
	{
		return lastModifiedTime;
	}

	public Date getLastModifiedDate()
	{
		return lastModifiedDate;
	}
	
	public int getDaysSinceBirth()
	{
		return metrics.get(EVersionMetricName.DAYS_SINCE_BIRTH);
	}
	
	public int getDaysSinceLastVersion()
	{
		return metrics.get(EVersionMetricName.DAYS_SINCE_LAST_VERSION);
	}
	
	public boolean hasDeletedClasses()
	{
		return hasDeletedClasses;
	}

	public void setHasDeletedClasses(boolean hasDeletedClasses)
	{
		this.hasDeletedClasses = hasDeletedClasses;
	}

	public EProcessingStatus getProcessingStatus()
	{
		return processingStatus;
	}
	
	public void setProcessingStatus(EProcessingStatus processingStatus)
	{
		if(processingStatus == null)
			throw new NullPointerException("Could not set processing status for version, specified value was null.");
		
		this.processingStatus = processingStatus;
	}
	
	//TODO: Can probably set this in processing
	public int getPackageCount()
	{
		Set<String> packages = new HashSet<String>();
		
		for (ClassMetricData classMetricData : classes.values())
			packages.add(classMetricData.getPackageName());
		
		return packages.size();
	}
	
	public int getClassCount()
	{
		return classes.size();
	}
}