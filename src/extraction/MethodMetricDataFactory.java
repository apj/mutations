package extraction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.MethodMetricData;
import model.MetricUtil;
import model.vocab.EMethodMetricName;
import model.vocab.ETypeModifier;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.TraceMethodVisitor;

import util.StringUtil;

/**
 * Factory class for extracting method information (metrics, dependencies, external calls) from an ASM MethodNode data type
 * to produce a MethodMetricData object containing the extracted information
 * 
 * @author Allan Jones
 */
public class MethodMetricDataFactory
{
	private static MethodMetricDataFactory instance;
	
	//Node containing method information extracted from
	//classes bytecode
	private MethodNode methodNode;
	//The classes that are considered internal according to the method
	private List<String> classesConsideredInternal;
	//The name of the class that owns this method
	private String owningClassName;
	//The scope of the owning class
	private ETypeModifier owningClassScope;
	
	//Map of method-related metrics matching the values that
	//have been extracted from the MethodNode
	private Map<EMethodMetricName, Integer> metricMap;
	//The classes that the method depends upon
	private Set<String> dependencies;
	//The calls made by the method that are considered external
	private Map<String, Integer> externalCalls;
	
	private MethodMetricDataFactory()
	{ }

	public static MethodMetricDataFactory getInstance()
	{
		if (instance == null) instance = new MethodMetricDataFactory();
		return instance;
	}
	
	/**
	 * Retrieves a MethodMetricData object containing information that has been extracted from the given MethodNode
	 * 
	 * @param methodNode The ASM MethodNode to extract the method information from
	 * @param classesConsideredInternal A list of classes that are considered internal to the method
	 * @param owningClassName The name of the class that owns the method
	 * @param owningClassScope The scope (i.e. private, protected, public) of the class that owns the method
	 * @return The extracted MethodMetricData object containing necessary information
	 */
	public MethodMetricData getMethodMetricData(MethodNode methodNode, List<String> classesConsideredInternal, String owningClassName,
			ETypeModifier owningClassScope)
	{
		this.methodNode = methodNode;
		this.classesConsideredInternal = classesConsideredInternal;
		this.owningClassName = owningClassName;
		this.owningClassScope = owningClassScope;
		
		return extractMethodMetricData();
	}
	
	/**
	 * Extracts various information about a method (variable counts, instructions, type modifiers and scope)
	 * and produces a MethodMetricData object containing this extracted information
	 * @return
	 */
	private MethodMetricData extractMethodMetricData()
	{
		initialiseMetricMap();
		
		this.dependencies = new HashSet<String>();
		this.externalCalls = new HashMap<String, Integer>();
		
		extractVariableCounts();
		extractInstructions();
		extractTypeModifiers();
		extractScope();
		
		return new MethodMetricData(metricMap, externalCalls, dependencies,
									methodNode.name + " " + methodNode.desc,
									methodNode.name + " " + shrinkTypeInformationInMethod(methodNode.desc));
	}
	
	/**
	 * Initialises all metric values within the metric map at 0
	 */
	private void initialiseMetricMap()
	{
		metricMap = new HashMap<EMethodMetricName, Integer>(50);
		
		EMethodMetricName[] metrics = EMethodMetricName.values();
		
		for (EMethodMetricName metric : metrics)
			metricMap.put(metric, 0);
	}
	
	/**
	 * Extracts the MethodNodes variable counts (parameters, exceptions, locals) and determines
	 * whether or not it is a constructor
	 */
	private void extractVariableCounts()
	{
		metricMap.put(EMethodMetricName.PARAM_COUNT, Type.getArgumentTypes(methodNode.desc).length);
		metricMap.put(EMethodMetricName.EXCEPTION_COUNT, methodNode.exceptions.size());
		metricMap.put(EMethodMetricName.LOCAL_VAR_COUNT, methodNode.maxLocals);
        
		//Determine if method is a constructor and flag accordingly
        if (methodNode.name.equals("<init>")) metricMap.put(EMethodMetricName.IS_CONSTRUCTOR, 1);
	}
	
	/**
	 * Helper method to shorten the type names (i.e. remove package qualification) of a methods
	 * arguments and combine them to form a shortened version of the arguments
	 * @param argumentsString The arguments to shorten
	 * @return The shortened arguments string
	 */
	private String shrinkTypeInformationInMethod(String argumentsString)
	{
		//Split the arguments string into separate types
		String[] params = argumentsString.split("[(;)]");
		
		StringBuilder shortArgumentsBuilder = new StringBuilder();
		
		for(String param : params)
		{
			//Retrieve the index at which the package qualification ends
			int packageEndIndex = param.lastIndexOf("/");
			
			//Append the type sans package name if it is within a package, otherwise just append the type
			if(packageEndIndex > 0) shortArgumentsBuilder.append(param.substring(packageEndIndex + 1) + " ");
			else shortArgumentsBuilder.append(param);
		}
		
		return shortArgumentsBuilder.toString();
	}

	/**
	 * Extracts information from all of the MethodNodes instructions
	 * by accepting a number of internally defined visitors on each instruction node
	 */
	private void extractInstructions()
	{
		//For each instruction node in the method
		for (int i = 0; i < methodNode.instructions.size(); ++i)
		{
			// Get instruction node.
			Object insn = methodNode.instructions.get(i);
			// Have it accept visitors and update the provided
			// MethodMetricData
			((AbstractInsnNode) insn).accept(getVariableInstructionVisitor());
			// Fields.
			((AbstractInsnNode) insn).accept(getFieldVisitor());
			// Jump/branching.
			((AbstractInsnNode) insn).accept(getJumpInstructionVisitor());
			// Switching.
			((AbstractInsnNode) insn).accept(getSwitchVisitor());
			// Op/Loads.
			((AbstractInsnNode) insn).accept(getOpAndLoadVisitor());
			// Type.
			((AbstractInsnNode) insn).accept(getTypeVisitor());
			// Methods.
			((AbstractInsnNode) insn).accept(getMethodVisitor());
		}
		
		//For each instruction node in the method
		for (int i = 0; i < methodNode.tryCatchBlocks.size(); ++i)
		{
			// Get instruction node.
			Object tryCatchBlockNode = methodNode.tryCatchBlocks.get(i);
			// Try/Catch blocks.
			((TryCatchBlockNode) tryCatchBlockNode).accept(getTryCatchBlockVisitor());
		}
	}

	/**
	 * Creates a TraceMethodVisitor that will visit a variable instruction node and extract
	 * all of it's associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract variable instruction information
	 */
	private TraceMethodVisitor getVariableInstructionVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitVarInsn(int opcode, int var)
			{
				//Instruction load
				if (opcode >= Opcodes.ILOAD && opcode <= Opcodes.DLOAD)
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.I_LOAD_COUNT);
				//Instruction store
				if (opcode >= Opcodes.ISTORE && opcode <= Opcodes.DSTORE)
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.I_STORE_COUNT);
				//Reference load
				if (opcode == Opcodes.ALOAD)
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.REF_LOAD_OP_COUNT);
				//Reference store
				if (opcode == Opcodes.ASTORE)
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.REF_STORE_OP_COUNT);
			}
		};
	}

	/**
	 * Creates a TraceMethodVisitor that will visit an try/catch block instruction node and extract
	 * all of it's associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract try/catch block instruction information
	 */
	private TraceMethodVisitor getTryCatchBlockVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
			{
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.TRY_CATCH_BLOCK_COUNT);
			}
		};
	}

	/**
	 * Creates a TraceMethodVisitor that will visit a field instruction node and extract
	 * all of it's associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract field instruction information
	 */
	private TraceMethodVisitor getFieldVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitFieldInsn(int opcode, String owner, String name, String desc)
			{
				if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) //Field store
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.STORE_FIELD_COUNT);
				else if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) //Field load
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.LOAD_FIELD_COUNT);
			}
		};
	}

	/**
	 * Creates a TraceMethodVisitor that will visit a jump instruction node and extract
	 * all of it's associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract jump instruction information
	 */
	private TraceMethodVisitor getJumpInstructionVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitJumpInsn(int opcode, Label label)
			{
				if (opcode != Opcodes.GOTO) MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.BRANCH_COUNT);
			}
		};
	}

	/**
	 * Creates a TraceMethodVisitor that will visit a type instruction node and extract
	 * all of it's associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract type instruction information
	 */
	private TraceMethodVisitor getTypeVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitTypeInsn(int opcode, String desc)
			{
				//instanceof keyword
				if (opcode == Opcodes.INSTANCEOF) MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.INSTANCE_OF_COUNT);
				
				//Type cast
				if (opcode == Opcodes.CHECKCAST) MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.CHECK_CAST_COUNT);
				
				//New keyword
				if (opcode == Opcodes.NEW)
				{
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.NEW_COUNT);
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.TYPE_CONSTRUCTION_COUNT);
				}
				
				// New array declaration
				if (opcode == Opcodes.ANEWARRAY)
				{
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.NEW_ARRAY_COUNT);
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.TYPE_CONSTRUCTION_COUNT);
				}

				//Increment type instruction count
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.TYPE_INSN_COUNT);
			}

			public void visitInsn(int opcode)
			{
				//Increment count of instruction with zero operations
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.ZERO_OP_INSN_COUNT);
				
				//Exception thrown
				if (opcode == Opcodes.ATHROW)
				{
					//Increment branch count and throw count
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.BRANCH_COUNT);
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.THROW_COUNT);
				}
			}
		};
	}

	/**
	 * Creates a TraceMethodVisitor that will visit op and load instruction nodes and extract
	 * all of their associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract op and load instruction information
	 */
	private TraceMethodVisitor getOpAndLoadVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitLdcInsn(Object cst)
			{
				//Increment constant load count
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.CONSTANT_LOAD_COUNT);
			}

			public void visitIincInsn(int var, int increment)
			{
				//Increment increment operator count
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.INCREMENT_OP_COUNT);
			}
		};
	}

	/**
	 * Creates a TraceMethodVisitor that will visit switch instruction nodes and extract
	 * all of their associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract switch instruction information
	 */
	private TraceMethodVisitor getSwitchVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels)
			{
				//Increment branch count by the number of lookups in the switch statement
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.BRANCH_COUNT, labels.length);
			}

			public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels)
			{
				//Increment branch count by the number of cases in the switch table
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.BRANCH_COUNT, labels.length);
			}
		};
	}

	/**
	 * Creates a TraceMethodVisitor that will visit method invocation instruction nodes and extract
	 * all of their associated metrics into the internal metric map
	 * @return The TraceMethodVisitor to extract method invocation instruction information
	 */
	private MethodVisitor getMethodVisitor()
	{
		return new TraceMethodVisitor()
		{
			public void visitMethodInsn(int opcode, String owner, String n, String d)
			{
				//Increment method call count
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.METHOD_CALL_COUNT);
				
				if(isInternalCall(owner)) //Is internal, increment call count
					MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.INTERNAL_METHOD_CALL_COUNT); 
				else
					addExternalCall(owner); //Register as an external call
			
				//Add owner as dependency
				dependencies.add(owner);
			}
			
			private boolean isInternalCall(String owner)
			{
				//Owning class is internal
				if (classesConsideredInternal.contains(owner))
					return true;
				else
				{
					int targetDI = owner.indexOf("$");
					int callerDI = owningClassName.indexOf("$");

					//Parent class, calls it's inner class
					if ((callerDI < 0) && (targetDI > 0) && (StringUtil.getParent(owner).equals(owningClassName))
					// Inner-class, calls its own normal-parent-class
					|| (callerDI > 0) && (targetDI < 0) && (StringUtil.getParent(owningClassName).equals(owner))
					// Inner-class, calls another inner-class in the same scope
					|| (callerDI > 0) && (targetDI > 0) && (StringUtil.getParent(owner).equals(StringUtil.getParent(owningClassName))))
						return true;
				}
				
				return false;
			}
			
			private void addExternalCall(String owner)
			{
				MetricUtil.incrementMetricValue(metricMap, EMethodMetricName.EXTERNAL_METHOD_CALL_COUNT);

				// Get outermost class
				String target = StringUtil.getParent(owner);
				Integer currentCallCount = externalCalls.get(target);

				if (currentCallCount == null) //Register a new external call if count does not exist
					externalCalls.put(target, Integer.valueOf(1));
				else //Increment count if it does exist
					externalCalls.put(target, currentCallCount.intValue() + 1);
			}
		};
	}
	
	/**
	 * Extract types modifiers (abstract, final, static, synchronised) from the MethodNode
	 * and stores them into the internal metric map
	 */
	private void extractTypeModifiers()
    {
		//Abstract method
		if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0) metricMap.put(EMethodMetricName.IS_ABSTRACT, 1);
		//Final method
		if ((methodNode.access & Opcodes.ACC_FINAL) != 0) metricMap.put(EMethodMetricName.IS_FINAL, 1);
		//Static method
		if ((methodNode.access & Opcodes.ACC_STATIC) != 0) metricMap.put(EMethodMetricName.IS_STATIC, 1);
		//Synchronised method
		if ((methodNode.access & Opcodes.ACC_SYNCHRONIZED) != 0) metricMap.put(EMethodMetricName.IS_SYNCHRONIZED, 1);
    }
	
	/**
	 * Extracts the methods scope (private, protected, public) from the MethodNode and stores
	 * it in the internal metric map 
	 */
	private void extractScope()
	{
		//Private method
		if ((methodNode.access & Opcodes.ACC_PRIVATE) != 0)
			metricMap.put(EMethodMetricName.SCOPE, ETypeModifier.PRIVATE.value());
		//Protected method
		else if ((methodNode.access & Opcodes.ACC_PROTECTED) != 0)
			metricMap.put(EMethodMetricName.SCOPE, ETypeModifier.PROTECTED.value());
		//Public method
		else if (((methodNode.access & Opcodes.ACC_PUBLIC) != 0) && owningClassScope == ETypeModifier.PUBLIC)
			metricMap.put(EMethodMetricName.SCOPE, ETypeModifier.PUBLIC.value());
	}
}