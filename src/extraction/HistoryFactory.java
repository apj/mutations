package extraction;

import io.InputDataSet;
import io.TextFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import model.ClassMetricData;
import model.History;
import model.MetricUtil;
import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.EEvolutionCategory;
import model.vocab.EModificationStatus;
import model.vocab.EVersionMetricName;
import persistence.IVersionWriter;
import persistence.VersionWriterFactory;

/**
 * Factory class for extracting information about a software systems history and the collection of versions associated
 * with the system
 * 
 * @author Allan Jones
 */
public class HistoryFactory
{
	private static HistoryFactory instance;

	//A list of the input files that represent the versions
	private List<File> versionFilesToProcess;
	//A map of version Release Sequence Number -> Version ID
	private Map<Integer, String> versions;
	//Metadata associated with the system
	private Map<String, String> metaData;
	
	//Specific packages to include within the extraction process
	private Set<String> includePackages;
	//Specific packages to exclude from the extraction process
	private Set<String> excludePackages;

	//A map of class names -> ClassMetricData indicating the first appearance of classes
	private Map<String, ClassMetricData> firstAppearances;
	
	private HistoryFactory()
	{ }

	public static HistoryFactory getInstance()
	{
		if (instance == null) instance = new HistoryFactory();
		return instance;
	}

	/**
	 * Gets information regarding a software systems history using a given input file
	 * 
	 * @param historyFile File containing information about the software system and a list of versions
	 * @return The extracted history information for the given software system
	 * @throws IOException if the history file cannot be read from
	 */
	public History getHistory(File historyFile) throws IOException
	{
		//TODO: Incorporate processing status checks
		extractHistoryInfo(historyFile);

		//If versions aren't already extracted, extract them from the InputDataSet and process them
		if(!versionsExtracted())
		{
			//TODO: Log
//			System.out.println("Versions have not been extracted, starting extraction processing.");
			//Extract versions
			extractVersionsFromInputData();
			//Post-process once all versions once they have been extracted
			postProcessVersions();
		}
//		else
//			System.out.println("All versions have been extracted already, skipping extraction process."); //TODO: Log
		
		return new History(versions, metaData);
	}
	
	/**
	 * Extracts the history information for the software system from file
	 * @param historyFile The history file to extract information from
	 * @throws IOException if the history file cannot be read from
	 */
	private void extractHistoryInfo(File historyFile) throws IOException
	{
		versionFilesToProcess = new ArrayList<File>();
		versions = new TreeMap<Integer, String>();
		metaData = new HashMap<String, String>();
		
		includePackages = new HashSet<String>();
		excludePackages = new HashSet<String>();
		
		//Create a new text file from the input file 
		TextFile historyTextFile = new TextFile(historyFile);
		
		//Process each line of the file
		for (String line : historyTextFile)
		{
			//Empty line, skip
			if (line.trim().length() == 0) continue;
			
			//Comment, skip
			if (line.trim().startsWith("#")) continue;

			//Project URL, skip
			if(line.trim().startsWith("@")) continue;
			
			//Metadata key-value pair
			if (line.trim().startsWith("%")) 
			{
				String[] tokens = line.substring(1).split("="); 
				String key = tokens[0].trim();
				String value = tokens[1].trim();
				
				metaData.put(key, value);
				
				continue;
			}
			
			//Processing directive
			if(line.trim().startsWith(">"))
			{
				String[] tokens = line.substring(1).split(":"); 
				String key = tokens[0].trim();
				String value = tokens[1].trim();
				
				//Include package directive
				if(key.equalsIgnoreCase("includePackage")) includePackages.add(value);
				//Exclude package directive
				else if(key.equalsIgnoreCase("excludePackage")) excludePackages.add(value);
				
				continue;
			}

			//Split version entry
			String[] cols = line.split(",");
			
			//If RSN, ID, File/Dir entry not found, skip as the data is incorrect
			//TODO: Log warning
			if (cols.length != 3) continue; 
			
			File versionFile = new File(historyFile.getParent(), cols[2].trim());
			
			versionFilesToProcess.add(versionFile);
			
			versions.put(Integer.parseInt(cols[0].trim()), cols[1].trim());
		}
		
		historyTextFile.close();
		
		String shortName = metaData.get("name").split(" ")[0].trim();
		metaData.put("short-name", shortName);
	}
	
	/**
	 * Checks whether each version in the systems history has been extracted to determine
	 * if all versions have been extracted
	 * @return Whether all versions have been extracted
	 */
	private boolean versionsExtracted()
	{
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		//For each version
		for(int i = 1; i <= versionFilesToProcess.size(); i++)
		{
			//If version hasn't been extracted, return false 
			if(!versionFactory.versionExtracted(metaData.get("short-name"), i))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Extracts the software systems version history from the input files list
	 * containing version classes
	 * @throws IOException
	 */
	private void extractVersionsFromInputData() throws IOException
	{
		InputDataSet versionData = null;
		
		//For each version file
		for(int i = 1; i <= versionFilesToProcess.size(); i++)
		{
			//Wrap version file in an InputDataSet container
			versionData = new InputDataSet();
			
			File fileToProcess = versionFilesToProcess.get(i - 1);
			
			if(fileToProcess.isDirectory())
				versionData.addInputDir(fileToProcess.getPath(), false);
			else
				versionData.addInputFile(versionFilesToProcess.get(i - 1));
			
			Version version = null;
			
			try
			{
				//Extract the version using the InputDataSet
				version = VersionFactory.getInstance().getVersion(versionData, i, versions.get(i),
						includePackages, excludePackages);
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.err.println("Failed to extract - RSN: " + i + ", ID: " + versions.get(i));
			}
			
			//Dump the version to an underlying data store using a VersionWriter
			//TODO: Change to dynamically load class
			VersionWriterFactory.getInstance().getWriter().writeVersion(metaData.get("short-name"), version);
		}
		
		versionFilesToProcess = null;
		includePackages = null;
		excludePackages = null;
	}
	
	/**
	 * Post-process version data once all versions have been extracted
	 */
	private void postProcessVersions()
	{
		scanAndMarkSurvivors();
		extractFirstAppearances();
		updateDistanceMovedSinceBirth();
		checkForSimpleRenames();
	}

	/**
	 * Process all versions, updating the relative age of each version, setting it's evolution
	 * metrics and marking classes that have been deleted
	 */
	private void scanAndMarkSurvivors()
	{
		//If only one version, skip, no evolution metrics can be extracted
		if (versions.size() < 2) return;
		
		VersionFactory versionFactory = VersionFactory.getInstance(); 
		
		//Set RSN 1 age to baseline and persist changes
		Version firstVersion = versionFactory.getVersion(metaData.get("short-name"), 1);
		firstVersion.setMetricValue(EVersionMetricName.DAYS_SINCE_BIRTH, 1);
		IVersionWriter versionWriter = VersionWriterFactory.getInstance().getWriter();
		versionWriter.writeVersion(metaData.get("short-name"), firstVersion);
		
		//For each version
		for (int i = 2; i <= versions.size(); i++)
		{
			//Get previous version for comparison
			Version comparingVersion = versionFactory.getVersion(metaData.get("short-name"), i - 1);
			//Get current versions
			Version version = versionFactory.getVersion(metaData.get("short-name"), i);
			
			//Set relative age of version
			setRelativeAge(version, comparingVersion.getLastModifiedTime(), firstVersion.getLastModifiedTime());
			
			//TODO: Log error
			if (version.getLastModifiedTime() < comparingVersion.getLastModifiedTime())
				System.out.println("THATS ODD - V2 age is negative " + version.getRSN() + " " + comparingVersion.getRSN());
			
			//Set evolution flags for version
			setEvolutionFlags(version, comparingVersion);

			//Mark deleted classes within the version
			markDeletedClasses(version, comparingVersion);
			
			//Persist version changes to underlying data store
			versionWriter.writeVersion(metaData.get("short-name"), comparingVersion);
			versionWriter.writeVersion(metaData.get("short-name"), version);
		}
	}

	/**
	 * Set the ages of the version relative to the previous version and the first version
	 * @param version The version to set the age for
	 * @param previousVersionModifiedTime The last modification time of the previous version
	 * @param firstVersionModifiedTime The last modification time of the first version
	 */
	private void setRelativeAge(Version version, long previousVersionModifiedTime, long firstVersionModifiedTime)
	{
		//TODO: Add sanity checks here
		version.setMetricValue(EVersionMetricName.DAYS_SINCE_BIRTH, (int) (version.getLastModifiedTime() - firstVersionModifiedTime));
		version.setMetricValue(EVersionMetricName.DAYS_SINCE_LAST_VERSION, (int) (version.getLastModifiedTime() - previousVersionModifiedTime));
	}

	/**
	 * Sets the evolution flags for each class in a version, based on it's status in the previous version
	 * @param currentVersion The current version whose flags are being set
	 * @param previousVersion The previous version that is being compared to
	 */
	private void setEvolutionFlags(Version currentVersion, Version previousVersion)
	{
		//For each class in the current version
		for (ClassMetricData currentVersionClass : currentVersion.getClasses().values())
		{
			//Assume that the distance is new or unchanged
			currentVersionClass.setMetricValue(EClassMetricName.EVOLUTION_DISTANCE, 0);
			
			//Get class in previous version
			ClassMetricData previousVersionClass = previousVersion.getClasses().get(currentVersionClass.getClassName());
			
			//Establish class as new if not found in previous version
			if (previousVersionClass == null)
				setNewFlags(currentVersionClass, currentVersion.getRSN());
			else
			{
				//Set classes born RSN to that of the previous versions class
				currentVersionClass.setMetricValue(EClassMetricName.BORN_RSN, previousVersionClass.getMetricValue(EClassMetricName.BORN_RSN));
				
				//If class is exact match, flag as unchanged,
				//else flag as modified
				if (currentVersionClass.isExactMatch(previousVersionClass))
					setUnchangedFlags(currentVersionClass, previousVersionClass);
				//Class has been modified in the current version
				else
					setModificationFlags(currentVersionClass, previousVersionClass);
			}
		}
	}

	/**
	 * Establishes a given class as being new to a particular version by setting it's evolution metrics to indicate it is new
	 * @param newClass The new class
	 * @param bornRSN The version that the new class belongs to
	 */
	private void setNewFlags(ClassMetricData newClass, int bornRSN)
	{
		newClass.setMetricValue(EClassMetricName.EVOLUTION_STATUS, EEvolutionCategory.ADDED.getValue());
		newClass.setMetricValue(EClassMetricName.EVOLUTION_DISTANCE, 0);
		newClass.setMetricValue(EClassMetricName.MODIFICATION_FREQUENCY, 0);
		newClass.setMetricValue(EClassMetricName.BORN_RSN, bornRSN);
		newClass.setMetricValue(EClassMetricName.AGE, 0);
	}

	/**
	 * Establishes a given class as being unchanged in a particular version by setting it's evolution metrics to indicate it is unchanged
	 * @param currentVersionClass The current version class
	 * @param previousVersionClass The previous version of the class
	 */
	private void setUnchangedFlags(ClassMetricData currentVersionClass, ClassMetricData previousVersionClass)
	{
		currentVersionClass.incrementMetric(EClassMetricName.AGE, previousVersionClass.getMetricValue(EClassMetricName.AGE) + 1);
		currentVersionClass.setMetricValue(EClassMetricName.EVOLUTION_STATUS,
										EEvolutionCategory.UNCHANGED.getValue());
		previousVersionClass.setMetricValue(EClassMetricName.NEXT_VERSION_STATUS,
										EEvolutionCategory.UNCHANGED.getValue());
		currentVersionClass.setMetricValue(EClassMetricName.MODIFICATION_FREQUENCY,
										previousVersionClass.getMetricValue(EClassMetricName.MODIFICATION_FREQUENCY));
		currentVersionClass.setMetricValue(EClassMetricName.EVOLUTION_DISTANCE, 0);
		currentVersionClass.setMetricValue(EClassMetricName.MODIFIED_METRIC_COUNT, 0);
	}
	
	/**
	 * Establishes a given class as being modified in a particular version by setting it's evolution metrics to indicate it is modified
	 * @param currentVersionClass The current version class
	 * @param previousVersionClass The previous version of the class
	 */
	private void setModificationFlags(ClassMetricData currentVersionClass, ClassMetricData previousVersionClass)
	{
		previousVersionClass.setMetricValue(EClassMetricName.NEXT_VERSION_STATUS, EEvolutionCategory.MODIFIED.getValue());
		previousVersionClass.setMetricValue(EClassMetricName.IS_MODIFIED, 1);
		
		currentVersionClass.setMetricValue(EClassMetricName.EVOLUTION_STATUS, EEvolutionCategory.MODIFIED.getValue());
		currentVersionClass.setMetricValue(EClassMetricName.MODIFICATION_FREQUENCY, previousVersionClass.getMetricValue(EClassMetricName.MODIFICATION_FREQUENCY) + 1);
		currentVersionClass.setMetricValue(EClassMetricName.AGE, 1);
		currentVersionClass.setMetricValue(EClassMetricName.EVOLUTION_DISTANCE, MetricUtil.getDistanceBetween(currentVersionClass, previousVersionClass));
		currentVersionClass.setMetricValue(EClassMetricName.MODIFIED_METRIC_COUNT, MetricUtil.computeModifiedMetrics(currentVersionClass, previousVersionClass));
	}
	
	/**
	 * Processes each class in a previous version and marks any classes that are not found in the following version
	 * as having been deleted
	 * @param currentVersion The current version
	 * @param previousVersion The previous version
	 */
	private void markDeletedClasses(Version currentVersion, Version previousVersion)
	{
		//For each class in the previous version
		for (ClassMetricData previousVersionClass : previousVersion.getClasses().values())
		{
			//Get the corresponding current version class
			ClassMetricData currentVersionClass = currentVersion.getClasses().get(previousVersionClass.getClassName());
			
			//If current version class not found, mark as deleted
			if (currentVersionClass == null)
			{
				//Flag the class as having deleted class
				previousVersion.setHasDeletedClasses(true);
				//Set deletion flags on the class
				setDeletedFlags(previousVersionClass);
			}
		}
	}

	/**
	 * Flags a class as having been deleted in the following version
	 * @param versionClass The class to flag as having been deleted
	 */
	private void setDeletedFlags(ClassMetricData versionClass)
	{
		versionClass.setMetricValue(EClassMetricName.IS_DELETED, 1);
		versionClass.setMetricValue(EClassMetricName.NEXT_VERSION_STATUS, EEvolutionCategory.DELETED.getValue());
	}
	
	/**
	 * Updates the distance moved for each class in each version
	 */
	private void updateDistanceMovedSinceBirth()
	{
		VersionFactory versionFactory = VersionFactory.getInstance();
		IVersionWriter versionWriter = VersionWriterFactory.getInstance().getWriter();
	
		//For each version
    	for (int i = 1; i <= versions.size(); i++)
    	{
    		//Get the Version object
    		Version version =  versionFactory.getVersion(metaData.get("short-name"), i);
    		
    		//For each class in the version
    		for (ClassMetricData classMetricData : version.getClasses().values())
    		{
    			//If next version status is modified, flag as being modified in the next version
    			if (classMetricData.getMetricValue(EClassMetricName.NEXT_VERSION_STATUS) == EEvolutionCategory.MODIFIED.getValue())
    				classMetricData.setMetricValue(EClassMetricName.IS_MODIFIED_NEXT_VERSION, 1);
    			
    			//If class was born in this version
    			if (classMetricData.getMetricValue(EClassMetricName.BORN_RSN) == version.getRSN())
    			{
    				//Set zero distance movement
    				classMetricData.setMetricValue(EClassMetricName.DISTANCE_MOVED_SINCE_BIRTH, 0);
    				//Set status since birth as new born
    				classMetricData.setMetricValue(EClassMetricName.MODIFICATION_STATUS_SINCE_BIRTH, EModificationStatus.NEW_BORN.getValue());
    				continue;
    			}
    			
    			//Sanity check to ensure class was not marked as being born in a later version,
    			//but found in this version
    			if (classMetricData.getMetricValue(EClassMetricName.BORN_RSN) > version.getRSN())
    			{
    				//TODO: Log error
//    				System.out.println("FATAL ERROR! INVESTIGATE THIS NOW!!");
    				continue; // keep moving
    			}
    			
    			//Update the distance the class has moved based on it's first version
    			updateDistanceBasedOnAncestor(classMetricData);
    		}
    		
    		//Write the version to the underlying data store
    		versionWriter.writeVersion(metaData.get("short-name"), version);
    	}
	}
	
	/**
	 * Updates the distance a given class has moved based on the first version of the class
	 * @param classMetricData
	 */
	private void updateDistanceBasedOnAncestor(ClassMetricData classMetricData)
	{		
		try
		{ 
			//Get the first appearance of the class
			ClassMetricData ancestor = firstAppearances.get(classMetricData.getClassName());
			
			//Mark as never modified if class is an exact match,
			//else calculate distance moved
			if (ancestor.isExactMatch(classMetricData))
				classMetricData.setMetricValue(EClassMetricName.MODIFICATION_STATUS_SINCE_BIRTH, EModificationStatus.NEVER_MODIFIED.getValue());
			else
			{
				//Determine distance moved since birth and set
				classMetricData.setMetricValue(EClassMetricName.DISTANCE_MOVED_SINCE_BIRTH, MetricUtil.getEvolutionDistanceSinceBirth(classMetricData, ancestor));
				//Mark as having been modified since birth
				classMetricData.setMetricValue(EClassMetricName.MODIFICATION_STATUS_SINCE_BIRTH, EModificationStatus.MODIFIED_AFTER_BIRTH.getValue());
				//Determine number of metrics that have been modified
				classMetricData.setMetricValue(EClassMetricName.MODIFIED_METRIC_COUNT_SINCE_BIRTH, MetricUtil.computeModifiedMetrics(classMetricData, ancestor));
			}
		}
		catch(Exception e)
		{
			System.err.println("Failed to fetch ancestor for: " + classMetricData.getClassName() + "| Born RSN = " + classMetricData.getMetricValue(EClassMetricName.BORN_RSN));
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracts the first appearances for each class that has been apart of the systems history
	 */
	private void extractFirstAppearances()
	{
		VersionFactory versionFactory = VersionFactory.getInstance();
		int releaseCount = versions.size();
		
		//Get the number of classes in the last version and initialise the map
		Version lastVersion = versionFactory.getVersion(metaData.get("short-name"), releaseCount);
		int lastVersionClassCount = lastVersion.getClassCount();
		firstAppearances = new HashMap<String, ClassMetricData>(lastVersionClassCount);
		
		//For each version
		for(int i = 1; i <= releaseCount; i++)
		{
			//Get the version object
			Version version = versionFactory.getVersion(metaData.get("short-name"), i);
			//Get the versions classes
			Map<String, ClassMetricData> versionClasses = version.getClasses();
			
			//Add each class that has not appeared previously to the first appearances map
			for(Entry<String, ClassMetricData> classEntry : versionClasses.entrySet())
			{
				String className = classEntry.getKey();
				
				if(!firstAppearances.containsKey(className))
					firstAppearances.put(className, classEntry.getValue());
			}
		}
	}
	
	/**
	 * Process each class in each version and determines whether simple class renames have occurred
	 */
	//TODO: This contains a bug...fix it! -- Suspect the bug is in clone checking
	//TODO: Refactor...break down to be smaller and more readable
	private void checkForSimpleRenames()
	{
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		//For each version
		for (int i = 2; i <= versions.size(); i++)
		{
			//Get the previous version
			Version previousVersion = versionFactory.getVersion(metaData.get("short-name"), i - 1);
			
			//Previous has no deleted classes, skip to the next version
			if (previousVersion.hasDeletedClasses() == false) continue;

			//Get the current version
			Version currentVersion = versionFactory.getVersion(metaData.get("short-name"), i);
			
			//Get a map of classes that match a class to a list of classes that appear to be clones
			Map<ClassMetricData, List<ClassMetricData>> deletedClassesMap = getDeletedClasses(previousVersion);
			
			//Get the new classes for the current version
			List<ClassMetricData> currentVersionNewClasses = getNewClassesInVersion(currentVersion);
			
			//For all deleted classes, compare to each new class in the current version
			for (Entry<ClassMetricData, List<ClassMetricData>> deletedClassEntry : deletedClassesMap.entrySet())
			{
				//Get the deleted class object
				ClassMetricData deletedClass = deletedClassEntry.getKey();
				
				//For each class that is new in the current version
				for (ClassMetricData currentVersionNewClass : currentVersionNewClasses)
				{
					//If the deleted class and new class are clones, add the new class
					//to the list of clones for the deleted class
					if (isClone(deletedClass, currentVersionNewClass))
					{
						//NOTE: If class identified multiple times, then clone
						//detection is weak
						if (deletedClassesMap.get(deletedClass) == null)
							deletedClassesMap.put(deletedClass, new ArrayList<ClassMetricData>());
						
						deletedClassesMap.get(deletedClass).add(currentVersionNewClass);
					}
				}
			}

			//For each deleted class
			for (Entry<ClassMetricData, List<ClassMetricData>> deletedClassEntry : deletedClassesMap.entrySet())
			{
				//Get the deleted class object
				ClassMetricData deletedClass = deletedClassEntry.getKey();
				
				//If no list matching the deleted class, continue
				if (deletedClassEntry.getValue() == null) continue;
				
				//Else if a unique clone was found for the deleted class
				else if (deletedClassesMap.get(deletedClass).size() == 1)
				{
					//Get the class the deleted class was renamed to
					ClassMetricData modifiedAs = deletedClassesMap.get(deletedClass).get(0);
					//Set modification flags for the deleted class
					setModificationFlags(modifiedAs, deletedClass);
				}
			}
		}
	}

	/**
	 * Creates a map of classes deleted within a version mapped to null values (placeholders)
	 * @param version The version to extract deleted classes from
	 * @return The map of deleted classes
	 */
	private Map<ClassMetricData, List<ClassMetricData>> getDeletedClasses(Version version)
	{
		//Create the deleted classes map
		Map<ClassMetricData, List<ClassMetricData>> deletedClassesMap = new HashMap<ClassMetricData, List<ClassMetricData>>();

		//For each class in the version
		for (ClassMetricData versionClass : version.getClasses().values())
			//If the class is deleted in the following version, add it to the map
			if (versionClass.getMetricValue(EClassMetricName.NEXT_VERSION_STATUS) == EEvolutionCategory.DELETED.getValue())
				deletedClassesMap.put(versionClass, null);
		
		return deletedClassesMap;
	}

	/**
	 * Extracts a list of classes that are new in the given version
	 * @param version The version to extract new classes from
	 * @return The list of classes new to the given version 
	 */
	private List<ClassMetricData> getNewClassesInVersion(Version version)
	{
		//Create the list of new class for the version
		List<ClassMetricData> newClassesInVersion = new ArrayList<ClassMetricData>();
		
		//For each class in the version
		for (ClassMetricData versionClass : version.getClasses().values())
		{
			//If the class is new, add it to the list of new classes
			if (versionClass.getMetricValue(EClassMetricName.EVOLUTION_STATUS) == EEvolutionCategory.ADDED.getValue())
				newClassesInVersion.add(versionClass);
		}
		return newClassesInVersion;
	}
	
	/**
	 * Determines whether two classes are a clone of one another based on a number of factors
	 * @param deletedClass The class that was 'deleted' from a version
	 * @param newClass The 'new' class that is being checked against the deleted class
	 * @return Whether the new class is effectively a clone of the deleted class
	 */
	//TODO: Describe factors that determine the match once confirmed
	//TODO: Move to metric util
	//TODO: Refactor...break down to be smaller and more readable
	private boolean isClone(ClassMetricData deletedClass, ClassMetricData newClass)
	{
		if ((deletedClass == null) || (newClass == null))
		{
			//TODO: Log error
			return false;
		}

		//Basic checks first -- assume that an interface does not get promoted into a class
    	if (deletedClass.getMetricValue(EClassMetricName.IS_INTERFACE).intValue() != newClass.getMetricValue(EClassMetricName.IS_INTERFACE).intValue()) return false;
    	
    	//Minor rename, where class upper/lower class renames happen
    	if (deletedClass.getClassName().equalsIgnoreCase(newClass.getClassName())) return true;
    	
    	double userSubsetPercent = MetricUtil.usersSubSetPercentage(deletedClass, newClass);
    	double methodSubsetPercent = MetricUtil.methodSubSetPercentage(deletedClass, newClass);
    	double fieldSubsetPercent = MetricUtil.fieldSubSetPercentage(deletedClass, newClass);
    	double depsSubsetPercent = MetricUtil.dependenciesSubSetPercentage(deletedClass, newClass);
    	
    	if (deletedClass.getShortClassName() == null) System.out.println("classMetricData1 - shortClassName is null");
    	if (newClass.getShortClassName() == null) System.out.println("classMetricData2 - shortClassName is null");
    	
		if ((deletedClass.getShortClassName().equalsIgnoreCase(newClass.getShortClassName())) &&
				(deletedClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue() == newClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue()) &&
				(userSubsetPercent > 0.8))
		{	
			return true;
		}

		if ((deletedClass.getShortClassName().equalsIgnoreCase(newClass.getShortClassName())) &&
				(deletedClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue() == newClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue()) &&
				(depsSubsetPercent > 0.7) && (fieldSubsetPercent > 0.7) &&				
				(methodSubsetPercent > 0.7))
			return true;
		
    	// Same short name and near identical metric set
		if ((deletedClass.getShortClassName().equalsIgnoreCase(newClass.getShortClassName())) &&
				(MetricUtil.distanceBetween(deletedClass, newClass) < 15.0))
			return true;
		
		if ((deletedClass.getShortClassName().equalsIgnoreCase(newClass.getShortClassName())) &&
			(MetricUtil.distanceBetween(deletedClass, newClass) < 30.0) && 
			(methodSubsetPercent > 0.3) && 
			(fieldSubsetPercent > 0.4) && 
			(depsSubsetPercent > 0.35))
			return true;

		if ((deletedClass.getShortClassName().equalsIgnoreCase(newClass.getShortClassName())) &&
				(methodSubsetPercent > 0.499) &&
				(fieldSubsetPercent > 0.499) &&
				(MetricUtil.distanceBetween(deletedClass, newClass) < 100.0))
				return true;

		if ((deletedClass.getShortClassName().equalsIgnoreCase(newClass.getShortClassName())) &&
			(methodSubsetPercent > 0.7))
			return true;
		
		//If exception, then should have same users more or less, and should not have changed much
    	if ((deletedClass.getMetricValue(EClassMetricName.IS_EXCEPTION).intValue() == 1) && (MetricUtil.distanceBetween(deletedClass, newClass) < 1.0) &&
    			(userSubsetPercent >= 0.9))
    		return true;
    	
    	//Class has a different name, but it is essentially the same class		
		if ((methodSubsetPercent > 0.99998) && (fieldSubsetPercent > 0.9998))			
		{
			if (MetricUtil.distanceBetween(deletedClass, newClass) == 0) return true;
			if (deletedClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue() == newClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue()) return true;
		}
				
		// Pick up a minor edit
		if ((deletedClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue() == newClass.getMetricValue(EClassMetricName.IS_ABSTRACT).intValue()) &&
			 (MetricUtil.exactMethodMatch(deletedClass, newClass)) &&
			 (MetricUtil.distanceBetween(deletedClass, newClass) < 1.0))
			return true;
    	
		return false;
	}
}