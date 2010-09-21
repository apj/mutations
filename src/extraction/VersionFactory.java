package extraction;

import io.InputDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import model.ClassMetricData;
import model.MetricUtil;
import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.EProcessingStatus;
import persistence.IVersionReader;
import persistence.VersionReaderFactory;
import util.MathUtil;
import util.StringUtil;

/**
 * Factory class that will load a Version object comprised of a number of ClassMetricData objects
 * that have been loaded from an InputDataSet. Alternatively, if the version has already been extracted
 * it will load the Version object from an underlying data store
 * @author Allan Jones
 */
public class VersionFactory
{
	private static VersionFactory instance;

	//The input data representing the classes that make up the version
	private InputDataSet inputData;
	//The versions Release Sequence Number
	private int rsn;
	//
	private String versionId;
	
	//TODO: Consider moving include/exclude packages to Config Manager/Util
	//Specific package names to include in extraction
	private Set<String> includePackages;
	//Specific package names to exclude in extraction
	private Set<String> excludePackages;
	
	//Maps the class names to their corresponding metric data containers
	private Map<String, ClassMetricData> classes;
	//Maps usage of external classes to the number of times they are used
	private Map<String, Integer> externalUsage;
	
	
	private VersionFactory()
	{ }

	public static VersionFactory getInstance()
	{
		if (instance == null) instance = new VersionFactory();
		return instance;
	}
	
	/**
	 * Indicates whether the version with the given RSN for a particular system
	 * has been extracted
	 * @param system The software system the version belongs to
	 * @param rsn The versions Release Sequence Number
	 * @return Whether the version with the specified RSN has been extracted previously
	 */
	public boolean versionExtracted(String system, int rsn)
	{
		//TODO: Use Config Manager to load Reader class
		IVersionReader versionReader = VersionReaderFactory.getInstance().getVersionReader();
		return versionReader.versionExtracted(system, rsn);
	}
	
	/**
	 * Uses a VersionReader to obtain a pre-extracted Version object
	 * @param system The software system the version belongs to
	 * @param rsn The versions Release Sequence Number
	 * @return The Version object that was loaded
	 */
	public Version getVersion(String system, int rsn)
	{
		//TODO: Use Config Manager to load Reader class
		IVersionReader versionReader = VersionReaderFactory.getInstance().getVersionReader();
		
		//TODO: Log de-serialization time info
		Version version = versionReader.readVersion(system, rsn); 
		return version;
	}
	
	/**
	 * Gets a version by indicating an InputDataSet containing it's classes and extracting classes from the data set
	 * @param inputData The InputDataSet containing the classes
	 * @param rsn The versions Release Sequence Number
	 * @param versionId The versions ID
	 * @param includePackages Specific packages to include in the extraction process
	 * @param excludePackages Specific packages to exclude in the extraction process
	 * @return The Version object extracted from the InputDataSet
	 * @throws IOException if there was an error reading from the InputDataSet
	 */
	public Version getVersion(InputDataSet inputData, int rsn, String versionId, Set<String> includePackages, Set<String> excludePackages) throws IOException
	{
		this.inputData = inputData;
		this.rsn = rsn;
		this.versionId = versionId;
		this.includePackages = includePackages;
		this.excludePackages = excludePackages;
		
		return extractVersion();
	}
	
	/**
	 * Extracts and post-processes all classes that make up the version
	 * @return The Version extracted following the processing of all classes
	 * @throws IOException if the InputDataSet could not be read from
	 */
	private Version extractVersion() throws IOException
	{
		classes = new HashMap<String, ClassMetricData>(inputData.size());
		externalUsage = new HashMap<String, Integer>();
		
		//TODO: Incorporate status changes
		// Extract raw class metrics
		extractClasses();
		
		// Perform post-processing once all classes have been loaded
		postProcessClasses();

		// Garbage collect to remove loaded files from memory
		long lastModifiedTime = inputData.lastModTime;
		inputData = null;
		
		return new Version(classes, rsn, versionId, lastModifiedTime);
	}

	/**
	 * Extracts all classes from the InputDataSet specified and stores them in the
	 * Class Name -> ClassMetricData map
	 * 
	 * @return Class Name -> ClassMetricData map containing extracted class information
	 */
	private Map<String, ClassMetricData> extractClasses() throws IOException
	{
		ClassMetricDataFactory classFactory = ClassMetricDataFactory.getInstance();

		//For each class
		for (InputStream classStream : inputData)
		{
			try
			{
				//Get the CLassMetricData object containing information about the current class
				ClassMetricData classMetricData = classFactory.getClassMetricData(classStream);

				//If there are exclude packages, check if the current class is to be skipped
				if (excludePackages != null && excludePackages.size() > 0)
				{
					boolean mustSkip = false;

					//Check classes package name against exclude packages
					for (String excludePackage : excludePackages)
						if (classMetricData.getMetaDataValue(EClassMetricName.PACKAGE_NAME).contains(excludePackage))
							mustSkip = true;

					if (mustSkip) continue;
				}

				//If there are include packages, check if the current class is to be skipped
				if (includePackages != null && includePackages.size() > 0) 
				{
					//Check classes package name against exclude packages
					for (String includedPackage : includePackages)
						if (classMetricData.getMetaDataValue(EClassMetricName.PACKAGE_NAME).contains(includedPackage))
							classes.put(classMetricData.getClassName(), classMetricData);
				}
				else //No include packages specified, assume the class is to be processed
					classes.put(classMetricData.getClassName(), classMetricData);
				
				classStream.close();
			}
			catch (IOException ioe)
			{
				// TODO: Log error
				System.err.println("Error processing Input Stream from IDS...skipping class");
				continue;
			}
		}

		return classes;
	}

	/**
	 * Post-processes all classes as a whole, following their initial extraction
	 */
	private void postProcessClasses()
	{
		// The order of these two method calls matters -- will yield diff. results
		//Update the external calls made by classes
		updateExternalCallCount();
		//Merge inner classes into parent class
		mergeInnerClasses();

		//Compute the dependencies held by classes
		computeDependencies();
		//Compute the layers at which classes reside
		computeLayers();
		//Compute the classes considered GUI and IO
		computeGUIAndIOClasses();
		//Compute the versions clustering coefficient
		computeClusteringCoeff();
		//Compute metrics relating the the inheritence hierarchy
		computeInheritanceMetrics();
		
		//Mark all classes as having been post-processed
		markClassesAsPostProcessed();
	}

	/**
	 * Updates classes external call counts by disseminating between internal/external
	 * library method calls
	 */
	private void updateExternalCallCount()
	{
		//For each class
		for (ClassMetricData classMetricData : classes.values())
		{
			//Get the classes external calls
			Map<String, Integer> externalCalls = classMetricData.getExternalCalls();

			//For each external call
			for (Entry<String, Integer> externalCall : externalCalls.entrySet())
			{
				//Get the external call class name
				String externalCallClassName = externalCall.getKey();
				
				//Get the current call count
				Integer timesCalled = externalCall.getValue();

				//If class that is externally class is not part of processed class set,
				//consider it external, else it is internal
				if (!classes.containsKey(externalCallClassName))
				{
					//Map external call to count as an external library
					classMetricData.getExternalLibraryCalls().put(externalCallClassName, timesCalled);
					classMetricData.incrementMetric(EClassMetricName.EXTERNAL_LIB_METHOD_CALL_COUNT, timesCalled);
				}
				else
				{
					//Map internal call to count as an internal library
					classMetricData.getInternalLibraryCalls().put(StringUtil.getParent(classMetricData.getClassName()), timesCalled);
					classMetricData.incrementMetric(EClassMetricName.INTERNAL_LIB_METHOD_CALL_COUNT, timesCalled);
				}
			}
		}
	}
	
	/**
	 * This should be invoked after entire version data is loaded. It will merge
	 * the inner class metrics into the parent class and remove inner classes
	 * from version measures
	 */
	private void mergeInnerClasses()
	{
		//Create a set to hold inner classes to be removed
		Set<String> innerClassNamesToRemove = new HashSet<String>();

		//For each class
		for (ClassMetricData classMetricData : classes.values())
		{
			//If class is an inner class
			if (classMetricData.getMetricValue(EClassMetricName.IS_INNER_CLASS) == 1)
			{
				//Get the outer classes name
				ClassMetricData outerClass = classes
						.get(classMetricData.getMetaDataValue(EClassMetricName.OUTER_CLASS_NAME));

				//If no outer class found, mark for removal
				if (outerClass == null)
				{
					innerClassNamesToRemove.add(classMetricData.getClassName());
					continue;
				}

				//Merge the inner classes metrics with the outer classes metrics and mark it for
				//removal
				outerClass.mergeMetrics(classMetricData);
				innerClassNamesToRemove.add(classMetricData.getClassName());
			}

			//Remove all redundant dependencies
			removeRedundantDependencies(classMetricData);
		}

		//Remove all the inner classes marked for removal
		for (String innerClassNameToRemove : innerClassNamesToRemove)
			classes.remove(innerClassNameToRemove);
	}

	/**
	 * Removes all redundant dependencies from a given ClassMetricData object
	 * @param classMetricData The ClassMetricData object to remove dependencies from
	 */
	private void removeRedundantDependencies(ClassMetricData classMetricData)
	{
		removePrimitiveDependencies(classMetricData);
		removeClassDependencies(classMetricData);
	}
	
	/**
	 * Removes all dependencies that are considered primitive from the ClassMetricData objects
	 * set of dependencies
	 * @param classMetricData The ClassMetricData object to remove dependencies from
	 */
	private void removePrimitiveDependencies(ClassMetricData classMetricData)
	{
		classMetricData.removeDependency("java/lang/Object");
		classMetricData.removeDependency("java/lang/String");
		classMetricData.removeDependency("java/lang/Double");
		classMetricData.removeDependency("java/lang/Integer");
		classMetricData.removeDependency("java/lang/Float");
		classMetricData.removeDependency("java/lang/Byte");
		classMetricData.removeDependency("java/lang/Character");
		classMetricData.removeDependency("java/lang/StringBuffer");
		classMetricData.removeDependency("java/lang/Short");
	}

	/**
	 * Removes dependencies (inner classes, array type, field types) that are extracted with qualifying information
	 * from the bytecode for the given ClassMetricData object. Will also add any data types that it is dependent upon
	 * that are embedded within the qualified dependencies
	 * @param classMetricData The ClassMetricData object to remove qualified class dependencies from
	 */
	//TODO: Revise -- Might be able to remove this as the same is done within class extraction 
	private void removeClassDependencies(ClassMetricData classMetricData)
	{
		// check for inner class dependencies and remove them
		HashSet<String> dependenciesToRemove = new HashSet<String>();
		HashSet<String> dependenciesToAdd = new HashSet<String>();

		for (String dependency : classMetricData.getDependencies())
		{
			//Remove if inner class
			if (dependency.contains("$")) dependenciesToRemove.add(dependency);
			
			//If type is an array
			if (dependency.startsWith("["))
			{
				//Indicates that the array type is non-primitive and should be added separately
				if (dependency.endsWith(";"))
				{
					//Add the type associated with the array as a dependency
            		String dependencyName = dependency.substring(2, dependency.length()-1); 
                	dependenciesToAdd.add(dependencyName);
				}
				dependenciesToRemove.add(dependency);
			}
			
			//If dependency is a reference
			if (dependency.startsWith("L"))
			{
				//Remove the 'L' from the start and add the type as a dependency
				String dependencyType = dependency.substring(1);
				dependenciesToAdd.add(dependencyType);
				dependenciesToRemove.add(dependency);
			}
		}

		//Add and remove all the dependency flagged for adding and removal
        // The order of the following 2 loops is very important -- DO NOT CHANGE THESE
		for (String dependencyToAdd : dependenciesToAdd)
			classMetricData.addDependency(dependencyToAdd);
		for (String dependencyToRemove : dependenciesToRemove)
			classMetricData.removeDependency(dependencyToRemove);
	}

	/**
	 * Computes dependency-related information for each class
	 */
	private void computeDependencies()
	{
		//For each class
		for (ClassMetricData classMetricData : classes.values())
		{
			setLoadAndStoreMetrics(classMetricData);
			registerInheritanceWithSuperClasses(classMetricData);
			registerInternalDependencies(classMetricData);
		}
		
		//For each class
		for (ClassMetricData classMetricData : classes.values())
		{
			//Set in/out degree counts
			setInOutDegreeCounts(classMetricData);
			//Merge classes external usage with versions
			registerExternalClassUsage(classMetricData);

			//Update the distance the class has moved
			classMetricData.setMetricValue(EClassMetricName.DISTANCE_MOVED,
											MetricUtil.computeDistanceMoved(classMetricData));

			//Merge classes internal usage with versions 
			registerInternalClassUsage(classMetricData);
		}
	}

	/**
	 * Sets load and store related metrics for the given ClassMetricData object
	 * @param classMetricData The ClassMetricData object to set the metrics for
	 */
	private void setLoadAndStoreMetrics(ClassMetricData classMetricData)
	{
		//Get the total number of loads and set the value
		int loadCount = classMetricData.getMetricValue(EClassMetricName.I_LOAD_COUNT)
				+ classMetricData.getMetricValue(EClassMetricName.LOAD_FIELD_COUNT)
				+ classMetricData.getMetricValue(EClassMetricName.REF_LOAD_OP_COUNT)
				+ classMetricData.getMetricValue(EClassMetricName.CONSTANT_LOAD_COUNT);
		classMetricData.setMetricValue(EClassMetricName.LOAD_COUNT, loadCount);

		//Get the total number of stores and set the value
		int storeCount = classMetricData.getMetricValue(EClassMetricName.I_STORE_COUNT)
				+ classMetricData.getMetricValue(EClassMetricName.STORE_FIELD_COUNT)
				+ classMetricData.getMetricValue(EClassMetricName.REF_STORE_OP_COUNT)
				+ classMetricData.getMetricValue(EClassMetricName.INITIALIZED_FIELD_COUNT);
		classMetricData.setMetricValue(EClassMetricName.STORE_COUNT, storeCount);
		
		//Calculate and set the ratio of loads to stores
		int loadRatio = (int) (((double) loadCount / (loadCount + storeCount)) * 10);
		classMetricData.setMetricValue(EClassMetricName.LOAD_RATIO, loadRatio);
	}

	/**
	 * Registers a class as a user of it's superclass and interfaces
	 * @param classMetricData The ClassMetricData object to register
	 */
	private void registerInheritanceWithSuperClasses(ClassMetricData classMetricData)
	{
		//Add the class as a user of it's super class (if it has one)
		ClassMetricData superClass = classes.get(classMetricData.getMetaDataValue(EClassMetricName.SUPER_CLASS_NAME));
		if (superClass != null) superClass.addChild(classMetricData.getClassName());

		//Add the class as a user of each of it's interfaces
		for (String interfaceName : classMetricData.getInterfaces())
		{
			ClassMetricData implementedInterface = classes.get(interfaceName);
			
			//Only add interface if it is part of the system being analysed
			if (implementedInterface != null)
				implementedInterface.addChild(classMetricData.getClassName());
		}
	}
	
	/**
	 * Registers the given class as a user of each of the classes it is dependent upon
	 * provided they are a part of the system being analysed
	 * 
	 * @param classMetricData The ClassMetricData object to register as a dependent
	 */
	private void registerInternalDependencies(ClassMetricData classMetricData)
	{
		for (String dependencyName : classMetricData.getDependencies())
		{
			//Ignore self-dependencies
			if (dependencyName.equals(classMetricData.getClassName())) continue;
			
			//Get the class being depended upon
			ClassMetricData fanInNode = classes.get(dependencyName);
			
			//If the class is part of the system being analysed
			if (fanInNode != null)
			{
				//Add class being depended upon as internal dependency
				classMetricData.addInternalDependency(dependencyName);
				//Add class as a user of dependency
				fanInNode.addUser(classMetricData.getClassName());
			}
		}
	}
	
	/**
	 * Set the in/out degree counts and internal/external out degree counts for the given ClassMetricData
	 * object
	 * @param classMetricData The ClassMetricData object to set the counts for
	 */
	private void setInOutDegreeCounts(ClassMetricData classMetricData)
	{
		//Set in/out degree count based on user/dependency count
		classMetricData.setMetricValue(EClassMetricName.IN_DEGREE_COUNT, classMetricData.getUserCount());
		classMetricData.setMetricValue(EClassMetricName.OUT_DEGREE_COUNT, classMetricData.getDependencyCount());
		
		//Set internal/external out degree count based on internal/external dependency count
		classMetricData.setMetricValue(EClassMetricName.INTERNAL_OUT_DEGREE_COUNT, classMetricData
				.getInternalDependencyCount());
		classMetricData.setMetricValue(EClassMetricName.EXTERNAL_OUT_DEGREE_COUNT, classMetricData
				.getExternalCallCount());
	}
	
	/**
	 * Merges the external class usage for the version with a given ClassMetricData object
	 * 
	 * @param classMetricData The ClassMetricData object to merge external class usage with
	 */
	private void registerExternalClassUsage(ClassMetricData classMetricData)
	{
		//For each external class in the classes set
		for (String externalClass : classMetricData.getExternalCalls().keySet())
		{
			//Skip if external class is actually internal
			if (classes.containsKey(externalClass)) continue;
			
			//If external class already register, update the count,
			//else register and set call count
			if (externalUsage.containsKey(externalClass))
			{
				int usageCount = externalUsage.get(externalClass);
				externalUsage
						.put(externalClass, classMetricData.getExternalCalls().get(externalClass) + usageCount);
			}
			else
			{
				externalUsage.put(externalClass, classMetricData.getExternalCalls().get(externalClass)); // first encounter
			}

			//Increment classes usage of of java.util package classes
			if (externalClass.startsWith("java/util"))
				classMetricData.incrementMetric(EClassMetricName.JAVA_UTIL_OUT_DEGREE_COUNT, classMetricData
						.getExternalCalls().get(externalClass));
		}
	}
	
	/**
	 * Register the amount of times a class uses other internal classes
	 * 
	 * @param classMetricData The ClassMetricData object to register internal usage for 
	 */
	private void registerInternalClassUsage(ClassMetricData classMetricData)
	{
		//For each internal library class that the class uses
		for (String internalClassName : classMetricData.getInternalLibraryCalls().keySet())
		{
			//Get the internal class being used
			ClassMetricData internalClass = classes.get(internalClassName);

			if (internalClass == null)
				System.out.println("NULL  FOUND: " + classMetricData.getClassName()
						+ "\t" + internalClassName); // TODO: Remove/log error
			else
			{
				//Increment the number of times the internal class is used by the number
				//of times it is used by the class
				Integer timesUsed = classMetricData.getInternalLibraryCalls().get(internalClassName);
				internalClass.incrementMetric(EClassMetricName.USAGE_COUNT, timesUsed); // update the count
			}
		}
	}

	/**
	 * Computes information regarding layers and instability for each class in the version
	 */
	private void computeLayers()
	{
		//For each class
		for (ClassMetricData classMetricData : classes.values())
		{
			int inDegreeCount = classMetricData.getMetricValue(EClassMetricName.IN_DEGREE_COUNT);
			int outDegreeCount = classMetricData.getMetricValue(EClassMetricName.OUT_DEGREE_COUNT);
			int internalOutDegreeCount = classMetricData.getMetricValue(EClassMetricName.INTERNAL_OUT_DEGREE_COUNT);

			//Calculate and set the classes instability value
			int instability = (int) (((double) outDegreeCount / (outDegreeCount + inDegreeCount)) * 1000);
			classMetricData.setMetricValue(EClassMetricName.INSTABILITY, instability);

			int layer = 0;
			
			//Determine the layer that the class resides at
			if ((inDegreeCount > 0) && (internalOutDegreeCount > 0))
				layer = 1; // mid
			else if ((inDegreeCount == 0) && (internalOutDegreeCount > 0))
				layer = 2; // top
			else if ((inDegreeCount > 0) && (internalOutDegreeCount == 0))
				layer = 0; // foundation
			else if ((inDegreeCount == 0) && (internalOutDegreeCount == 0))
				layer = 3; // free
			else
				layer = 4; // there should be no classes here, hopefully

			//Set the layer that the class resides at
			classMetricData.setMetricValue(EClassMetricName.LAYER, layer);
		}
	}

	/**
	 * Recursively flags all classes that are determined to be GUI and I/O
	 */
	private void computeGUIAndIOClasses()
	{
		// Flag all GUI classes
		Set<String> flagged = classes.keySet();
		while (flagged.size() > 0)
			flagged = flagUsersAsGUI(flagged);

		// Flag all IO classes
		flagged = classes.keySet();
		while (flagged.size() > 0)
			flagged = flagUsersAsIO(flagged);

	}

	/**
	 * Flags all classes that are users of an GUI class based on a given set of classes
	 * 
	 * @param classNameSet The classes to process
	 * @return The set of classes that were flagged as GUI
	 */
	private Set<String> flagUsersAsGUI(Set<String> classNameSet)
	{
		Set<String> flagged = new HashSet<String>();
		
		//For each class in the input set
		for (String className : classNameSet)
		{
			//Get classes GUI distance 
			double currentGUIDistance = classes.get(className).getMetricValue(EClassMetricName.IS_GUI_CLASS);
			
			//If GUI distance > 0, class is a GUI class
			if (currentGUIDistance != 0)
			{
				//For each of the classes users
				for (String userClassName : classes.get(className).getUsers())
				{
					//If not flagged as a GUI class, do so now
					if (classes.get(userClassName).getMetricValue(EClassMetricName.IS_GUI_CLASS).intValue() == 0)
					{
						classes.get(userClassName).setMetricValue(EClassMetricName.IS_GUI_CLASS, 1);
						flagged.add(userClassName);
					}
				}
			}
		}
		return flagged;
	}
	
	/**
	 * Flags all classes that are users of an I/O class based on a given set of classes
	 * 
	 * @param classNameSet The classes to process
	 * @return The set of classes that were flagged as I/O
	 */
	private Set<String> flagUsersAsIO(Set<String> classNameSet)
	{
		Set<String> flagged = new HashSet<String>();
		
		//For each class in the input set
		for (String className : classNameSet)
		{
			//If class is an I/O
			if (classes.get(className).getMetricValue(EClassMetricName.IS_IO_CLASS) == 1)
			{
				//For each of the classes users
				for (String userClassName : classes.get(className).getUsers())
				{
					//If not already flag as I/O, do so now
					if (classes.get(userClassName).getMetricValue(EClassMetricName.IS_IO_CLASS) == 0)
					{
						classes.get(userClassName).setMetricValue(EClassMetricName.IS_IO_CLASS, 1);
						flagged.add(userClassName);
					}
				}
			}
		}

		return flagged;
	}

	/**
	 * Computes the clustering coefficient for a class based on it's interconnections
	 */
	public void computeClusteringCoeff()
	{
		// for each class in the version compute the clustering coefficient
		for (String className : classes.keySet())
		{
			//Get the class
			ClassMetricData classMetricData = classes.get(className);
			
			//Get the number of internal dependencies
			int neighbourCount = classMetricData.getInternalDependencyCount();
			
			//Determine the maximum neighbourhood size
			int maxNeighbourhoodSize = (neighbourCount - 1) * neighbourCount;

			//Get the number of interconnections
			int interConnections = countInterconnections(classMetricData.getInternalDependencies());

			double clusteringCoeff = 0.0;

			//If there is a max neighbourhood size, calculate the clustering coefficient value
			if (maxNeighbourhoodSize > 0) clusteringCoeff = (double)interConnections / (double)maxNeighbourhoodSize;

			//Set the clustering coefficient value
			classMetricData.setMetricValue(EClassMetricName.CLUSTERING_COEFF,
											(int) MathUtil.round(clusteringCoeff * 10));
		}

	}

	// TODO: Maybe move to StatsUtil
	/**
	 * Count the number of internal directed connections between the set of
	 * classes provided
	 */
	private int countInterconnections(Set<String> classNames)
	{
		int interconnectionCount = 0;
		
		for (String className : classNames)
		{
			// get the neighbours for each class
			Set<String> neighbours = classes.get(className).getInternalDependencies();

			//For each neighbour
			for (String neighbourClassName : neighbours)
			{
				//Ignore self links
				if (neighbourClassName.equals(className)) continue;

				//If neighbours are in the original set, then they are
				//connected, increment count
				if (classNames.contains(neighbourClassName)) interconnectionCount++;
			}
		}
		// TODO: Move this out of here
		// if (verbose) System.out.println(out);
		return interconnectionCount;
	}

	/**
	 * Compute the inheritance-related metrics (depth in inheritance tree, no. of children
	 * no. of descendants) for each class
	 */
	private void computeInheritanceMetrics()
	{
		//For each class
		for (ClassMetricData classMetricData : classes.values())
		{
			//Get the classes depth in the inheritance tree
			int depthInTree = getDepthInTree(classMetricData);

			//Get the classes interface depth in tree
			Set<Integer> interfaceDepth = new HashSet<Integer>();
			getInterfaceDepthInTree(classMetricData, interfaceDepth, 0);

			//If interface depth > inheritance depth, set depth in tree
			//to interface depth
			if (depthInTree < interfaceDepth.size()) depthInTree = interfaceDepth.size();

			//Get the no. of children the class has
			int noOfChildren = classMetricData.getChildCount();
			
			//Get the no. of descendants the class has
			Set<String> descendants = new HashSet<String>();
			getNoOfDescendants(classMetricData, descendants);
			int noOfDescendants = descendants.size();

			//Set the inheritance-related metric values
			classMetricData.setMetricValue(EClassMetricName.DEPTH_IN_INHERITANCE_TREE, depthInTree);
			classMetricData.setMetricValue(EClassMetricName.NUMBER_OF_CHILDREN, noOfChildren);
			classMetricData.setMetricValue(EClassMetricName.NUMBER_OF_DESCENDANTS, noOfDescendants);
		}
	}

	/**
	 * Recursively calculates the a classes depth in the inheritance tree
	 * 
	 * The algorithm here is not quite perfect, as we need to realistically walk
	 * the entire inheritance tree including multiple interface paths, but that
	 * is not being done here
	 */
	//TODO: Extend this methods capabilities according to description provided
	private int getDepthInTree(ClassMetricData classMetricData)
	{
		//Return 0 if inheriting directly from Object
		if (classMetricData.getMetaDataValue(EClassMetricName.SUPER_CLASS_NAME).equals("java/lang/Object")) return 0;

		//Get the classes current DIT value
		int currentDIT = classMetricData.getMetricValue(EClassMetricName.DEPTH_IN_INHERITANCE_TREE);
		
		//If current value, return value as it has previously been calculated
		if (currentDIT > 0) return currentDIT;

		//Get the classes superclass
		ClassMetricData superClass = classes.get(classMetricData.getMetaDataValue(EClassMetricName.SUPER_CLASS_NAME));

		//If super class is not internal, treat the depth as inheriting directly from Object,
		//else get it's depth
		if (superClass == null)
			return 1;
		else
			return getDepthInTree(superClass) + 1;
	}

	/**
	 * Recursively calculates a classes interface depth in the inheritance tree
	 * @param classMetricData The class to determine the depth for
	 * @param depth The interface depth set
	 * @param level The depth level
	 */
	//TODO: Revise this calculation...the depth set elements are not being accessed anywhere
	private void getInterfaceDepthInTree(ClassMetricData classMetricData, Set<Integer> depth, int level)
	{
		//Null class, return
		if (classMetricData == null) return;

		//No interfaces found, return
		if (classMetricData.getInterfaceCount() == 0) return;

		//Add a new level of depth
		depth.add(level + 1);

		//For each interface in the classes set
		for (String interfaceName : classMetricData.getInterfaces())
		{
			//Get the interface class
			ClassMetricData superInterface = classes.get(interfaceName);

			//If interface is external, skip,
			//else recursively process using interface class
			if (superInterface == null)
				continue;
			else
				getInterfaceDepthInTree(superInterface, depth, level + 1);
		}
	}
	
	/**
	 * Recursive method to compute the number of descendants by walking children
	 * @param classMetricData The class to extract descendants for
	 * @param descendants The classes set of descendants
	 * @return The number of descendants a class has
	 */
	private int getNoOfDescendants(ClassMetricData classMetricData, Set<String> descendants)
	{
		//No children, escape recursion
		if (classMetricData.getChildCount() == 0) return 0;

		//Get number of children
		int noOfDescendants = classMetricData.getChildCount();

		//For each child
		for (String childName : classMetricData.getChildren())
		{
			//Get the child 
			ClassMetricData child = classes.get(childName);			
			//Add as a descendant to the class
			descendants.add(child.getClassName());
			//Increment count based on recursively determined count
			noOfDescendants += getNoOfDescendants(child, descendants);
		}
		
		return noOfDescendants;
	}
	
	/**
	 * Marks each class within the version (processing status) as having been post-processed
	 */
	private void markClassesAsPostProcessed()
	{
		//For each class
		for(ClassMetricData classMetricData : classes.values())
			classMetricData.setProcessingStatus(EProcessingStatus.POST_PROCESSED); //Class was post-processed
	}
}