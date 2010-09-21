package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import model.vocab.EClassMetricName;
import model.vocab.EEvolutionCategory;
import model.vocab.EModificationStatus;
import model.vocab.EProcessingStatus;

//TODO: Last 4 members require further explanation
/**
 * Represents a container for a number of metrics and meta-data related to a Java class
 * 
 * This includes:
 * 	- Meta-data: 				String-based properties (class name, super class name, etc.)
 *  - Basic metrics: 			Integer-based metrics (method count, public method count, field count, etc.)
 *  - Method Data: 				Metric data for each of the methods within the class
 *  - Methods: 					Names of the methods within the class (name is a combination of method name and bytecode signature)
 *  - Short Methods: 			Short names of the methods within the class (short name is a combination of method name and parameters)
 *  - Fields: 					Names of the fields within the class
 *  - Dependencies: 			Names of the classes that the class depends upon
 *  - Users: 					Names of the classes that use the class
 *  - Children: 				Names of the classes that inherit from the class
 *  - Interfaces: 				Names of the interfaces that the class implements
 *  - Internal dependencies:	Names of the dependencies the class has that are considered internal
 *  - External calls:			Names of the external classes the class calls and the number of times they are called
 *  - Internal library calls:	Names of the internal library classes the class calls and the number of times they are called
 *  - External library calls:	Names of the external library classes the class calls and the number of times they are called
 * 
 * @author Allan Jones
 *
 */
public class ClassMetricData
{
	//Meta-data associated with the class
	private Map<EClassMetricName, String> metaData = new HashMap<EClassMetricName, String>(20);
	//Metric values for the class
	private Map<EClassMetricName, Integer> metrics;
	
	//The names of the methods within the class
	private Set<String> methods = new HashSet<String>();
	//The short names of the methods within the class
	private Set<String> shortMethods = new HashSet<String>();
	//The names of the fields within the class
	private Set<String> fields = new HashSet<String>();
	
	//The names of the classes that the class depends on (includes library classes)
	private Set<String> dependencies = new HashSet<String>();
	//The names of the classes that use the class
	private Set<String> users = new HashSet<String>();
	//The names of the classes that inherit from this class
	private Set<String> children = new HashSet<String>();
	//The names of the interfaces that the class implements
	private Set<String> interfaces = new HashSet<String>();    
    
	//The names of the classes considered internal (non-library classes) that the class depends upon
	private Set<String> internalDependencies = new HashSet<String>();
	//The names of the classes external to the class that are called and the number of times they are called 
	private Map<String, Integer> externalCalls = new HashMap<String, Integer>();
	//The names of the classes that are considered internal (TODO: Define what is considered internal) and the number of times they are called
	private Map<String, Integer> internalLibraryCalls = new HashMap<String, Integer>();
	//The names of the classes that are considered external (TODO: Define what is considered external) and the number of times they are called
	private Map<String, Integer> externalLibraryCalls = new HashMap<String, Integer>();
	
	//The extent to which this class has been processed (TODO: Add explanation of possible values and when they are set)
	private EProcessingStatus processingStatus = EProcessingStatus.UNPROCESSED;
	
	/**
	 * Constructs a new ClassMetricData object with initialised metric values
	 */
	public ClassMetricData()
	{
		initialiseMetricMap();
	}
	
	/**
	 * Initialises the values of the metric map to sensible values
	 */
	private void initialiseMetricMap()
	{
		//Create the metric map with space for 100 metrics
		metrics = new HashMap<EClassMetricName, Integer>(100);
		
		//Get the list of metric names
		EClassMetricName[] names = EClassMetricName.values();
    	
		//Set the value of each metric to 0
    	for(EClassMetricName name : names)
    		if(name != EClassMetricName.UNKNOWN)
    			metrics.put(name, 0);
    	
    	
    	//Set the age to 1 (this assumes that the class is new -- value is likely to change in processing)
    	metrics.put(EClassMetricName.AGE, 1);
    	//Set the RSN the class was new in to 1 (this assumes that the class has been present since the start -- value is likely to change in processing)
    	metrics.put(EClassMetricName.BORN_RSN, 1); //Assume in first version
    	
    	//Set the Evolution Status to 'Unchanged' (this assumes it is unchanged unless otherwise specified)
    	metrics.put(EClassMetricName.EVOLUTION_STATUS, EEvolutionCategory.UNCHANGED.ordinal());
    	//Set the Next Version Status to 'Unchanged' (this assumes it is unchanged unless otherwise specified)
    	metrics.put(EClassMetricName.NEXT_VERSION_STATUS, EEvolutionCategory.UNCHANGED.ordinal()); //Unknown initially
    	
    	//Set the Distance Moved as unknown
    	metrics.put(EClassMetricName.DISTANCE_MOVED, -1); //Unknown initially
    	//Set the Distance Moved as unknown
    	metrics.put(EClassMetricName.DISTANCE_MOVED_SINCE_BIRTH, -1); //Unknown initially
    	
    	//Set the status of modification since birth to 'Never Modified' (this assumes the class has never changed -- value is likely to change in processing)
    	metrics.put(EClassMetricName.MODIFICATION_STATUS_SINCE_BIRTH, EModificationStatus.NEVER_MODIFIED.ordinal()); //Unknown initially
	}
	
	public Map<EClassMetricName, String> getMetaData()
	{
		return metaData;
	}
	
	public void setMetaData(Map<EClassMetricName, String> metaData)
	{
		if(metaData == null)
			throw new NullPointerException("Could not set meta data map, given value was null.");
		
		this.metaData = metaData;
	}
	
	public String getMetaDataValue(EClassMetricName metric)
	{
		if(metric == null)
			throw new NullPointerException("Could not get meta data value, given metric name was null.");
		
		return metaData.get(metric);
	}
	
	public void setMetaDataValue(EClassMetricName metric, String value)
	{
		if(metric == null)
			throw new NullPointerException("Could not set metric value, given metric to set was null.");
		
		if(value == null)
			throw new NullPointerException("Could not set metric value, given value was null.");
		
		metaData.put(metric, value);
	}
	
	public Map<EClassMetricName, Integer> getMetrics()
	{
		return metrics;
	}
	
	public void setMetrics(Map<EClassMetricName, Integer> metrics)
	{
		if(metrics == null)
			throw new NullPointerException("Could not set metrics map, given value was null.");
		
		this.metrics = metrics;
	}
	
	public Integer getMetricValue(EClassMetricName metric)
	{
		if(metric == null)
			throw new NullPointerException("Could not get metric value, given metric name was null.");
		
		return metrics.get(metric);
	}
	
	public void setMetricValue(EClassMetricName metric, int value)
	{
		MetricUtil.setMetricValue(metrics, metric, value);
	}
	
	public String getClassName()
	{
		return getMetaDataValue(EClassMetricName.CLASS_NAME);
	}
	
	public String getShortClassName()
	{
		return metaData.get(EClassMetricName.SHORT_CLASS_NAME);
	}
	
	public String getPackageName()
	{
		return getMetaDataValue(EClassMetricName.PACKAGE_NAME);
	}
	
	private String getSuperClassName()
	{
		return getMetaDataValue(EClassMetricName.SUPER_CLASS_NAME);
	}
	
	//TODO: This should probably be pre-calculated and stored as meta-data
	/**
	 * Determines the classes type, based upon metrics extracted (normal, abstract, interface)
	 */
	public String getClassType()
	{
		String classType = "C";
		
		if (getMetricValue(EClassMetricName.IS_ABSTRACT) == 1) classType = "A";
		if (getMetricValue(EClassMetricName.IS_INTERFACE) == 1) classType = "I";
		
		return classType;
	}
	
	public Set<String> getMethods()
	{
		return methods;
	}
	
	public void setMethods(Set<String> methods)
	{
		if(methods == null)
			throw new NullPointerException("Could not set methods for " + getClassName() + ", the set passed was null.");
		
		this.methods = methods;
	}
	
	public Set<String> getShortMethods()
	{
		return shortMethods;
	}
	
	public void setShortMethods(Set<String> shortMethods)
	{
		if(shortMethods == null)
			throw new NullPointerException("Could not set short methods for " + getClassName() + ", the set passed was null.");
		
		this.shortMethods = shortMethods;
	}
	
	public Set<String> getFields()
	{
		return fields;
	}
	
	public void setFields(Set<String> fields)
	{
		if(methods == null)
			throw new NullPointerException("Could not set fields for " + getClassName() + ", the set passed was null.");
		
		this.fields = fields;
	}
	
	public Set<String> getDependencies()
	{
		return dependencies;
	}
	
	public void setDependencies(Set<String> dependencies)
	{
		if(dependencies == null)
			throw new NullPointerException("Could not set dependencies for " + getClassName() + ", the set passed was null.");
		
		this.dependencies = dependencies;
	}
	
	public Set<String> getUsers()
	{
		return users;
	}	
	
	public void setUsers(Set<String> users)
	{
		if(users == null)
			throw new NullPointerException("Could not set users for " + getClassName() + ", the set passed was null.");
		
		this.users = users;
	}

	public Set<String> getChildren()
	{
		return children;
	}
	
	public void setChildren(Set<String> children)
	{
		if(children == null)
			throw new NullPointerException("Could not set methods for " + getClassName() + ", the set passed was null.");
		
		this.children = children;
	}

	public Set<String> getInterfaces()
	{
		return interfaces;
	}
	
	public void setInterfaces(Set<String> interfaces)
	{
		if(interfaces == null)
			throw new NullPointerException("Could not set interfaces for " + getClassName() + ", the set passed was null.");
		
		this.interfaces = interfaces;
	}
	
	public Set<String> getInternalDependencies()
	{
		return internalDependencies;
	}
	
	public void setInternalDependencies(Set<String> internalDependencies)
	{
		if(internalDependencies == null)
			throw new NullPointerException("Could not set internal dependencies for " + getClassName() + ", the set passed was null.");
		
		this.internalDependencies = internalDependencies;
	}
	
	public Map<String, Integer> getExternalCalls()
	{
		return externalCalls;
	}
	
	public void setExternalCalls(Map<String, Integer> externalCalls)
	{
		if(externalCalls == null)
			throw new NullPointerException("Could not set external calls for " + getClassName() + ", the map passed was null.");
		
		this.externalCalls = externalCalls;
	}
	
	public Map<String, Integer> getInternalLibraryCalls()
	{
		return internalLibraryCalls;
	}
	
	public void setInternalLibraryCalls(Map<String, Integer> internalLibraryCalls)
	{
		if(internalLibraryCalls == null)
			throw new NullPointerException("Could not set internal library calls for " + getClassName() + ", the map passed was null.");
		
		this.internalLibraryCalls = internalLibraryCalls; 
	}
	
	public Map<String, Integer> getExternalLibraryCalls()
	{
		return externalLibraryCalls;
	}
	
	public void setExternalLibraryCalls(Map<String, Integer> externalLibraryCalls)
	{
		if(externalLibraryCalls == null)
			throw new NullPointerException("Could not set external library calls for " + getClassName() + ", the map passed was null.");
		
		this.externalLibraryCalls = externalLibraryCalls; 
	}

	public EProcessingStatus getProcessingStatus()
	{
		return processingStatus;
	}
	
	public void setProcessingStatus(EProcessingStatus processingStatus)
	{
		if(processingStatus == null)
			throw new NullPointerException("Could not set processing status for " + getClassName() + ", the status passed was null.");
		
		this.processingStatus = processingStatus;
	}
	
	public int getMethodCount()
	{
		return methods.size();
	}
	
	public int getShortMethodCount()
	{
		return shortMethods.size();
	}
	
	public int getFieldCount()
	{
		return fields.size();
	}
	
	public int getDependencyCount()
	{
		return this.dependencies.size();
	}
	
	public int getUserCount()
	{
		return users.size();
	}
	
	public int getChildCount()
	{
		return children.size();
	}
	
	public int getInterfaceCount()
	{
		return interfaces.size();
	}
	
	public int getInternalDependencyCount()
	{
		return internalDependencies.size();
	}
	
	public int getExternalCallCount()
	{
		return externalCalls.keySet().size();
	}
	
	/**
	 * Adds a method to the set of methods that belong to the class
	 * @param methodName The name of the method to add to the classes set
	 */
	public void addMethod(String methodName)
	{
		if(methodName == null)
			throw new NullPointerException("Could not add method to " + getClassName() + ", the method name passed was null.");
		
		methods.add(methodName);
	}
	
	/**
	 * Adds a short-named method to the set of short-named methods that belong to the class
	 * @param shortMethodName The short name of the method to add to the classes set
	 */
	public void addShortMethod(String shortMethodName)
	{
		if(shortMethodName == null)
			throw new NullPointerException("Could not add short method to " + getClassName() + ", the short method name passed was null.");
		
		shortMethods.add(shortMethodName);
	}
	
	/**
	 * Adds a field to the set of fields that belong to the class
	 * @param field The field to add to the classes set
	 */
	public void addField(String field)
	{
		if(field == null)
			throw new NullPointerException("Could not add field to " + getClassName() + ", the field passed was null.");
		
		fields.add(field);
	}
	
	/**
	 * Adds a class that the class is dependent upon to it's set of dependencies
	 * @param dependency The dependency to add to the classes set
	 */
	public void addDependency(String dependency)
	{
		if(dependency == null)
			throw new NullPointerException("Could not add dependency to " + getClassName() + ", the dependency passed was null.");
		
		dependencies.add(dependency);
	}
	
	/**
	 * Removes a class from the set of those which the class is dependent upon
	 * @param dependency The dependency to remove from the classes set
	 */
	public void removeDependency(String dependency)
	{
		if(dependency == null)
			throw new NullPointerException("Could not remove dependency from " + getClassName() + ", the dependency passed was null.");
		
//		if(!dependencies.contains(dependency))
//			throw new NoSuchElementException("Could not remove dependency from " + getClassName() + ", the dependency was not found in the classes set.");
		
		dependencies.remove(dependency);
	}
	
	/**
	 * Adds a class that uses the class to it's set of users
	 * @param user The user to add to the classes set
	 */
	public void addUser(String user)
	{
		if(user == null)
			throw new NullPointerException("Could not add user to " + getClassName() + ", the user passed was null.");
		
		users.add(user);
	}
	
	/**
	 * Adds a class to the set of classes that inherit from the class
	 * @param child The child to add to the classes set
	 */
	public void addChild(String child)
	{
		if(child == null)
			throw new NullPointerException("Could not add child to " + getClassName() + ", the child passed was null.");
		
		children.add(child);
	}
	
	/**
	 * Adds a class to the set of interfaces that the class implements
	 * @param interfaceName The interface to add to the classes set
	 */
	public void addInterface(String interfaceName)
	{
		if(interfaceName == null)
			throw new NullPointerException("Could not add interface to " + getClassName() + ", the interface passed was null.");
		
		getInterfaces().add(interfaceName);
	}

	/**
	 * Removes a class from the set of those which the class implements
	 * @param interfaceName The interface to remove from the classes set
	 */
	public void removeInterface(String interfaceName)
	{
		if(interfaceName == null)
			throw new NullPointerException("Could not remove interface from " + getClassName() + ", the interface passed was null.");
	
		if(!interfaceName.contains(interfaceName))
			throw new NoSuchElementException("Could not remove interface from " + getClassName() + ", the interface was not found in the classes set.");
		
		getInterfaces().remove(interfaceName);
	}
	
	/**
	 * Adds a class to the set of internal dependencies that the class relies upon
	 * @param internalDependency The internal dependency to add to the classes set
	 */
	public void addInternalDependency(String internalDependency)
	{
		if(internalDependency == null)
			throw new NullPointerException("Could not remove internal dependency from " + getClassName() + ", the internal dependency passed was null.");
			
		internalDependencies.add(internalDependency);
	}
	
	/**
	 * Increments the value of a given metric by 1
	 * @param metric The metric to increment
	 */
	public void incrementMetric(EClassMetricName metric)
	{
		MetricUtil.incrementMetricValue(metrics, metric);
	}
	
	/**
	 * Increments the value of a metric by a specified valuer
	 * @param metric The metric to increment
	 * @param value The value to increment the metric by
	 */
	public void incrementMetric(EClassMetricName metric, int value)
	{
		MetricUtil.incrementMetricValue(metrics, metric, value);
	}

	/**
	 * Merge the metrics of the class with those of a specified class
	 * @param classToMergeWith The class whose metrics will be merged into the class
	 */
	public void mergeMetrics(ClassMetricData classToMergeWith)
	{
		//Check if class is inner and merge interface count if so (inner class do not implement interfaces)
		if (classToMergeWith.metrics.get(EClassMetricName.IS_INNER_CLASS) != 1)
		{
			this.incrementMetric(EClassMetricName.INTERFACE_COUNT,
					classToMergeWith.metrics.get(EClassMetricName.INTERFACE_COUNT));        	
		}
		
		//TODO: Might be able to extract the individual increments in favour of iterating over ClassMetricName values
		this.incrementMetric(EClassMetricName.BRANCH_COUNT, classToMergeWith.metrics.get(EClassMetricName.BRANCH_COUNT));
    	this.incrementMetric(EClassMetricName.EXTERNAL_METHOD_CALL_COUNT, classToMergeWith.metrics.get(EClassMetricName.EXTERNAL_METHOD_CALL_COUNT));
    	this.incrementMetric(EClassMetricName.INTERNAL_METHOD_CALL_COUNT, classToMergeWith.metrics.get(EClassMetricName.INTERNAL_METHOD_CALL_COUNT));
    	this.incrementMetric(EClassMetricName.STATIC_METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.STATIC_METHOD_COUNT));
    	this.incrementMetric(EClassMetricName.METHOD_CALL_COUNT, classToMergeWith.metrics.get(EClassMetricName.METHOD_CALL_COUNT));  	    	
    	this.incrementMetric(EClassMetricName.METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.METHOD_COUNT));
    	this.incrementMetric(EClassMetricName.INSTRUCTION_COUNT, classToMergeWith.metrics.get(EClassMetricName.INSTRUCTION_COUNT));
        this.incrementMetric(EClassMetricName.PUBLIC_METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.PUBLIC_METHOD_COUNT));
        this.incrementMetric(EClassMetricName.FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.FIELD_COUNT));
        this.incrementMetric(EClassMetricName.INNER_CLASS_COUNT, classToMergeWith.metrics.get(EClassMetricName.INNER_CLASS_COUNT));
        this.incrementMetric(EClassMetricName.THROW_COUNT, classToMergeWith.metrics.get(EClassMetricName.THROW_COUNT));
        this.incrementMetric(EClassMetricName.PARAM_COUNT, classToMergeWith.metrics.get(EClassMetricName.PARAM_COUNT));
        this.incrementMetric(EClassMetricName.EXCEPTION_COUNT, classToMergeWith.metrics.get(EClassMetricName.EXCEPTION_COUNT));
        this.incrementMetric(EClassMetricName.INSTANCE_OF_COUNT, classToMergeWith.metrics.get(EClassMetricName.INSTANCE_OF_COUNT));
        this.incrementMetric(EClassMetricName.CHECK_CAST_COUNT, classToMergeWith.metrics.get(EClassMetricName.CHECK_CAST_COUNT));
        this.incrementMetric(EClassMetricName.NEW_COUNT, classToMergeWith.metrics.get(EClassMetricName.NEW_COUNT));
        this.incrementMetric(EClassMetricName.NEW_ARRAY_COUNT, classToMergeWith.metrics.get(EClassMetricName.NEW_ARRAY_COUNT));
        this.incrementMetric(EClassMetricName.TYPE_CONSTRUCTION_COUNT, classToMergeWith.metrics.get(EClassMetricName.TYPE_CONSTRUCTION_COUNT));
        this.incrementMetric(EClassMetricName.REF_LOAD_OP_COUNT, classToMergeWith.metrics.get(EClassMetricName.REF_LOAD_OP_COUNT)); // Number of reference loads
        this.incrementMetric(EClassMetricName.REF_STORE_OP_COUNT, classToMergeWith.metrics.get(EClassMetricName.REF_STORE_OP_COUNT)); // Number of reference stores
        this.incrementMetric(EClassMetricName.LOAD_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.LOAD_FIELD_COUNT));  // Number of times a field was loaded
        this.incrementMetric(EClassMetricName.STORE_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.STORE_FIELD_COUNT)); // Number of times a field was stored
        this.incrementMetric(EClassMetricName.TRY_CATCH_BLOCK_COUNT, classToMergeWith.metrics.get(EClassMetricName.TRY_CATCH_BLOCK_COUNT)); // Number of try-catch blocks
        this.incrementMetric(EClassMetricName.LOCAL_VAR_COUNT, classToMergeWith.metrics.get(EClassMetricName.LOCAL_VAR_COUNT));
        this.incrementMetric(EClassMetricName.PRIVATE_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.PRIVATE_FIELD_COUNT));
        this.incrementMetric(EClassMetricName.PROTECTED_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.PROTECTED_FIELD_COUNT));
        this.incrementMetric(EClassMetricName.PUBLIC_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.PUBLIC_FIELD_COUNT));
        this.incrementMetric(EClassMetricName.STATIC_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.STATIC_FIELD_COUNT));
        this.incrementMetric(EClassMetricName.FINAL_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.FINAL_FIELD_COUNT));
        this.incrementMetric(EClassMetricName.LOAD_COUNT, classToMergeWith.metrics.get(EClassMetricName.LOAD_COUNT));
        this.incrementMetric(EClassMetricName.STORE_COUNT, classToMergeWith.metrics.get(EClassMetricName.STORE_COUNT));
        this.incrementMetric(EClassMetricName.I_STORE_COUNT, classToMergeWith.metrics.get(EClassMetricName.I_STORE_COUNT));
        this.incrementMetric(EClassMetricName.I_LOAD_COUNT, classToMergeWith.metrics.get(EClassMetricName.I_LOAD_COUNT));
        this.incrementMetric(EClassMetricName.TYPE_INSN_COUNT, classToMergeWith.metrics.get(EClassMetricName.TYPE_INSN_COUNT));
        this.incrementMetric(EClassMetricName.ZERO_OP_INSN_COUNT, classToMergeWith.metrics.get(EClassMetricName.ZERO_OP_INSN_COUNT));
        this.incrementMetric(EClassMetricName.INITIALIZED_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.INITIALIZED_FIELD_COUNT));
        this.incrementMetric(EClassMetricName.UNINITIALIZED_FIELD_COUNT, classToMergeWith.metrics.get(EClassMetricName.UNINITIALIZED_FIELD_COUNT));
        this.incrementMetric(EClassMetricName.FINAL_METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.FINAL_METHOD_COUNT));
        this.incrementMetric(EClassMetricName.PRIVATE_METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.PRIVATE_METHOD_COUNT));
        this.incrementMetric(EClassMetricName.SYNCHRONIZED_METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.SYNCHRONIZED_METHOD_COUNT));
        this.incrementMetric(EClassMetricName.PROTECTED_METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.PROTECTED_METHOD_COUNT));
        this.incrementMetric(EClassMetricName.ABSTRACT_METHOD_COUNT, classToMergeWith.metrics.get(EClassMetricName.ABSTRACT_METHOD_COUNT));
        this.incrementMetric(EClassMetricName.CONSTANT_LOAD_COUNT, classToMergeWith.metrics.get(EClassMetricName.CONSTANT_LOAD_COUNT));
        this.incrementMetric(EClassMetricName.INCREMENT_OP_COUNT, classToMergeWith.metrics.get(EClassMetricName.INCREMENT_OP_COUNT));
        this.incrementMetric(EClassMetricName.INTERNAL_LIB_METHOD_CALL_COUNT, classToMergeWith.metrics.get(EClassMetricName.INTERNAL_LIB_METHOD_CALL_COUNT));
        this.incrementMetric(EClassMetricName.EXTERNAL_LIB_METHOD_CALL_COUNT, classToMergeWith.metrics.get(EClassMetricName.EXTERNAL_LIB_METHOD_CALL_COUNT));
        
        methods.addAll(classToMergeWith.methods);
        shortMethods.addAll(classToMergeWith.shortMethods);
        fields.addAll(classToMergeWith.fields);
        dependencies.addAll(classToMergeWith.dependencies);
        users.addAll(classToMergeWith.users);
        internalDependencies.addAll(classToMergeWith.internalDependencies);
        
        this.internalLibraryCalls.putAll(classToMergeWith.internalLibraryCalls);
        this.externalCalls.putAll(classToMergeWith.externalCalls);
	}
	
	/**
	 * Checks the class against another class to determine whether it is an exact match
	 * 
	 * An exact match implies that the comparing class has the following in common:
	 * 	- The exact same class name (including package)
	 * 	- The exact same super class name (including package)
	 * 	- Matches via an equality test (.equals(comparingClass))
	 * 	- All methods
	 * 	- All fields
	 * 	- All dependencies
	 * 
	 * @param comparingClass The class that is being compared as a match
	 * @return Whether the comparing class was an exact match
	 */
	public boolean isExactMatch(ClassMetricData comparingClass)
	{
		//Check for exact class name match 
		if (!comparingClass.getClassName().equals(getClassName())) return false;
		//Check for exact super class name match
		if (!comparingClass.getSuperClassName().equals(getSuperClassName())) return false;
		//Check for object equality
    	if (!equals(comparingClass)) return false;
    	
    	//Check comparing class has all methods that this class has
    	for (String method : methods)
			if (!comparingClass.methods.contains(method)) return false;
    	
    	//Check comparing class has all fields that this class has
    	for (String field : fields)
			if (!comparingClass.fields.contains(field)) return false;
    	
    	//Check comparing class has all dependencies that this class has
		for (String dependency : dependencies)
			if (!comparingClass.dependencies.contains(dependency)) return false;
		
		return true;
	}
	
	/**
	 * Determines the equality between this object and another object
	 * 
	 * Two classes will be considered equal if all of their
	 * comparison metric values are equal
	 */
	public boolean equals(Object compare)
	{
		if (!(compare instanceof ClassMetricData))
			return false;

		ClassMetricData comparingClass = (ClassMetricData)compare;
		
		//Get the equality comparison metrics
		EClassMetricName[] comparisonMetrics = MetricUtil.getComparisonMetrics();
		
		//Compare each metric value between the two classes,
		//returning false if there is a mismatch
		for(EClassMetricName metric : comparisonMetrics)
		{
			try
			{
				if (comparingClass.getMetricValue(metric).intValue() != this.getMetricValue(metric).intValue())
					return false;
			}
			catch (Exception e)
			{
				//TODO: Log error
				e.printStackTrace();
			}
		}
		
		//TODO: Should there be more to equality than just a metric match?
		return true;
	}

	public String toString()
	{
		return String.format("%3d, %3d, %3d, %3d, %3d, %3d, %3d, %4d, %3d, %3d, %3d, %4d, %4d, %3d, %3d, %s, %s",
				metrics.get(EClassMetricName.IN_DEGREE_COUNT), metrics.get(EClassMetricName.OUT_DEGREE_COUNT),
				metrics.get(EClassMetricName.METHOD_COUNT), metrics.get(EClassMetricName.LOAD_COUNT), metrics
						.get(EClassMetricName.STORE_COUNT), metrics.get(EClassMetricName.BRANCH_COUNT), metrics
						.get(EClassMetricName.FIELD_COUNT), metrics.get(EClassMetricName.SUPER_CLASS_COUNT),
				metrics.get(EClassMetricName.INTERFACE_COUNT), metrics.get(EClassMetricName.LOCAL_VAR_COUNT),
				metrics.get(EClassMetricName.TYPE_INSN_COUNT), metrics.get(EClassMetricName.ZERO_OP_INSN_COUNT),
				metrics.get(EClassMetricName.INTERNAL_METHOD_CALL_COUNT), metrics
						.get(EClassMetricName.EXTERNAL_METHOD_CALL_COUNT), metrics.get(EClassMetricName.AGE),
				getClassType(), getClassName());
	}
}