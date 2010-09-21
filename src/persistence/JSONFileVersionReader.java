package persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import model.ClassMetricData;
import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.EVersionMetricName;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Provides a means of loading a Version object from an InputStream containing it in
 * JSON (JavaScript Object Notation) format
 * 
 * @author Allan Jones
 */
public class JSONFileVersionReader implements IVersionReader
{
	@SuppressWarnings("rawtypes")
	@Override
	public Version readVersion(String system, int rsn)
	{
		Version version = null;
		
		try
		{
			//Get the InputStream for the specified version
			InputStream versionStream = VersionFileStreamAccessor.getInstance().getVersionStream(system, rsn);
			
			//Load the JSON formatted Version object
			JSONObject versionObject = getVersionObjectFromStream(versionStream);
			
			//Extract the versions meta-data
			Map<EVersionMetricName, String> metaData = new HashMap<EVersionMetricName, String>(5);
			metaData.put(EVersionMetricName.ID, (String)((Map)(versionObject.get("metaData"))).get("ID"));
			
			//Extract the versions metrics
			Map<EVersionMetricName, Integer> metrics = getVersionMetrics((JSONObject)versionObject.get("metrics"));
		
			//Get the classes that make up this version
			Map<String, ClassMetricData> classes = getClasses(versionObject);
			
			//Get the flag that indicates whether the version contains classes that deleted in the following version
			boolean hasDeletedClasses = ((Boolean)versionObject.get("hasDeletedClasses")).booleanValue();
			
			//Construct the Version object with the extracted information
			version = new Version(metaData, metrics, classes,
									(Long)versionObject.get("lastModifiedTime"),
									(Long)versionObject.get("lastModifiedDate"),
									hasDeletedClasses);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace(); //TODO: Log and throw a de-serialization exception
		}
		catch(ParseException pe)
		{
			pe.printStackTrace();
		}
		
		return version;
	}

	/** 
	 * Loads the JSON representation of the Version from the specified InputStream
	 * @param versionStream The InputStream containing the versions JSON
	 * @return The JSON representation of the version being loaded
	 * @throws IOException if the InputStream could not be read from
	 * @throws ParseException if the JSON string was incorrectly formed and could not be parsed
	 */
	private JSONObject getVersionObjectFromStream(InputStream versionStream) throws IOException, ParseException
	{
		//Wrap a buffered reader around the InputStream
		BufferedReader reader = new BufferedReader(new InputStreamReader(versionStream));
		//Parse the stream using the JSONParser and return the result
		return (JSONObject)(new JSONParser().parse(reader));
	}
	
	/**
	 * Extracts the metrics key-value map for the versions metrics from a JSONObject representation of the metrics map
	 * @param versionMetricsObject The JSON representation of the version metrics to be extracted
	 * @return The version metrics key-value map extracted from the JSON representation
	 */
	private Map<EVersionMetricName, Integer> getVersionMetrics(JSONObject versionMetricsObject)
	{
		Map<EVersionMetricName, Integer> metrics = new HashMap<EVersionMetricName, Integer>(5);
		metrics.put(EVersionMetricName.RSN, ((Long)versionMetricsObject.get("RSN")).intValue());
		
		Long daysSinceBirth = (Long)versionMetricsObject.get("DAYS_SINCE_BIRTH");
		
		if(daysSinceBirth != null)
			metrics.put(EVersionMetricName.DAYS_SINCE_BIRTH, daysSinceBirth.intValue());
		
		Long daysSinceLastVersion = (Long)versionMetricsObject.get("DAYS_SINCE_LAST_VERSION");
		
		if(daysSinceLastVersion != null)
			metrics.put(EVersionMetricName.DAYS_SINCE_LAST_VERSION, daysSinceLastVersion.intValue());
		
		return metrics;
	}
	
	/**
	 * Extracts a map of class name -> ClassMetricData representing the collection of classes that make up the version
	 * from the JSON representation of the versions data
	 * @param versionObject The JSON representation of the version to extract classes from
	 * @return The versions collection of classes extracted from the JSON representation 
	 */
	private Map<String, ClassMetricData> getClasses(JSONObject versionObject)
	{
		//Retrieve the array storing the classes from the JSON object and initialise the
		//class map to hold the specified number of classes
		JSONArray classesArray = (JSONArray)versionObject.get("classes");
		Map<String, ClassMetricData> classes = new HashMap<String, ClassMetricData>(classesArray.size());
		
		//For each class found in the JSON array
		for(Object classObject : classesArray)
		{
			//Cast the current class as a JSONObject and use the object to extract
			//class info
			ClassMetricData classMetricData = extractClass((JSONObject)classObject);
			classes.put(classMetricData.getClassName(), classMetricData);
		}
		
		return classes;
	}
	
	/**
	 * Extracts a ClassMetricData object from a given JSONObject representation of the class
	 * @param classObject The JSON representation of the class to be extracted
	 * @return The ClassMetricData object that has been extracted from the JSONObject
	 */
	@SuppressWarnings("unchecked")
	private ClassMetricData extractClass(JSONObject classObject)
	{
		ClassMetricData classMetricData = new ClassMetricData();
		
		//Meta-data
		classMetricData.setMetaData(getClassMetaData((JSONObject)classObject.get("metaData")));
		//Metrics
		classMetricData.setMetrics(getClassMetrics((JSONObject)classObject.get("metrics")));
		//Methods
		classMetricData.setMethods(new HashSet<String>((List<String>)classObject.get("methods")));
		//Short methods
		classMetricData.setShortMethods(new HashSet<String>((List<String>)classObject.get("shortMethods")));
		//Fields
		classMetricData.setFields(new HashSet<String>((List<String>)classObject.get("fields")));
		//Dependencies
		classMetricData.setDependencies(new HashSet<String>((List<String>)classObject.get("dependencies")));
		//Users
		classMetricData.setUsers(new HashSet<String>((List<String>)classObject.get("users")));
		//Children
		classMetricData.setChildren(new HashSet<String>((List<String>)classObject.get("children")));
		//Interfaces
		classMetricData.setInterfaces(new HashSet<String>((List<String>)classObject.get("interfaces")));
		//Internal dependencies
		classMetricData.setInternalDependencies(new HashSet<String>((List<String>)classObject.get("internalDependencies")));
		//External calls
		classMetricData.setExternalCalls(new HashMap<String, Integer>((Map<String, Integer>)classObject.get("externalCalls")));
		//Internal library calls
		classMetricData.setInternalLibraryCalls(new HashMap<String, Integer>((Map<String, Integer>)classObject.get("internalLibCalls")));
		//External library calls
		classMetricData.setExternalLibraryCalls(new HashMap<String, Integer>((Map<String, Integer>)classObject.get("externalLibCalls")));
		
		return classMetricData;
	}
	
	/**
	 * Extracts the meta-data key-value map for a class from a JSONObject representation of the class
	 * @param classMetaDataObject The JSON representation of the class meta-data to be extracted
	 * @return The class meta-data key-value map extracted from the JSON representation
	 */
	private Map<EClassMetricName, String> getClassMetaData(JSONObject classMetaDataObject)
	{
		//Create the map and set it's size the number of elements in the JSONObject
		Map<EClassMetricName, String> classMetaData = new HashMap<EClassMetricName, String>(classMetaDataObject.size());
		
		//For each metric name in the JSON map
		for(Object key : classMetaDataObject.keySet())
		{
			//Get the corresponding metric name for the key and store the value in the map
			EClassMetricName metric = EClassMetricName.valueOf((String)key);
			classMetaData.put(metric, (String)classMetaDataObject.get(key));
		}
		
		return classMetaData;
	}
	
	/**
	 * Extracts the metrics key-value map for a class from a JSONObject representation of the class
	 * @param classMetaDataObject The JSON representation of the class metrics to be extracted
	 * @return The class metrics key-value map extracted from the JSON representation
	 */
	private Map<EClassMetricName, Integer> getClassMetrics(JSONObject classMetricsObject)
	{
		//Create the map and set it's size the number of elements in the JSONObject
		Map<EClassMetricName, Integer> classMetrics = new HashMap<EClassMetricName, Integer>(classMetricsObject.size());
		
		//For each metric name in the JSON map
		for(Object key : classMetricsObject.keySet())
		{
			//Get the corresponding metric name for the key and store the value in the map
			EClassMetricName metric = EClassMetricName.valueOf((String)key);
			classMetrics.put(metric, ((Long)classMetricsObject.get(key)).intValue());
		}
		
		return classMetrics;
	}

	@Override
	//TODO: Add checks for class finalization
	public boolean versionExtracted(String system, int rsn)
	{
		try
		{
			//Get the InputStream for the specified version
			InputStream versionStream = VersionFileStreamAccessor.getInstance().getVersionStream(system, rsn);
			//Considered extracted if there is actually something to load
			return versionStream != null;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}