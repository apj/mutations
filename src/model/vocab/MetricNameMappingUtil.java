package model.vocab;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that assists in retrieving different representations of metric names (e.g. acronyms, camel case variants)
 * 
 * @author Allan Jones
 */
public class MetricNameMappingUtil
{
	//TODO: Make this generic if there is a way to make it play nice with fields
	//Mapping of class metric names to their associated name mappings
	private static Map<EClassMetricName, MetricNameMapping<EClassMetricName>> classMetricNameMap = new HashMap<EClassMetricName, MetricNameMapping<EClassMetricName>>();
	
	static
	{
		classMetricNameMap.put(EClassMetricName.METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.METHOD_COUNT, "NOM"));
		classMetricNameMap.put(EClassMetricName.ABSTRACT_METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.ABSTRACT_METHOD_COUNT, "AMC"));
		classMetricNameMap.put(EClassMetricName.CLASS_CONSTRUCTOR_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.CLASS_CONSTRUCTOR_COUNT, "CCC"));
		classMetricNameMap.put(EClassMetricName.PROTECTED_METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.PROTECTED_METHOD_COUNT, "RMC"));
		classMetricNameMap.put(EClassMetricName.PUBLIC_METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.PUBLIC_METHOD_COUNT, "PMC"));
		classMetricNameMap.put(EClassMetricName.PRIVATE_METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.PRIVATE_METHOD_COUNT, "IMC"));
		classMetricNameMap.put(EClassMetricName.STATIC_METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.STATIC_METHOD_COUNT, "SMC"));
		classMetricNameMap.put(EClassMetricName.FINAL_METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.FINAL_METHOD_COUNT, "FMC"));
		classMetricNameMap.put(EClassMetricName.SYNCHRONIZED_METHOD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.SYNCHRONIZED_METHOD_COUNT, "YMC"));
		classMetricNameMap.put(EClassMetricName.FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.FIELD_COUNT, "NOF"));
		classMetricNameMap.put(EClassMetricName.INITIALIZED_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INITIALIZED_FIELD_COUNT, "ZFC"));
		classMetricNameMap.put(EClassMetricName.UNINITIALIZED_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.UNINITIALIZED_FIELD_COUNT, "UFC"));
		classMetricNameMap.put(EClassMetricName.PUBLIC_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.PUBLIC_FIELD_COUNT, "PFC"));
		classMetricNameMap.put(EClassMetricName.PRIVATE_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.PRIVATE_FIELD_COUNT, "IFC"));
		classMetricNameMap.put(EClassMetricName.PROTECTED_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.PROTECTED_FIELD_COUNT, "RFC"));
		classMetricNameMap.put(EClassMetricName.FINAL_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.FINAL_FIELD_COUNT, "FFC"));
		classMetricNameMap.put(EClassMetricName.STATIC_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.STATIC_FIELD_COUNT, "SFC"));
		classMetricNameMap.put(EClassMetricName.INNER_CLASS_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INNER_CLASS_COUNT, "ICC"));
		classMetricNameMap.put(EClassMetricName.INTERFACE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INTERFACE_COUNT, "INC"));
		classMetricNameMap.put(EClassMetricName.TRY_CATCH_BLOCK_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.TRY_CATCH_BLOCK_COUNT, "CBC"));
		classMetricNameMap.put(EClassMetricName.THROW_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.THROW_COUNT, "THC"));
		classMetricNameMap.put(EClassMetricName.LOCAL_VAR_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.LOCAL_VAR_COUNT, "LVC"));
		classMetricNameMap.put(EClassMetricName.METHOD_CALL_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.METHOD_CALL_COUNT, "MCC"));
		classMetricNameMap.put(EClassMetricName.INTERNAL_METHOD_CALL_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INTERNAL_METHOD_CALL_COUNT, "MCI"));
		classMetricNameMap.put(EClassMetricName.EXTERNAL_METHOD_CALL_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.EXTERNAL_METHOD_CALL_COUNT, "MCE"));
		classMetricNameMap.put(EClassMetricName.INTERNAL_LIB_METHOD_CALL_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INTERNAL_LIB_METHOD_CALL_COUNT, "LMCI"));
		classMetricNameMap.put(EClassMetricName.EXTERNAL_LIB_METHOD_CALL_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.EXTERNAL_LIB_METHOD_CALL_COUNT, "LMCE"));
		classMetricNameMap.put(EClassMetricName.EXCEPTION_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.EXCEPTION_COUNT, "EC"));
		classMetricNameMap.put(EClassMetricName.CONSTANT_LOAD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.CONSTANT_LOAD_COUNT, "CLC"));
		classMetricNameMap.put(EClassMetricName.BRANCH_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.BRANCH_COUNT, "CC"));
		classMetricNameMap.put(EClassMetricName.INSTANCE_OF_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INSTANCE_OF_COUNT, "IAC"));
		classMetricNameMap.put(EClassMetricName.CHECK_CAST_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.CHECK_CAST_COUNT, "CAC"));
		classMetricNameMap.put(EClassMetricName.OUT_DEGREE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.OUT_DEGREE_COUNT, "ODC"));
		classMetricNameMap.put(EClassMetricName.IN_DEGREE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.IN_DEGREE_COUNT, "IDC"));
		classMetricNameMap.put(EClassMetricName.EXTERNAL_OUT_DEGREE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.EXTERNAL_OUT_DEGREE_COUNT, "EODC"));
		classMetricNameMap.put(EClassMetricName.INTERNAL_OUT_DEGREE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INTERNAL_OUT_DEGREE_COUNT, "IODC"));
		classMetricNameMap.put(EClassMetricName.TYPE_CONSTRUCTION_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.TYPE_CONSTRUCTION_COUNT, "TCC"));
		classMetricNameMap.put(EClassMetricName.NEW_ARRAY_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.NEW_ARRAY_COUNT, "NAC"));
		classMetricNameMap.put(EClassMetricName.NUMBER_OF_CHILDREN, new MetricNameMapping<EClassMetricName>(EClassMetricName.NUMBER_OF_CHILDREN, "NOC"));
		classMetricNameMap.put(EClassMetricName.NUMBER_OF_ANCESTORS, new MetricNameMapping<EClassMetricName>(EClassMetricName.NUMBER_OF_ANCESTORS, "NOA"));
		classMetricNameMap.put(EClassMetricName.NUMBER_OF_DESCENDANTS, new MetricNameMapping<EClassMetricName>(EClassMetricName.NUMBER_OF_DESCENDANTS, "NOD"));
		classMetricNameMap.put(EClassMetricName.DEPTH_IN_INHERITANCE_TREE, new MetricNameMapping<EClassMetricName>(EClassMetricName.DEPTH_IN_INHERITANCE_TREE, "DIT"));
		classMetricNameMap.put(EClassMetricName.SUPER_CLASS_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.SUPER_CLASS_COUNT, "SCC"));
		classMetricNameMap.put(EClassMetricName.LOAD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.LOAD_COUNT, "LIC"));
		classMetricNameMap.put(EClassMetricName.STORE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.STORE_COUNT, "SIC"));
		classMetricNameMap.put(EClassMetricName.I_LOAD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.I_LOAD_COUNT, "ILC"));
		classMetricNameMap.put(EClassMetricName.I_STORE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.I_STORE_COUNT, "ISC"));
		classMetricNameMap.put(EClassMetricName.LOAD_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.LOAD_FIELD_COUNT, "LFI"));
		classMetricNameMap.put(EClassMetricName.STORE_FIELD_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.STORE_FIELD_COUNT, "SFI"));
		classMetricNameMap.put(EClassMetricName.REF_LOAD_OP_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.REF_LOAD_OP_COUNT, "RLC"));
		classMetricNameMap.put(EClassMetricName.REF_STORE_OP_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.REF_STORE_OP_COUNT, "RSC"));
		classMetricNameMap.put(EClassMetricName.LOAD_RATIO, new MetricNameMapping<EClassMetricName>(EClassMetricName.LOAD_RATIO, "LRT"));
		classMetricNameMap.put(EClassMetricName.INCREMENT_OP_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INCREMENT_OP_COUNT, "IOC"));
		classMetricNameMap.put(EClassMetricName.INSTRUCTION_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.INSTRUCTION_COUNT, "ITC"));
		classMetricNameMap.put(EClassMetricName.IS_ABSTRACT, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_ABSTRACT, "IAS"));
		classMetricNameMap.put(EClassMetricName.IS_INTERFACE, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_INTERFACE, "INF"));
		classMetricNameMap.put(EClassMetricName.IS_EXCEPTION, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_EXCEPTION, "IE"));
		classMetricNameMap.put(EClassMetricName.IS_PRIVATE, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_PRIVATE, "II"));
		classMetricNameMap.put(EClassMetricName.IS_PROTECTED, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_PROTECTED, "IR"));
		classMetricNameMap.put(EClassMetricName.IS_PUBLIC, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_PUBLIC, "IP"));
		classMetricNameMap.put(EClassMetricName.IS_INNER_CLASS, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_INNER_CLASS, "IIN"));
		classMetricNameMap.put(EClassMetricName.IS_PACKAGE_ACCESSIBLE, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_PACKAGE_ACCESSIBLE, "IK"));
		classMetricNameMap.put(EClassMetricName.IS_MODIFIED, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_MODIFIED, "IM"));
		classMetricNameMap.put(EClassMetricName.IS_DELETED, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_DELETED, "ID"));
		classMetricNameMap.put(EClassMetricName.IS_MODIFIED_NEXT_VERSION, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_MODIFIED_NEXT_VERSION, "IMN"));
		classMetricNameMap.put(EClassMetricName.EVOLUTION_STATUS, new MetricNameMapping<EClassMetricName>(EClassMetricName.EVOLUTION_STATUS, "EVS"));
		classMetricNameMap.put(EClassMetricName.EVOLUTION_DISTANCE, new MetricNameMapping<EClassMetricName>(EClassMetricName.EVOLUTION_DISTANCE, "EVD"));
		classMetricNameMap.put(EClassMetricName.JAVA_UTIL_OUT_DEGREE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.JAVA_UTIL_OUT_DEGREE_COUNT, "JUO"));
		classMetricNameMap.put(EClassMetricName.PARAM_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.PARAM_COUNT, "NOP"));
		classMetricNameMap.put(EClassMetricName.RAW_SIZE, new MetricNameMapping<EClassMetricName>(EClassMetricName.RAW_SIZE, "RSZ"));
		classMetricNameMap.put(EClassMetricName.NEXT_VERSION_STATUS, new MetricNameMapping<EClassMetricName>(EClassMetricName.NEXT_VERSION_STATUS, "NVS"));
		classMetricNameMap.put(EClassMetricName.USAGE_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.USAGE_COUNT, "USC"));
		classMetricNameMap.put(EClassMetricName.BORN_RSN, new MetricNameMapping<EClassMetricName>(EClassMetricName.BORN_RSN, "BRS"));
		classMetricNameMap.put(EClassMetricName.AGE, new MetricNameMapping<EClassMetricName>(EClassMetricName.AGE, "AGE"));
		classMetricNameMap.put(EClassMetricName.MODIFICATION_STATUS_SINCE_BIRTH, new MetricNameMapping<EClassMetricName>(EClassMetricName.MODIFICATION_STATUS_SINCE_BIRTH, "MSB"));
		classMetricNameMap.put(EClassMetricName.NORMALIZED_BRANCH_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.NORMALIZED_BRANCH_COUNT, "NBC"));
		classMetricNameMap.put(EClassMetricName.ZERO_OP_INSN_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.ZERO_OP_INSN_COUNT, "ZOC"));
		classMetricNameMap.put(EClassMetricName.TYPE_INSN_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.TYPE_INSN_COUNT, "TIC"));
		classMetricNameMap.put(EClassMetricName.NEW_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.NEW_COUNT, "NCN"));
		classMetricNameMap.put(EClassMetricName.MODIFIED_METRIC_COUNT, new MetricNameMapping<EClassMetricName>(EClassMetricName.MODIFIED_METRIC_COUNT, "MMC"));
		classMetricNameMap.put(EClassMetricName.MODIFIED_METRIC_COUNT_SINCE_BIRTH, new MetricNameMapping<EClassMetricName>(EClassMetricName.MODIFIED_METRIC_COUNT_SINCE_BIRTH, "MMB"));
		classMetricNameMap.put(EClassMetricName.IS_IO_CLASS, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_IO_CLASS, "IIC"));
		classMetricNameMap.put(EClassMetricName.IS_GUI_CLASS, new MetricNameMapping<EClassMetricName>(EClassMetricName.IS_GUI_CLASS, "IGC"));
		classMetricNameMap.put(EClassMetricName.GUI_DISTANCE, new MetricNameMapping<EClassMetricName>(EClassMetricName.GUI_DISTANCE, "GUD"));
		classMetricNameMap.put(EClassMetricName.CLUSTERING_COEFF, new MetricNameMapping<EClassMetricName>(EClassMetricName.CLUSTERING_COEFF, "CCE"));
		classMetricNameMap.put(EClassMetricName.INSTABILITY, new MetricNameMapping<EClassMetricName>(EClassMetricName.INSTABILITY, "INS"));
		classMetricNameMap.put(EClassMetricName.DISTANCE_MOVED, new MetricNameMapping<EClassMetricName>(EClassMetricName.DISTANCE_MOVED, "DMV"));
		classMetricNameMap.put(EClassMetricName.DISTANCE_MOVED_SINCE_BIRTH, new MetricNameMapping<EClassMetricName>(EClassMetricName.DISTANCE_MOVED_SINCE_BIRTH, "DMB"));
		classMetricNameMap.put(EClassMetricName.LAYER, new MetricNameMapping<EClassMetricName>(EClassMetricName.LAYER, "LAY"));
		classMetricNameMap.put(EClassMetricName.MODIFICATION_FREQUENCY, new MetricNameMapping<EClassMetricName>(EClassMetricName.MODIFICATION_FREQUENCY, "MFR"));
	}
	
	/**
	 * Retrieves the acronym representation of a metric name
	 * @param <E> The metric name enum type
	 * @param metricName The metric name
	 * @return The acronym representation of the metric name
	 */
	public static <E extends Enum<E> & IMetricName> String getMetricAcronym(E metricName)
	{
		//If the metric is a class metric
		if(metricName.getClass() == EClassMetricName.class)
		{
			//Throw an exception if no entry found in class metric name map
			if(!classMetricNameMap.containsKey(metricName))
				throw new NullPointerException("Class metric name map does not contain a mapping for " + metricName);
			else
			{
				//Get the associated metric name mapping
				MetricNameMapping<EClassMetricName> mapping = classMetricNameMap.get(metricName);
				
				//Throw a NullPointerException if the mapping is null,
				//else return it's acronym
				if(mapping == null)
					throw new NullPointerException("Class metric name mapping for " + metricName + " was null");
				else
					return mapping.getAcronym();
			}
		}
		return null;
	}
	
	/**
	 * Retrieves the camel case string representation of a metric name
	 * @param <E> The metric name enum type
	 * @param metricName The metric name
	 * @return The camel case string representation of the metric name
	 */
	public static <E extends Enum<E> & IMetricName> String toCamelString(E metricName)
	{
		//Lower case the metric name string and split it by underscore into tokens
		String[] tokens = metricName.name().toString().toLowerCase().split("_");
				
		StringBuilder builder = new StringBuilder();
		//Add the first token (lower case start) to the output string
		builder.append(tokens[0]);
		
		//For each subsequent token
		for(int i = 1; i < tokens.length; i++)
		{
			//Replace the first character in each token with it's upper case variant
			char firstChar = tokens[i].charAt(0);
			builder.append(tokens[i].replaceFirst(String.valueOf(firstChar),
												  String.valueOf(Character.toUpperCase(firstChar))));
		}
		
		return builder.toString();
	}

	/**
	 * Retrieves the EClassMetricName associated with the given name string input
	 * 
	 * @param nameString The metric name string
	 * @return The EClassMetricName enum that was retrieved from the given name string,
	 * or UNKNOWN if no matching type was found
	 */
	public static EClassMetricName classMetricFromCamelString(String nameString)
	{
		try
		{
			return EClassMetricName.valueOf(nameString.replaceAll("([A-Z]{1})", "_$1").toUpperCase()); 
		}
		catch(IllegalArgumentException iae)
		{
			//TODO: Log error
			return EClassMetricName.UNKNOWN;
		}
	}
}
