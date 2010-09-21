package extraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import util.StringUtil;

import model.ClassMetricData;
import model.MethodMetricData;
import model.MetricUtil;
import model.vocab.EClassMetricName;
import model.vocab.EMethodMetricName;
import model.vocab.EProcessingStatus;
import model.vocab.ETypeModifier;

/**
 * Factory class for extracting ClassMetricData objects and their associated
 * meta-data metrics from a given InputStream using ASM
 * @author Allan Jones
 */
public class ClassMetricDataFactory
{
	private static ClassMetricDataFactory instance;
	
	//ASM class node storing information extracted from
	//the classes bytecode
	private ClassNode classNode;
	
	private int rawSize;
	
	private ClassMetricDataFactory()
	{ }

	public static ClassMetricDataFactory getInstance()
	{
		if (instance == null) instance = new ClassMetricDataFactory();
		return instance;
	}
	
	/**
	 * Retrieves a ClassMetricData object with metrics that have been extracted from the given InputStream
	 * @param classStream The classes InputStream
	 * @return The ClassMetricData object corresponding to the InputStream 
	 * @throws IOException if the ASM ClassReader cannot be established for the given InputStream
	 */
	public ClassMetricData getClassMetricData(InputStream classStream) throws IOException
	{
		//Create a ClassReader to read from the InputStream
		ClassReader classReader = new ClassReader(classStream);
		
		//Store the raw size of the class
		rawSize = classReader.b.length;
		
		//Create the ClassNode and accept it using the ClassReader to read in class data from the stream
		//and store it in the ClassNode
		classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.SKIP_DEBUG);
		
		//Start the extraction process
		return extractClassMetricData();
	}
	
	/**
	 * Extracts the class-level metrics, method-level metrics and dependencies for the given class
	 * @return The ClassMetricData object housing the extracted information
	 */
	private ClassMetricData extractClassMetricData()
	{
		ClassMetricData classMetricData = new ClassMetricData();

		//Extract class-level and method-level metrics and indicate the completion
		//of base extraction process
		extractClassMetrics(classMetricData);
		extractMethodMetrics(classMetricData);
		
		classMetricData.setProcessingStatus(EProcessingStatus.BASE_EXTRACTED);
		
		//Extract class dependencies and indicate the completion of dependency extraction
		extractDependencies(classMetricData);
		classMetricData.setProcessingStatus(EProcessingStatus.DEPENDENCIES_EXTRACTED);
		
		return classMetricData;
	}

	/**
	 * Extract all class-level metrics for the given class into the ClassMetricData object 
	 * @param classMetricData The ClassMetricData object housing the extracted metrics
	 */
	private void extractClassMetrics(ClassMetricData classMetricData)
	{
		extractMetaData(classMetricData);
		extractMemberCounts(classMetricData);
		extractTypeMetrics(classMetricData);
		extractAccessMetrics(classMetricData);
		extractFieldMetrics(classMetricData);
		extractSizeMetrics(classMetricData);
	}


	/**
	 * Extract access/modifier information and initialisation status regarding the fields within the class
	 * @param classMetricData The ClassMetricData object housing the extracted field information
	 */
	@SuppressWarnings("unchecked")
	private void extractFieldMetrics(ClassMetricData classMetricData)
	{
		List<FieldNode> fields = classNode.fields;

		for (FieldNode field : fields)
		{
			if ((field.access & Opcodes.ACC_PRIVATE) != 0) //Private field
				classMetricData.incrementMetric(EClassMetricName.PRIVATE_FIELD_COUNT);
			if ((field.access & Opcodes.ACC_PROTECTED) != 0) //Protected field
				classMetricData.incrementMetric(EClassMetricName.PROTECTED_FIELD_COUNT);
			if ((field.access & Opcodes.ACC_PUBLIC) != 0) //Public field
				classMetricData.incrementMetric(EClassMetricName.PUBLIC_FIELD_COUNT);
			if ((field.access & Opcodes.ACC_STATIC) != 0) //Static field
				classMetricData.incrementMetric(EClassMetricName.STATIC_FIELD_COUNT);
			if ((field.access & Opcodes.ACC_FINAL) != 0) // Final Field
				classMetricData.incrementMetric(EClassMetricName.FINAL_FIELD_COUNT);

			//Add the field to the ClassMetricData objects set of fields
			//and add the fields type as a dependency
			classMetricData.addField(field.name + " " + field.desc);
			addDependency(classMetricData, Type.getType(field.desc));

			if (field.value == null) //Field has no initialisation value
				classMetricData.incrementMetric(EClassMetricName.UNINITIALIZED_FIELD_COUNT);
			else //Field has an initialisation value
				classMetricData.incrementMetric(EClassMetricName.INITIALIZED_FIELD_COUNT);
		}
	}

	/**
	 * Extracts basic meta-data (class name, super class name, package name) for the class
	 * @param classMetricData The ClassMetricData object housing the extracted meta-data
	 */
	private void extractMetaData(ClassMetricData classMetricData)
	{
		//Extract the class name, super class, package name and short name for the class
		String className = classNode.name; //Classes fully qualified (i.e. includes packages)
		String superClassName = classNode.superName.trim(); //The classes superclass
		String packageName = className.indexOf('/') == -1 ? "" : className.substring(0, className.lastIndexOf('/')); //The package the class belongs to
		String shortClassName = className.substring(className.lastIndexOf('/') + 1); //The classes short name (i.e. without packages)
		
		if (shortClassName == null) shortClassName = className; // TODO: Log warning
		
		classMetricData.setMetaDataValue(EClassMetricName.CLASS_NAME, className);
		classMetricData.setMetaDataValue(EClassMetricName.SUPER_CLASS_NAME, superClassName);
		classMetricData.setMetaDataValue(EClassMetricName.PACKAGE_NAME, packageName);
		classMetricData.setMetaDataValue(EClassMetricName.SHORT_CLASS_NAME, shortClassName);
	}

	/**
	 * Extract information regarding the classes members
	 * @param classMetricData The ClassMetricData object housing the extracted member information
	 */
	@SuppressWarnings("unchecked")
	private void extractMemberCounts(ClassMetricData classMetricData)
	{
		int innerClassCount = classNode.innerClasses.size();
		int interfaceCount = classNode.interfaces.size();
		int fieldCount = classNode.fields.size();
		int methodCount = classNode.methods.size();

		//Get the classes methods
		List<MethodNode> methods = classNode.methods;

		//Absorb the instruction count from each method
		for (MethodNode method : methods)
			classMetricData.incrementMetric(EClassMetricName.INSTRUCTION_COUNT, method.instructions.size());

		//Add the number of methods, fields and interfaces to the instruction count
		classMetricData.incrementMetric(EClassMetricName.INSTRUCTION_COUNT, methodCount + fieldCount + interfaceCount);

		classMetricData.setMetricValue(EClassMetricName.INTERFACE_COUNT, interfaceCount);
		classMetricData.setMetricValue(EClassMetricName.FIELD_COUNT, fieldCount);
		classMetricData.setMetricValue(EClassMetricName.METHOD_COUNT, methodCount);

		//Add to interfaces implemented to the classes internal set
		for (Object interfaceImplemented : classNode.interfaces)
        	classMetricData.addInterface((String)interfaceImplemented); 
		
		String className = classMetricData.getClassName(); 
		
		//Extract inner class info if this is an inner class,
		//else set the number of inner classes
		if (className.contains("$"))
        	extractInnerClassInfo(classMetricData, className);
		else
			classMetricData.setMetricValue(EClassMetricName.INNER_CLASS_COUNT, innerClassCount);
	}

	/**
	 * Extraction information (owning class name, depth) pertaining to an inner class
	 * @param classMetricData The ClassMetricData object housing the inner class information
	 * @param className The classes name
	 */
	private void extractInnerClassInfo(ClassMetricData classMetricData, String className)
	{
		//Flag as inner class
		classMetricData.setMetricValue(EClassMetricName.IS_INNER_CLASS, 1);
		
		//Get the depth of the inner class
		int innerClassCount = classNode.name.split("$").length - 1;
		classMetricData.setMetricValue(EClassMetricName.INNER_CLASS_COUNT, innerClassCount);
		
		//Get the classes outer (owning) class
		String outerClassName = StringUtil.getParent(className);
		classMetricData.setMetaDataValue(EClassMetricName.OUTER_CLASS_NAME, outerClassName);
	}

	/**
	 * Extracts type metrics (super class count, whether the class is an exception) for the class
	 * @param classMetricData The ClassMetricData object housing the extracted type information
	 */
	private void extractTypeMetrics(ClassMetricData classMetricData)
	{
		//Indicate 1 super class if deriving directly from 'Object'
		if (!classMetricData.getMetaDataValue(EClassMetricName.SUPER_CLASS_NAME).equals("java/lang/Object"))
			classMetricData.setMetricValue(EClassMetricName.SUPER_CLASS_COUNT, 1);
		
		//Flag as exception if 'Exception' or 'Throwable'
		if (classMetricData.getMetaDataValue(EClassMetricName.SUPER_CLASS_NAME).contains("Exception"))
			classMetricData.setMetricValue(EClassMetricName.IS_EXCEPTION, 1);
		if (classMetricData.getMetaDataValue(EClassMetricName.SUPER_CLASS_NAME).equals("java/lang/Throwable"))
			classMetricData.setMetricValue(EClassMetricName.IS_EXCEPTION, 1);

		//Flag whether the class is abstract/interface
		if ((classNode.access & Opcodes.ACC_ABSTRACT) != 0) classMetricData.setMetricValue(EClassMetricName.IS_ABSTRACT, 1);
		if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) classMetricData.setMetricValue(EClassMetricName.IS_INTERFACE, 1);
	}

	/**
	 * Extracts metrics regarding the access (public, private, protected) of the class
	 * @param classMetricData The ClassMetricData object housing the extracted access information
	 */
	private void extractAccessMetrics(ClassMetricData classMetricData)
	{
		if ((classNode.access & Opcodes.ACC_PUBLIC) != 0) //Public class
		{
			classMetricData.setMetricValue(EClassMetricName.IS_PUBLIC, 1);
			classMetricData.setMetricValue(EClassMetricName.ACCESS, ETypeModifier.PUBLIC.value());
		}
		else if ((classNode.access & Opcodes.ACC_PRIVATE) != 0) //Private class
		{
			classMetricData.setMetricValue(EClassMetricName.IS_PRIVATE, 1);
			classMetricData.setMetricValue(EClassMetricName.ACCESS, ETypeModifier.PRIVATE.value());
		}
		else if ((classNode.access & Opcodes.ACC_PROTECTED) != 0) //Protected class
		{
			classMetricData.setMetricValue(EClassMetricName.IS_PROTECTED, 1);
			classMetricData.setMetricValue(EClassMetricName.ACCESS, ETypeModifier.PROTECTED.value());
		}
	}

	/**
	 * Extracts the metrics for each of the methods within the class and then consumes the extracted
	 * metrics using the ClassMetricData object
	 * @param classMetricData The ClassMetricData object housing the extracted method metrics
	 */
	@SuppressWarnings("unchecked")
	private void extractMethodMetrics(ClassMetricData classMetricData)
	{
		//Get the classes set of methods
		List<MethodNode> methodsToExtract = classNode.methods;
		//Initialise a MethodMetricData set to house the extracted method metrics
		Set<MethodMetricData> methodSet = new HashSet<MethodMetricData>(methodsToExtract.size());

		//Get the MethodMetricData factory to extract method metrics
		MethodMetricDataFactory methodDataFactory = MethodMetricDataFactory.getInstance();
		
		//TODO: Make this configurable
		//Establish the list of classes that are considered internal
		List<String> classesConsideredInternal = new ArrayList<String>();		
		classesConsideredInternal.add(classNode.name); //Class itself
		classesConsideredInternal.add(classNode.superName.trim()); //Classes superclass
		classesConsideredInternal.add(StringUtil.getParent(classNode.name)); //Classes parent (if inner class)
		classesConsideredInternal.add("java/lang/Object"); //Default base class

		//Get the classes access
		ETypeModifier classesAccess = ETypeModifier.fromValue(classMetricData.getMetricValue(EClassMetricName.ACCESS));
		
		//Extract each methods metric and add it to the set
		for (MethodNode method : methodsToExtract)
			methodSet.add(methodDataFactory.getMethodMetricData(method, classesConsideredInternal,
																classNode.name, classesAccess));

		//Consume all of the method metrics extracted using the ClassMetricData object
		for(MethodMetricData method : methodSet)
			consumeMethodMetrics(classMetricData, method);
	}
	
	/**
	 * Consumes all of a methods extracted metrics using a ClassMetricData object
	 * @param classMetricData The ClassMetricData object housing the extracted method metrics
	 * @param methodData The method to be consumed
	 */
	private void consumeMethodMetrics(ClassMetricData classMetricData, MethodMetricData methodData)
	{
		//Increment class-level constructor count
	    if (methodData.getMetricValue(EMethodMetricName.IS_CONSTRUCTOR) ==  1)
        	classMetricData.incrementMetric(EClassMetricName.CLASS_CONSTRUCTOR_COUNT);
	    
		classMetricData.incrementMetric(EClassMetricName.PARAM_COUNT, methodData.getMetricValue(EMethodMetricName.PARAM_COUNT));
		classMetricData.incrementMetric(EClassMetricName.EXCEPTION_COUNT, methodData.getMetricValue(EMethodMetricName.EXCEPTION_COUNT));
		classMetricData.incrementMetric(EClassMetricName.LOCAL_VAR_COUNT, methodData.getMetricValue(EMethodMetricName.LOCAL_VAR_COUNT));
		
		classMetricData.incrementMetric(EClassMetricName.BRANCH_COUNT, methodData
				.getMetricValue(EMethodMetricName.BRANCH_COUNT));
		classMetricData.incrementMetric(EClassMetricName.CONSTANT_LOAD_COUNT, methodData
				.getMetricValue(EMethodMetricName.CONSTANT_LOAD_COUNT));
		classMetricData.incrementMetric(EClassMetricName.INCREMENT_OP_COUNT, methodData
				.getMetricValue(EMethodMetricName.INCREMENT_OP_COUNT));
		classMetricData.incrementMetric(EClassMetricName.I_STORE_COUNT, methodData
				.getMetricValue(EMethodMetricName.I_STORE_COUNT));
		classMetricData.incrementMetric(EClassMetricName.I_LOAD_COUNT, methodData
				.getMetricValue(EMethodMetricName.I_LOAD_COUNT));
		classMetricData.incrementMetric(EClassMetricName.REF_LOAD_OP_COUNT, methodData
				.getMetricValue(EMethodMetricName.REF_LOAD_OP_COUNT));
		classMetricData.incrementMetric(EClassMetricName.REF_STORE_OP_COUNT, methodData
				.getMetricValue(EMethodMetricName.REF_STORE_OP_COUNT));
		classMetricData.incrementMetric(EClassMetricName.LOAD_FIELD_COUNT, methodData
				.getMetricValue(EMethodMetricName.LOAD_FIELD_COUNT));
		classMetricData.incrementMetric(EClassMetricName.STORE_FIELD_COUNT, methodData
				.getMetricValue(EMethodMetricName.STORE_FIELD_COUNT));
		classMetricData.incrementMetric(EClassMetricName.THROW_COUNT, methodData
				.getMetricValue(EMethodMetricName.THROW_COUNT));
		classMetricData.incrementMetric(EClassMetricName.TRY_CATCH_BLOCK_COUNT, methodData
				.getMetricValue(EMethodMetricName.TRY_CATCH_BLOCK_COUNT));
		classMetricData.incrementMetric(EClassMetricName.TYPE_INSN_COUNT, methodData
				.getMetricValue(EMethodMetricName.TYPE_INSN_COUNT));
		classMetricData.incrementMetric(EClassMetricName.ZERO_OP_INSN_COUNT, methodData
				.getMetricValue(EMethodMetricName.ZERO_OP_INSN_COUNT));

		classMetricData.incrementMetric(EClassMetricName.METHOD_CALL_COUNT, methodData
				.getMetricValue(EMethodMetricName.METHOD_CALL_COUNT));
		classMetricData.incrementMetric(EClassMetricName.INTERNAL_METHOD_CALL_COUNT, methodData
				.getMetricValue(EMethodMetricName.INTERNAL_METHOD_CALL_COUNT));
		classMetricData.incrementMetric(EClassMetricName.EXTERNAL_METHOD_CALL_COUNT, methodData
				.getMetricValue(EMethodMetricName.EXTERNAL_METHOD_CALL_COUNT));

		classMetricData.incrementMetric(EClassMetricName.TYPE_CONSTRUCTION_COUNT, methodData
				.getMetricValue(EMethodMetricName.TYPE_CONSTRUCTION_COUNT));
		classMetricData.incrementMetric(EClassMetricName.INSTANCE_OF_COUNT, methodData
				.getMetricValue(EMethodMetricName.INSTANCE_OF_COUNT));
		classMetricData.incrementMetric(EClassMetricName.CHECK_CAST_COUNT, methodData
				.getMetricValue(EMethodMetricName.CHECK_CAST_COUNT));
		classMetricData.incrementMetric(EClassMetricName.NEW_COUNT, methodData.getMetricValue(EMethodMetricName.NEW_COUNT));
		classMetricData.incrementMetric(EClassMetricName.NEW_ARRAY_COUNT, methodData
				.getMetricValue(EMethodMetricName.NEW_ARRAY_COUNT));

		if (methodData.getMetricValue(EMethodMetricName.SCOPE) == ETypeModifier.PUBLIC.value())
			classMetricData.incrementMetric(EClassMetricName.PUBLIC_METHOD_COUNT);
		else if (methodData.getMetricValue(EMethodMetricName.SCOPE) == ETypeModifier.PRIVATE.value())
			classMetricData.incrementMetric(EClassMetricName.PRIVATE_METHOD_COUNT);
		else if (methodData.getMetricValue(EMethodMetricName.SCOPE) == ETypeModifier.PROTECTED.value())
			classMetricData.incrementMetric(EClassMetricName.PROTECTED_METHOD_COUNT);

		if (methodData.getMetricValue(EMethodMetricName.IS_ABSTRACT) == 1)
			classMetricData.incrementMetric(EClassMetricName.ABSTRACT_METHOD_COUNT);

		if (methodData.getMetricValue(EMethodMetricName.IS_FINAL) == 1)
			classMetricData.incrementMetric(EClassMetricName.FINAL_METHOD_COUNT);

		if (methodData.getMetricValue(EMethodMetricName.IS_STATIC) == 1)
			classMetricData.incrementMetric(EClassMetricName.STATIC_METHOD_COUNT);

		if (methodData.getMetricValue(EMethodMetricName.IS_SYNCHRONIZED) == 1)
			classMetricData.incrementMetric(EClassMetricName.SYNCHRONIZED_METHOD_COUNT);
				
		classMetricData.addShortMethod(methodData.getShortMethodName());
		classMetricData.addMethod(methodData.getMethodName());
		
		Map<String, Integer> methodExternalCalls = methodData.getExternalCalls();
		Map<String, Integer> classExternalCalls = classMetricData.getExternalCalls();
		
		for(Entry<String, Integer> externalCall : methodExternalCalls.entrySet())
		{
			String externalCallClassName = externalCall.getKey();
			
			Integer callCount = classExternalCalls.get(externalCallClassName);
			Integer methodExternalCallCount = externalCall.getValue();
			
			//Already exists, increment
			if(callCount != null)
				classExternalCalls.put(externalCallClassName, callCount + methodExternalCallCount);
			else
				classExternalCalls.put(externalCallClassName, methodExternalCallCount);
		}
		
		//Consumes the dependencies held by the method
		Set<String> methodDependencies = methodData.getDependencies();
		
		for(String methodDependency : methodDependencies)
			classMetricData.addDependency(methodDependency);
	}
	
	/**
	 * Extract all dependencies for the class into the ClassMetricData object 
	 * @param classMetricData The ClassMetricData object housing the extracted dependencies
	 */
	@SuppressWarnings("unchecked")
	private void extractDependencies(ClassMetricData classMetricData)
	{
		//Get the classes list of methods
		List<MethodNode> methods = classNode.methods;
		
		//Extract dependencies from methods
		for (MethodNode method : methods)
		{
			//Add all exception types as dependencies
			for (Object exception : method.exceptions)
				classMetricData.addDependency((String)exception);
			
			//Add return type as a dependency
			Type type = Type.getReturnType(method.desc);
			addDependency(classMetricData, type);

			//Add arg types as dependencies
			Type[] argTypes = Type.getArgumentTypes(method.desc);
			
			for (int i = 0; i < argTypes.length; i++)
				addDependency(classMetricData, argTypes[i]);
		}
		
		//Remove all redundant dependencies from the class
		removeRedundantDependencies(classMetricData);
		
		//Set the classes out degree count
		classMetricData.setMetricValue(EClassMetricName.OUT_DEGREE_COUNT, classMetricData.getDependencies().size());
	}

	/**
	 * Remove all of the classes redundancies (self, primitive, other dependencies) from the ClassMetricData object
	 * @param classMetricData The ClassMetricData object to remove dependencies from
	 */
	//TODO: Possibly refactor this -- it repeats in VersionExtractor...does it need to be at both extraction levels?
	private void removeRedundantDependencies(ClassMetricData classMetricData)
    {
		removeSelfDependency(classMetricData);
		removePrimitiveDependencies(classMetricData);
        removeQualifiedDependencies(classMetricData);            	
    }

	/**
	 * Removes the class as a dependency of itself from the ClassMetricData object
	 * @param classMetricData The ClassMetricData object to remove self dependencies from
	 */
	private void removeSelfDependency(ClassMetricData classMetricData)
	{
		classMetricData.removeDependency(classNode.name);
	}

	/**
	 * Removes primitive type dependencies from the ClassMetricData object
	 * @param classMetricData The ClassMetricData object to remove primitive dependencies from
	 */
	private void removePrimitiveDependencies(ClassMetricData classMetricData)
	{
		//TODO: Make configurable
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
	private void removeQualifiedDependencies(ClassMetricData classMetricData)
	{
		//Create sets for dependencies to add and remove
		HashSet<String> dependenciesToRemove = new HashSet<String>();
        HashSet<String> dependenciesToAdd = new HashSet<String>();
        
        Set<String> dependencies = classMetricData.getDependencies();
        
        for (String dependency : dependencies)
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
            	//Remove fully qualified array type
            	dependenciesToRemove.add(dependency);
        	}
        	
        	//If dependency is a reference
        	if (dependency.startsWith("L"))
        	{
        		//Remove the 'L' from the start and add the type as a dependency
        		String dependencyName = dependency.substring(1);
        		dependenciesToAdd.add(dependencyName);
        		dependenciesToRemove.add(dependency);
        	}
        }
        
        //Add and remove all the dependency flagged for adding and removal
        // The order of the following 2 loops is very important -- DO NOT CHANGE THESE 
        for (String dependency : dependenciesToAdd) classMetricData.addDependency(dependency);
        for (String dependency : dependenciesToRemove) classMetricData.removeDependency(dependency); // now remove these inner classes from dependency list
	}
	
	/**
	 * Extracts type information from an ASM type object and adds the type as a
	 * dependency to the ClassMetricData objects set
	 * @param classMetricData The ClassMetricData object to add a dependency to
	 * @param type The ASM type object for the dependency
	 */
	private void addDependency(ClassMetricData classMetricData, Type type)
	{
		if (type == null) return;
		
		if (type.getSort() == Type.OBJECT) //Object type
		{
			//Get the types name and add as a dependency
			String dependency = type.getInternalName();
			classMetricData.addDependency(dependency);

			//Flag the class as an I/O class if dependency is an I/O type
			if(typeIsIO(dependency))
				MetricUtil.setMetricValue(classMetricData.getMetrics(), EClassMetricName.IS_IO_CLASS, 1);
			
			//Flag the class as an I/O class if dependency is a GUI type
			if(typeIsGUI(dependency))
				MetricUtil.setMetricValue(classMetricData.getMetrics(), EClassMetricName.IS_GUI_CLASS, 1);
		}
		else if (type.getSort() == Type.ARRAY) //Array type
		{
			String dependency = type.getInternalName();
			
			//If type is non-primitive, add as a dependency
			if (dependency.endsWith(";"))
			{
				//Remove square-brackets (start) and semi-colon (end) and add as dependency
				String dependencyName = dependency.substring(2, dependency.length() - 1);
				classMetricData.addDependency(dependencyName);
			}
		}
	}
	
	/**
	 * Determines whether a type is considered an I/O class
	 * 
	 * I/O classes are any class which belong to the following packages (sub-packages inclusive):
	 * 
	 * - java.io
	 * - java.nio
	 * 
	 * @param type The class type to distinguish as I/O
	 * @return Whether the given type is an I/O type
	 */
	private boolean typeIsIO(String type)
	{
		//TODO: Make configurable
		return type.startsWith("java/io") || type.startsWith("java/nio");
	}
	
	/**
	 * Determines whether a type is considered a GUI class
	 * 
	 * GUI classes are any class which belong to the following packages (sub-packages inclusive):
	 * 
	 * - java.awt.event
	 * - java.applet
	 * - javax.swing
	 * - javax.swing.event
	 * - org.eclipse.swt.events
	 * 
	 * @param type The class type to distinguish as GUI
	 * @return Whether the given type is a GUI type
	 */
	private boolean typeIsGUI(String type)
	{
		//TODO: Make configurable
		return type.startsWith("java/awt/event")
			|| type.startsWith("java/applet")
			|| type.startsWith("javax/swing")
			|| type.startsWith("javax/swing/event")
			|| type.startsWith("org/eclipse/swt/events");
	}
	
	/**
	 * Extracts size-related metrics (raw size, normalized branch count) for the given ClassMetricData object
	 * @param classMetricData The ClassMetricData object to extract size metrics for
	 */
	private void extractSizeMetrics(ClassMetricData classMetricData)
	{
		classMetricData.setMetricValue(EClassMetricName.RAW_SIZE, rawSize);
		
		int branchCount = classMetricData.getMetricValue(EClassMetricName.BRANCH_COUNT);
		int normalizedBranchCount = (int)(((double)branchCount / rawSize) * 100.0);
		
		classMetricData.setMetricValue(EClassMetricName.NORMALIZED_BRANCH_COUNT, normalizedBranchCount);
	}
}
