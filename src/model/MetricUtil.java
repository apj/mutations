package model;

import java.util.Collection;
import java.util.Map;

import util.MathUtil;

import model.vocab.EClassMetricName;
import model.vocab.EEvolutionCategory;
import model.vocab.EVersionMetricName;
import model.vocab.IMetricName;

public class MetricUtil
{	
	//TODO: Is there a better way of handling the enum generics in this case than an interface?
	public static <E extends Enum<E> & IMetricName> void incrementMetricValue(Map<E, Integer> metricMap, E metric, int value) 
	{
		if(!metricMap.containsKey(metric))
			throw new NullPointerException("Cannot increment value of " + metric + ", it does not currently exist in the metric map");
		
		Integer currentValue = metricMap.get(metric);
		
		if(currentValue == null)
			throw new NullPointerException("Cannot increment value of " + metric + ", it's value is currently null");
		
		setMetricValue(metricMap, metric, currentValue + value);
	}
	
	public static <E extends Enum<E> & IMetricName> void incrementMetricValue(Map<E, Integer> metricMap, E metric) 
	{
		incrementMetricValue(metricMap, metric, 1);
	}
	
	public static <E extends Enum<E> & IMetricName> void setMetricValue(Map<E, Integer> metricMap, E metric, int value) 
	{
		if(metric == null)
			throw new NullPointerException("Cannot set value for metric in the metric map, as the metric name specified was null.");
		
		if(metricMap == null)
			throw new NullPointerException("Cannot set value of " + metric + " in the metric map, as the given metric map was null");
			
		metricMap.put(metric, value);
	}
	
	public static int computeDistanceMoved(ClassMetricData classMetricData)
	{
		double distanceMoved = 0.0;
	    EClassMetricName[] distanceMetrics = getDistanceMetrics();
		
		for (EClassMetricName metric : distanceMetrics)
		{
			try 
			{
				distanceMoved += MathUtil.square(classMetricData.getMetricValue(metric));
			} catch (Exception e) {e.printStackTrace();} // lets hope it does not get to this
		}
		   	
        return MathUtil.scaleDoubleValue(distanceMoved, 10, 1000.0);
	}
	
	public static int getDistanceBetween(ClassMetricData classA, ClassMetricData classB)
	{
		double distanceValue = distanceBetween(classA, classB);
		int evolutionDistance = MathUtil.scaleDoubleValue(distanceValue, 100, 1000);
		
		return evolutionDistance;
	}
	
	public static double distanceBetween(ClassMetricData classA, ClassMetricData classB)
	{
		double distanceValue = 0;
		
		EClassMetricName[] classMetrics = getDistanceMetrics();
		
		for(EClassMetricName metric : classMetrics)
			distanceValue += MathUtil.square(classB.getMetricValue(metric) - classA.getMetricValue(metric));
		
		return distanceValue;
	}

	public static int computeModifiedMetrics(ClassMetricData classA, ClassMetricData classB)
	{
		int modifiedCount = 0;
		
		EClassMetricName[] classMetrics = MetricUtil.getComparisonMetrics();
		
		for(EClassMetricName metric : classMetrics)
			if(classA.getMetricValue(metric).intValue() != classB.getMetricValue(metric).intValue())
				modifiedCount++;
		
		return modifiedCount;
	}

	public static int getVersionMetricCount(Version version, EVersionMetricName metric)
	{
		int total = 0;
		
		switch(metric)
		{
			case CLASS_COUNT:
				total = version.getClassCount();
				break;
			case METHOD_COUNT:
				total = getMethodCount(version);
				break;
			case FIELD_COUNT:
				total = getFieldCount(version);
				break;
		}
		
		return total;
	}
	
	private static int getMethodCount(Version version)
	{
		Map<String, ClassMetricData> classes = version.getClasses();
		
		int methodCount = 0;
		
		for(ClassMetricData classMetricData : classes.values())
			methodCount += classMetricData.getMethodCount();
		
		return methodCount;
	}
	
	private static int getFieldCount(Version version)
	{
		Map<String, ClassMetricData> classes = version.getClasses();
		
		int fieldCount = 0;
		
		for(ClassMetricData classMetricData : classes.values())
			fieldCount += classMetricData.getMetricValue(EClassMetricName.FIELD_COUNT);
		
		return fieldCount;
	}
	
	public static int getEvolutionDistanceSinceBirth(ClassMetricData aClass, ClassMetricData ancestor)
	{
		double distanceValue = getDistanceBetween(aClass, ancestor);
		int distanceMovedSinceBirth = MathUtil.round(distanceValue);
		
		if(distanceValue > 0 && distanceMovedSinceBirth < 1) distanceMovedSinceBirth = 1;
		if(distanceValue == 0) distanceMovedSinceBirth = 0;
		
		return distanceMovedSinceBirth;
	}

	// TODO: Extract to model/util
	public static double dependenciesSubSetPercentage(ClassMetricData classMetricData1, ClassMetricData classMetricData2)
	{
		int matchCount = 0;
		
		if (classMetricData1.getDependencyCount() == 0) return 1.0;
		
		for (String dependency : classMetricData1.getDependencies())
		{
			if (classMetricData2.getDependencies().contains(dependency)) matchCount++; // next version does not contain it
		}
		return (double) matchCount / classMetricData1.getDependencies().size();
	}

	// TODO: Extract to model/util
	public static double usersSubSetPercentage(ClassMetricData classMetricData1, ClassMetricData classMetricData2)
	{
		int matchCount = 0;
		
		if (classMetricData1.getUserCount() == 0) return 1.0;
		
		for (String user : classMetricData1.getUsers())
		{
			if (classMetricData2.getUsers().contains(user)) matchCount++; // next version does not contain it
		}
		return (double) matchCount / classMetricData1.getUserCount();
	}

	// TODO: Extract to model/util
	public static double fieldSubSetPercentage(ClassMetricData classMetricData1, ClassMetricData classMetricData2)
	{
		int matchCount = 0;
		
		if (classMetricData1.getFieldCount() == 0) return 1.0;
		
		for (String field : classMetricData1.getFields())
		{
			if (classMetricData2.getFields().contains(field)) matchCount++; // next version does not
														// contain it
		}
		return (double) matchCount / classMetricData1.getFieldCount();
	}

	// TODO: Extract to model/util
	/**
	 * Compares methods in two classes -- true if cm1 is a subset of cm2 in
	 * terms of methods
	 */
	public static double methodSubSetPercentage(ClassMetricData classMetricData1, ClassMetricData classMetricData2)
	{
		int matchCount = 0;
		
		if (classMetricData1.getShortMethodCount() == 0) return 1.0;
		
		for (String shortMethod : classMetricData1.getShortMethods())
		{
			if (classMetricData2.getShortMethods().contains(shortMethod)) matchCount++; // next version
																// does not
																// contain it
		}
		return (double) matchCount / classMetricData1.getShortMethods().size();
	}

	// TODO: Extract to model/util
	/** CM1 and CM2 have the exact same set of methods */
	public static boolean exactMethodMatch(ClassMetricData classMetricData1, ClassMetricData classMetricData2)
	{
		if (classMetricData1.getMethodCount() != classMetricData2.getMethodCount()) return false;
		
		for (String method : classMetricData1.getMethods())
		{
			if (!classMetricData2.getMethods().contains(method)) return false;
		}
		return true;
	}
	
	public static int getEvolutionCategoryClassCount(Version version, EEvolutionCategory evolutionCategory)
	{
		Map<String, ClassMetricData> versionClassesMap = version.getClasses();
		Collection<ClassMetricData> classes = versionClassesMap.values();
		
		int evolutionCategoryClassCount = 0;
		
		for(ClassMetricData classMetricData : classes)
		{
			if(evolutionCategory == EEvolutionCategory.DELETED)
			{
				if(evolutionCategory.getValue() == classMetricData.getMetricValue(EClassMetricName.NEXT_VERSION_STATUS).intValue())
					evolutionCategoryClassCount++;
			}
			else
			{
				if(evolutionCategory.getValue() == classMetricData.getMetricValue(EClassMetricName.EVOLUTION_STATUS).intValue())
					evolutionCategoryClassCount++;
			}
		}
		
		return evolutionCategoryClassCount;
	}
	
	public static EClassMetricName[] getComparisonMetrics()
	{
		return new EClassMetricName[]
		{
				EClassMetricName.BRANCH_COUNT,
				EClassMetricName.CONSTANT_LOAD_COUNT,
				EClassMetricName.I_LOAD_COUNT,
				EClassMetricName.I_STORE_COUNT,
				EClassMetricName.LOAD_FIELD_COUNT,
				EClassMetricName.STORE_FIELD_COUNT,
				EClassMetricName.REF_LOAD_OP_COUNT,
				EClassMetricName.REF_STORE_OP_COUNT,
				EClassMetricName.SUPER_CLASS_COUNT,
				EClassMetricName.INNER_CLASS_COUNT,
				EClassMetricName.EXCEPTION_COUNT,
				EClassMetricName.STATIC_METHOD_COUNT,
				EClassMetricName.SYNCHRONIZED_METHOD_COUNT,
				EClassMetricName.FINAL_METHOD_COUNT,
				EClassMetricName.STATIC_FIELD_COUNT,
				EClassMetricName.FINAL_FIELD_COUNT,
				EClassMetricName.IS_ABSTRACT,
				EClassMetricName.IS_PUBLIC,
				EClassMetricName.IS_PRIVATE,
				EClassMetricName.IS_INTERFACE,
				EClassMetricName.IS_PROTECTED,
				EClassMetricName.IS_EXCEPTION,
				EClassMetricName.OUT_DEGREE_COUNT,
				EClassMetricName.INTERFACE_COUNT,
				EClassMetricName.METHOD_COUNT,
				EClassMetricName.TRY_CATCH_BLOCK_COUNT,
				EClassMetricName.TYPE_INSN_COUNT,
				EClassMetricName.FIELD_COUNT,
				EClassMetricName.METHOD_CALL_COUNT,
				EClassMetricName.PUBLIC_METHOD_COUNT,
				EClassMetricName.PRIVATE_METHOD_COUNT,
				EClassMetricName.ABSTRACT_METHOD_COUNT,
				EClassMetricName.PROTECTED_METHOD_COUNT,
				EClassMetricName.PRIVATE_FIELD_COUNT,
				EClassMetricName.PROTECTED_FIELD_COUNT,
				EClassMetricName.PUBLIC_FIELD_COUNT,
				EClassMetricName.ZERO_OP_INSN_COUNT,
				EClassMetricName.LOCAL_VAR_COUNT,
				EClassMetricName.INTERNAL_METHOD_CALL_COUNT,
				EClassMetricName.EXTERNAL_METHOD_CALL_COUNT,
				EClassMetricName.INTERNAL_OUT_DEGREE_COUNT,
				EClassMetricName.INCREMENT_OP_COUNT,
				EClassMetricName.THROW_COUNT,
				EClassMetricName.CLASS_CONSTRUCTOR_COUNT
		};
	}
	
	public static EClassMetricName[] getDistanceMetrics()
	{
		return new EClassMetricName[]
		{
				EClassMetricName.BRANCH_COUNT,
				EClassMetricName.CONSTANT_LOAD_COUNT,
				EClassMetricName.I_LOAD_COUNT,
				EClassMetricName.I_STORE_COUNT,
				EClassMetricName.LOAD_FIELD_COUNT,
				EClassMetricName.STORE_FIELD_COUNT,
				EClassMetricName.REF_LOAD_OP_COUNT,
				EClassMetricName.REF_STORE_OP_COUNT,
				EClassMetricName.SUPER_CLASS_COUNT,
				EClassMetricName.INNER_CLASS_COUNT,
				EClassMetricName.EXCEPTION_COUNT,
				EClassMetricName.OUT_DEGREE_COUNT,
				EClassMetricName.INTERFACE_COUNT,
				EClassMetricName.METHOD_COUNT,
				EClassMetricName.TRY_CATCH_BLOCK_COUNT,
				EClassMetricName.TYPE_INSN_COUNT,
				EClassMetricName.FIELD_COUNT,
				EClassMetricName.METHOD_CALL_COUNT,
				EClassMetricName.PUBLIC_METHOD_COUNT,
				EClassMetricName.PRIVATE_METHOD_COUNT,
				EClassMetricName.PROTECTED_METHOD_COUNT,
				EClassMetricName.ABSTRACT_METHOD_COUNT,
				EClassMetricName.PRIVATE_FIELD_COUNT,
				EClassMetricName.PROTECTED_FIELD_COUNT,
				EClassMetricName.PUBLIC_FIELD_COUNT,
				EClassMetricName.ZERO_OP_INSN_COUNT,
				EClassMetricName.LOCAL_VAR_COUNT,
				EClassMetricName.INTERNAL_METHOD_CALL_COUNT,
				EClassMetricName.EXTERNAL_METHOD_CALL_COUNT,
				EClassMetricName.INTERNAL_OUT_DEGREE_COUNT,
				EClassMetricName.INCREMENT_OP_COUNT,
				EClassMetricName.THROW_COUNT,
				EClassMetricName.CLASS_CONSTRUCTOR_COUNT
		};
	}
}
