package persistence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

import model.ClassMetricData;
import model.Version;

import org.json.simple.JSONObject;

import config.ConfigKeys;
import config.ConfigManager;

/**
 * Provides a means for writing Version objects to file in JSON (JavaScript Object Notation) format
 * 
 * @author Allan Jones
 */
public class JSONFileVersionWriter implements IVersionWriter
{
	@Override
	public void writeVersion(String system, Version version)
	{
		try
		{
			if(version == null)
				throw new NullPointerException("Could not write version as the specified version object was null");
			
			//Convert the Version object to JSON
			JSONObject versionObject = getVersionObject(version);

			//Create the output folder for the system
			File outputFolder = new File(ConfigManager.getStringProperty(ConfigKeys.VERSION_PERSISTENCE_DIRECTORY) + system + "/");
			
			if(!outputFolder.exists())
			{
				boolean foldersCreated = outputFolder.mkdirs();
				
				if(!foldersCreated)
					throw new IOException("Could not create folders for path: " + outputFolder.getPath());
			}
			
			//Write the JSON representation of the version to file
			writeVersionObjectToFile(versionObject, new File(outputFolder.getPath() + "/" + system + "-" + version.getRSN() + ".ver.json"));
		}
		catch (IOException e)
		{
			e.printStackTrace(); //TODO: Log and throw serialization exception
		}
	}

	/**
	 * Gets a JSON representation of the data contained within the specified Version object
	 * @param version The Version containing the data to convert to JSON
	 * @return The JSON representation of the data within the version
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getVersionObject(Version version)
	{
		//Create an empty JSON object to house version data
		JSONObject versionObject = new JSONObject();
		
		//Meta-data
		versionObject.put("metaData", version.getMetaData());
		//Metrics
		versionObject.put("metrics", version.getMetrics());
		//Time since the version was last modified
		versionObject.put("lastModifiedTime", version.getLastModifiedTime());
		//Date the version was last modified
		versionObject.put("lastModifiedDate", version.getLastModifiedDate().getTime());
		//Whether the version has deleted class
		versionObject.put("hasDeletedClasses", version.hasDeletedClasses());
		//The processing status of the version
		versionObject.put("processingStatus", version.getProcessingStatus().name());
		//Classes
		versionObject.put("classes", getClasses(version));
		
		return versionObject;
	}
	
	/**
	 * Converts the versions collection of classes to a list of JSON objects
	 * representing the classes. This method allows the classes to stored in
	 * an array in the JSON output rather than as a map. 
	 * @param version The versions containing the classes to get
	 * @return The list of JSON representations of classes that have been obtained from the version
	 */
	private LinkedList<JSONObject> getClasses(Version version)
	{
		LinkedList<JSONObject> classObjects = new LinkedList<JSONObject>();
		Map<String, ClassMetricData> classes = version.getClasses();
	
		//For each class in the version, add to the list
		for(ClassMetricData classMetricData : classes.values())
			classObjects.add(getClassObject(classMetricData));
		
		return classObjects;
	}
	
	/**
	 * Gets a JSON representation of a ClassMetricData object
	 * @param classMetricData The ClassMetricData object to obtain a JSON representation of
	 * @return The JSON representation of the given ClassMetricData object
	 */
	@SuppressWarnings("unchecked")
	private JSONObject getClassObject(ClassMetricData classMetricData)
	{
		//Create new JSONObject to house the ClassMetricData data
		JSONObject classMetricDataObject = new JSONObject();
		
		//Meta-data
		classMetricDataObject.put("metaData", classMetricData.getMetaData());
		//Metrics
		classMetricDataObject.put("metrics", classMetricData.getMetrics());
		//Methods
		classMetricDataObject.put("methods", new ArrayList<String>(classMetricData.getMethods()));
		//Short methods
		classMetricDataObject.put("shortMethods", new ArrayList<String>(classMetricData.getShortMethods()));
		//Fields
		classMetricDataObject.put("fields", new ArrayList<String>(classMetricData.getFields()));
		//Dependencies
		classMetricDataObject.put("dependencies", new ArrayList<String>(classMetricData.getDependencies()));
		//Users
		classMetricDataObject.put("users", new ArrayList<String>(classMetricData.getUsers()));
		//Children
		classMetricDataObject.put("children", new ArrayList<String>(classMetricData.getChildren()));
		//Interfaces
		classMetricDataObject.put("interfaces", new ArrayList<String>(classMetricData.getInterfaces()));
		//Internal dependencies
		classMetricDataObject.put("internalDependencies", new ArrayList<String>(classMetricData.getInternalDependencies()));
		//External calls
		classMetricDataObject.put("externalCalls", classMetricData.getExternalCalls());
		//Internal library calls
		classMetricDataObject.put("internalLibCalls", classMetricData.getInternalLibraryCalls());
		//External library calls
		classMetricDataObject.put("externalLibCalls", classMetricData.getExternalLibraryCalls());
		
		return classMetricDataObject;
	}
	
	/**
	 * Writes a Version object represented JSON format to a text file 
	 * @param versionObject The JSON representation of the version to be written to file
	 * @param outputFile The file that the JSON representation of the version is to be output to
	 * @throws IOException if the output file could not be written
	 */
	private void writeVersionObjectToFile(JSONObject versionObject, File outputFile) throws IOException
	{
		//Setup the writer for the version output
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)); 
		
		//Write the object to file
		JSONObject.writeJSONString(versionObject, writer);
		
		//Flush and close the file writer
		writer.flush();
		writer.close();
	}
}