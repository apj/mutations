package report.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.ClassMetricData;
import model.MetricUtil;
import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.EEvolutionCategory;
import util.MathUtil;
import util.StatsUtil;
import flanagan.analysis.Regression;

//TODO: Cleanup -- this code is a mess
//TODO: Might serve well to put some of this functionality elsewhere later on
public class ReportBuilderUtil
{
	/***** Model-related *****/
	
	/**
	 * Calculates the age of a system in terms of days, based on the last modification times of the first and last version
	 * @param firstVersionLastModifiedTime The time at which the first version was modified (in days since the epoch)
	 * @param lastVersionLastModifiedTime The time at which the last version was modified (in days since the epoch)
	 * @return The age of the system in terms of days
	 */
	public static int getAgeInDays(long firstVersionLastModifiedTime, long lastVersionLastModifiedTime)
	{
		//Sanity check -- last version mod. time should be larger than the firsts
		if(firstVersionLastModifiedTime > lastVersionLastModifiedTime)
			throw new IllegalArgumentException("First Version Last Modified Time is larger than Last Version Last Modified Time"
												+ "First Version: " + firstVersionLastModifiedTime + ", "
												+ "Last Version: " + lastVersionLastModifiedTime);
		
		int ageInDays = (int) (lastVersionLastModifiedTime - firstVersionLastModifiedTime);
		return ageInDays;
	}
	
	/**
	 * Calculates the age of a system in terms of weeks, based on the last modification times of the first and last version
	 * @param firstVersionLastModifiedTime The time at which the first version was modified (in days since the epoch)
	 * @param lastVersionLastModifiedTime The time at which the last version was modified (in days since the epoch)
	 * @return The age of the system in terms of weeks
	 */
	public static int getAgeInWeeks(long firstVersionLastModifiedTime, long lastVersionLastModifiedTime)
	{
		//Get the age of the system in days, then return the result divided by 7
		int ageInDays = getAgeInDays(firstVersionLastModifiedTime, lastVersionLastModifiedTime);
		return ageInDays / 7;
	}
	
	/***** Gini *****/
	
	/**
	 * Calculates the Gini coefficient for a number of different metrics in a given version
	 * @param version The version containing the class metrics to get the Gini coefficients of
	 * @param classMetrics The metrics to get the Gini coefficients for
	 * @param ignoreZeros Whether to ignore classes that contain 0 for their metric values
	 * @return A map of metric name to the Gini coefficient
	 * @throws Exception - if there was an error in calculating the Gini coefficient for a given metric within the specified input values
	 */
	public static Map<EClassMetricName, Double> getGiniValuesMapForVersion(Version version, EClassMetricName[] classMetrics, boolean ignoreZeros) throws Exception
	{
		Map<EClassMetricName, Double> giniValuesMap = new HashMap<EClassMetricName, Double>(classMetrics.length);
		
		//Calculate the Gini for each metric
		for(EClassMetricName metric : classMetrics)
			giniValuesMap.put(metric, calcGiniCoefficient(version, metric, ignoreZeros));
		
		return giniValuesMap;
	}

	/**
	 * Extracts the values for a given metric for each class in a given version and calculates the Gini coefficient of
	 * the metric values
	 * @param version The version to extract class metric values from
	 * @param metric The metric that the Gini coefficient is being calculated for
	 * @param ignoreZero Whether to ignore classes that have a value of 0 for the given metric
	 * @return The Gini coefficient for the specified metric in the given version
	 * @throws Exception - if there was an error in the calculation of the Gini coefficient from the extracted values
	 */
	public static double calcGiniCoefficient(Version version, EClassMetricName metric, boolean ignoreZero)
			throws Exception
	{
		return calcGiniCoefficient(version, metric, ignoreZero, false);
	}
	
	/**
	 * Extracts the values for a given metric for each class belonging to a specific evolution category in a given version
	 * and calculates the Gini coefficient of the metric values
	 * @param version The version to extract class metric values from
	 * @param metric The metric that the Gini coefficient is being calculated for
	 * @param ignoreZero Whether to ignore classes that have a value of 0 for the given metric
	 * @param evolutionCategory The evolution category of interest that classes belong to in the given version
	 * @return The Gini coefficient for the specified metric in the given version
	 * @throws Exception - if there was an error in the calculation of the Gini coefficient from the extracted values
	 */
	public static double calcGiniCoefficient(Version version, EClassMetricName metric, boolean ignoreZero, EEvolutionCategory evolutionCategory) throws Exception
	{
		List<Double> values = new ArrayList<Double>(version.getClasses().values().size());
		
		//For each class
		for (ClassMetricData classMetricData : version.getClasses().values()) 
		{
			//Skip the class if the Gini coefficient cannot be calculated
			if (!isGiniComputable(classMetricData, metric, ignoreZero) == false) continue;
			
			//If the class belongs to the specified evolution category, add it's value to the list of
			//values to be factored into the calculation of the Gini
			if (classMetricData.getMetricValue(EClassMetricName.EVOLUTION_STATUS) == evolutionCategory.getValue())
				values.add((double)classMetricData.getMetricValue(metric));
		}
		
		return StatsUtil.calcGiniCoefficient(values);
	}

	/**
	 * Extracts the values for a given metric for each class in a given version and calculates the Gini coefficient of
	 * the metric values falling below a given cut off percentile
	 * @param version The version to extract class metric values from
	 * @param metric The metric that the Gini coefficient is being calculated for
	 * @param ignoreZero Whether to ignore classes that have a value of 0 for the given metric
	 * @param cutOffPercentile The percentile at which classes above should be ignored
	 * @return The Gini coefficient for the specified metric in the given version
	 * @throws Exception - if there was an error in the calculation of the Gini coefficient from the extracted values
	 */
	public static double calcGiniCoefficient(Version version, EClassMetricName metric, boolean ignoreZero, double cutOffPercentile) throws Exception
	{
		List<Double> values = new ArrayList<Double>(version.getClasses().values().size());
		int cutOffValue = findValueUnderPercentile(version, cutOffPercentile, metric);
		
		//For each class
		for (ClassMetricData classMetricData : version.getClasses().values()) 
		{
			//Skip the class if the Gini coefficient cannot be calculated
			if (!isGiniComputable(classMetricData, metric, ignoreZero) == false) continue;
			
			//Get the classes value for the metric
			int metricValue = classMetricData.getMetricValue(metric);
			
			//If the metric value is below the cut off value, add it's value to the list of
			//values to be factored into the calculation of the Gini
			if (metricValue < cutOffValue)
			{
				values.add((double)metricValue);
			}
		}
		
		return StatsUtil.calcGiniCoefficient(values);
	}
	
	/**
	 * Extracts the values for a given metric for each class in a given version and calculates the Gini coefficient of
	 * the metric values
	 * @param version The version to extract class metric values from
	 * @param metric The metric that the Gini coefficient is being calculated for
	 * @param ignoreZero Whether to ignore classes that have a value of 0 for the given metric
	 * @param ignoreNew Whether to ignore classes that are new to the given version
	 * @return The Gini coefficient for the specified metric in the given version
	 * @throws Exception - if there was an error in the calculation of the Gini coefficient from the extracted values
	 */
	public static double calcGiniCoefficient(Version version, EClassMetricName metric, boolean ignoreZero, boolean ignoreNew) throws Exception
	{
		List<Double> values = new ArrayList<Double>(version.getClassCount());
		
		//For each class
		for (ClassMetricData classMetricData : version.getClasses().values()) 
		{
			//Skip the class if the Gini coefficient cannot be calculated
			if (!isGiniComputable(classMetricData, metric, ignoreZero)) continue;
			
			if(ignoreNew)
			{
				if(classMetricData.getMetricValue(EClassMetricName.EVOLUTION_STATUS) != EEvolutionCategory.ADDED.getValue())
					values.add((double)classMetricData.getMetricValue(metric));
			}
			else
				values.add((double)classMetricData.getMetricValue(metric));
		}
		
		return StatsUtil.calcGiniCoefficient(values);
	}
	
	/**
	 * Performs sanity checks to determine whether the Gini coefficient can sensibly be calculated for a specified class for the given metric.
	 * 
	 * In the case that the class is an interface type, the check will fail if the Gini being calculated is for one of the following metrics:
	 * - Load count
	 * - Store count
	 * - Branch count
	 * - Field count
	 * - Method call count
	 * - Type construction count
	 * 
	 * Additionally, the check will fail if the ignoreZero flag is set and the value for the specified metric is equal to 0
	 * 
	 * @param classMetricData The class to be checked  
	 * @param metric The metric
	 * @param ignoreZero Whether to ignore the class if it has a value of 0 for the specified metric
	 * @return Whether the Gini coefficient can sensibly be calculated, given the class and metric type
	 */
	private static boolean isGiniComputable(ClassMetricData classMetricData, EClassMetricName metric, boolean ignoreZero)
	{
		if ((metric.equals(EClassMetricName.LOAD_COUNT)) && (classMetricData.getMetricValue(EClassMetricName.IS_INTERFACE) == 1)) return false;
		if ((metric.equals(EClassMetricName.STORE_COUNT)) && (classMetricData.getMetricValue(EClassMetricName.IS_INTERFACE) == 1))  return false;
		if ((metric.equals(EClassMetricName.BRANCH_COUNT)) && (classMetricData.getMetricValue(EClassMetricName.IS_INTERFACE) == 1)) return false;
		if ((metric.equals(EClassMetricName.FIELD_COUNT)) && (classMetricData.getMetricValue(EClassMetricName.IS_INTERFACE) == 1))  return false;
		if ((metric.equals(EClassMetricName.METHOD_CALL_COUNT)) && (classMetricData.getMetricValue(EClassMetricName.IS_INTERFACE) == 1))  return false;
		if ((metric.equals(EClassMetricName.TYPE_CONSTRUCTION_COUNT)) && (classMetricData.getMetricValue(EClassMetricName.IS_INTERFACE) == 1))  return false;
		if (ignoreZero && (classMetricData.getMetricValue(metric) == 0))  return false;
		
		return true;
	}
	
	/***** Beta *****/
	
	/**
	 * Calculates the Beta value for a number of different metrics in a given version
	 * @param version The version containing the class metrics to get the Beta values for
	 * @param classMetrics The metrics to get the Beta values for
	 * @return A map of metric name to the Beta value
	 */
	public static Map<EClassMetricName, Double> getBetaValuesMapForVersion(Version version, EClassMetricName[] classMetrics)
	{
		Map<EClassMetricName, Double> betaValuesMap = new HashMap<EClassMetricName, Double>(classMetrics.length);
		
		for(EClassMetricName metric : classMetrics)
			betaValuesMap.put(metric, getBeta(version, metric));
		
		return betaValuesMap;
	}
	
	/**
	 * 
	 * @param version
	 * @param metric
	 * @return
	 */
	//TODO: Move to stats util
	public static double getBeta(Version version, EClassMetricName metric)
	{
		double sum = getMetricValueSum(version, metric);
		
		if (sum == 0) return 0;
		
		return StatsUtil.calcBeta(sum, version.getClassCount());
	}
	
	
	/**
	 * Sums the value for each class in the given version for a specified metric
	 * @param version The version to sum the class metric values of
	 * @param metric The metric to get the sum of
	 * @return The sum of the metric values for each class in the given version for the specified metric
	 */
	public static int getMetricValueSum(Version version, EClassMetricName metric)
	{
		int sum = 0;
		
		Map<String, ClassMetricData> classes = version.getClasses();

		//For each class, increment the sum by the metric value for the class
		for (ClassMetricData classMetricData : classes.values())
			sum += classMetricData.getMetricValue(metric);
		
		return sum;
	}
	
	/***** Histogram *****/
	
	/**
	 * 
	 * @param versionRelativeFrequencyTableMap
	 * @return
	 */
	public static double computeMaxMinAreaChange(Map<Integer, Map<EClassMetricName, double[]>> versionRelativeFrequencyTableMap)
    {
		try
		{
			return MathUtil.sum(computeMaxMinAreaChangeValues(versionRelativeFrequencyTableMap));
		}
		catch(Exception e)
		{
			//TODO: Log error
			System.err.println("Failed to compute Max Min Area Change...defaulting to -1");
			e.printStackTrace();
			return -1;
		}
    }
	
	/**
	 * 
	 * @param historyRelativeFrequencyTables
	 * @return
	 * @throws Exception
	 */
	public static double[] computeMaxMinAreaChangeValues(Map<Integer, Map<EClassMetricName, double[]>> historyRelativeFrequencyTables) throws Exception
    {
		EClassMetricName[] metrics = mmLong;
		
        double[] areaChangeValues = new double[metrics.length];
        
		for (int i = 0; i < areaChangeValues.length; i++)
			areaChangeValues[i] = computeMaxMinAreaChange(historyRelativeFrequencyTables, metrics[i]);
		
		return areaChangeValues;
    }
    
	/** Compute the total distance betwen max. and min. for all versions
     * percentile value is used to determine the max. value
     * max. value will be used as the cut-off for determining the histogram 
     *  
     */
	
    /**
     * 
     * @param historyRelativeFrequencyTables
     * @param metric
     * @return
     * @throws Exception
     */
    public static double computeMaxMinAreaChange(Map<Integer, Map<EClassMetricName, double[]>> historyRelativeFrequencyTables, EClassMetricName metric) throws Exception
    {
        // set defaults with the first version
    	double[] firstVersionValues = historyRelativeFrequencyTables.get(1).get(metric); 
    	double[] lastVersionValues = historyRelativeFrequencyTables.get(historyRelativeFrequencyTables.keySet().size()).get(metric); 
    	
    	int limit = lastVersionValues.length;
    	
        double[] maxValues = firstVersionValues.clone();
        double[] minValues = firstVersionValues.clone();
        
		// find the maximum and minimum for the field
		for (Map<EClassMetricName, double[]> currentVersionTableMap : historyRelativeFrequencyTables.values())
		{
			double[] currentVersionTable = currentVersionTableMap.get(metric);
			
			for (int i = 0; i < limit; i++)
			{
				if (currentVersionTable[i] > maxValues[i]) maxValues[i] = currentVersionTable[i];
				if (currentVersionTable[i] < minValues[i]) minValues[i] = currentVersionTable[i];
			}
		}
        
        double maxArea = StatsUtil.calcAreaUnderCurve(maxValues);
        double minArea = StatsUtil.calcAreaUnderCurve(minValues);
        
        double changeArea = maxArea - minArea;
        
        //TODO: Log info
        if (changeArea < 0) System.out.println("*** INVESTIGATE AREA-UNDER-CURVE COMPUTATION ***");
        
        return changeArea;    	
    }
	
    /**
     * 
     * @param version
     * @param metric
     * @return
     */
	public static int getMaxValue(Version version, EClassMetricName metric)
	{
		int max = 0;
		
		Map<String, ClassMetricData> classes = version.getClasses();
		
		for (ClassMetricData classMetricData : classes.values())
		{
			try 
			{
				int metricValue = classMetricData.getMetricValue(metric);
				if (metricValue > max) max = metricValue;
			} 
			catch (Exception e) { e.printStackTrace(); }
		}
		return max;
	}
	
	/**
	 * 
	 * @param versionA
	 * @param versionB
	 * @param metric
	 * @param percentile
	 * @return
	 */
	public static double bhattacharyyaDistance(Version versionA, Version versionB, EClassMetricName metric, double percentile)
    {
    	int maxValue = findValueUnderPercentile(versionA, percentile, metric);
       	
    	double[] v1Hist = createRelFreqTable(versionA, metric, maxValue);
    	double[] v2Hist = createRelFreqTable(versionB, metric, maxValue);
    	
    	return StatsUtil.bhattacharyyaMeasure(v1Hist, v2Hist);
    }
    
	/**
	 * 
	 * @param versionAHistogram
	 * @param versionBHistogram
	 * @return
	 */
    public static double bhattacharyyaDistance(double[] versionAHistogram, double[] versionBHistogram)
    {
    	return StatsUtil.bhattacharyyaMeasure(versionAHistogram, versionBHistogram);
    }
    
    /**
	 * 
	 * @param versionAHistogram
	 * @param versionBHistogram
	 * @return
	 */
    public static double bhattacharyyaDistance(int[] versionAHistogram, int[] versionBHistogram)
    {
    	return StatsUtil.bhattacharyyaMeasure(versionAHistogram, versionBHistogram);
    }
	
	/***** Frequency tables *****/

    /**
	 * 
	 * @param version
	 * @param metric
	 * @return
	 */
	public static int[] createFreqTable(Version version, EClassMetricName metric)
	{
		//Ascertain the metric's max value
		int maxValue = getMaxValue(version, metric);
		return createFreqTable(version, metric, maxValue);
	}
    
	/**
	 * 
	 * @param version
	 * @param metric
	 * @param maxValue
	 * @return
	 */
	public static int[] createFreqTable(Version version, EClassMetricName metric, int maxValue)
	{
		//Create a frequency table with the range of values: 0 - maxValue
		int[] frequencyTable = new int[maxValue + 1];
		
		//Assume metric value is 0
		int index = 0;
		
		Map<String, ClassMetricData> classes = version.getClasses();
		
		//For each class
		for (ClassMetricData classMetricData : classes.values())
		{
			try
			{
				int metricValue = classMetricData.getMetricValue(metric);
				
				//If the metric value is greater than the length of the frequency table (i.e., above the max value),
				//clip it's value and increment the count for the last index (i.e., higher than max),
				//else increment the count for the metric value
				index = metricValue >= frequencyTable.length ? index = frequencyTable.length - 1 : metricValue;
				frequencyTable[index] = frequencyTable[index] + 1;
			}
			catch (Exception e) // TODO: should not happen -- check with assert
			{
				e.printStackTrace();
			}
		}
		
		return frequencyTable;
	}
	
	/**
	 * 
	 * @param freqTable
	 * @return
	 */
	public static double[] createRelFreqTable(int[] freqTable)
	{
		return StatsUtil.computeRelativeFreqTable(freqTable);
	}
	
	/**
	 * 
	 * @param version
	 * @param metric
	 * @param maxValue
	 * @return
	 */
	public static double[] createRelFreqTable(Version version, EClassMetricName metric)
	{
		return StatsUtil.computeRelativeFreqTable(createFreqTable(version, metric));
	}
	
	/**
	 * 
	 * @param version
	 * @param metric
	 * @param maxValue
	 * @return
	 */
	public static double[] createRelFreqTable(Version version, EClassMetricName metric, int maxValue)
	{
		return StatsUtil.computeRelativeFreqTable(createFreqTable(version, metric, maxValue));
	}

	/**
	 * 
	 * @param version
	 * @param metric
	 * @return
	 */
	public static double[] createCumlFreqTable(Version version, EClassMetricName metric)
	{
		return StatsUtil.computeCummulFreqTable(createFreqTable(version, metric));
	}
	
//	/** Compute Cuml Freq Table, then returns the value at Percentage
//	 *  This can be used to detect the metric value at which say under 90% of the data fall.
//	 *  This 90% is the parameter that is passed in.
//	 *  If percentValue is outside the range 0 - 1, then 1 is assumed
//	 */
	
	/**
	 * 
	 * @param version
	 * @param metric
	 * @param maxValue
	 * @return
	 */
	public static double[] createCumlFreqTable(Version version, EClassMetricName metric, int maxValue)
	{
		return StatsUtil.computeCummulFreqTable(createFreqTable(version, metric, maxValue));
	}
	
	/**
	 * 
	 * @param version
	 * @param percentValue
	 * @param metric
	 * @return
	 */
	public static int findValueUnderPercentile(Version version, double percentValue, EClassMetricName metric)
	{
		double pv = percentValue;
		if ((percentValue < 0.0) || (percentValue > 1.0)) pv = 1.0;
		
		double[] cumlFreq = createCumlFreqTable(version, metric);
		
		for (int i = 0; i < cumlFreq.length; i++)
		{
			if (cumlFreq[i] > pv) return i;
		}
		
		return cumlFreq.length;
	}
	
	/***** Regression *****/
	
	/**
	 * 
	 * @param metricGrowthValues
	 * @return
	 */
    public static String getGrowthRateType(List<Integer> metricGrowthValues)
    {
    	int versionCount = metricGrowthValues.size();
    	
    	//Populate xValues with version numbers
    	double[] xValues = new double[versionCount];
		for (int i = 0; i < xValues.length; i++)
			xValues[i] = i + 1;
    		
    	//Retrieve metric count yValues
    	double[] yValues = new double[xValues.length];
    	int startIndex = (versionCount - xValues.length);
    	
		for (int i = startIndex; i < versionCount; i++)
			yValues[i - startIndex] = metricGrowthValues.get(i);
    	
    	Regression regression = new Regression(xValues, yValues);
    	regression.polynomial(2);
    	
    	double[] coeffs = regression.getBestEstimates();
    	
		String type = "Linear";
		if (coeffs[2] < -0.01) type = "Sub-Linear";
		if (coeffs[2] > 0.01) type = "Super-Linear";
    	
    	if (regression.getAdjustedR2() < 0.65) type += "*";  // model does not fit too well
    	
    	return type;
    }

    /***** Class change *****/
    
    /**
     * 
     * @param version
     * @return
     */
	public static int getModifiedClassCount(Version version)
	{
		return MetricUtil.getEvolutionCategoryClassCount(version, EEvolutionCategory.MODIFIED);
	}
	
	/**
	 * 
	 * @param version
	 * @return
	 */
	public static int getAddedClassCount(Version version)
	{
		return MetricUtil.getEvolutionCategoryClassCount(version, EEvolutionCategory.ADDED);
	}
	
	
	/***** Metric sets *****/
	
	private static final EClassMetricName[] mmLong = 
	{ 
		EClassMetricName.METHOD_COUNT,
		EClassMetricName.PUBLIC_METHOD_COUNT,
		EClassMetricName.FIELD_COUNT,
		EClassMetricName.BRANCH_COUNT,
		EClassMetricName.IN_DEGREE_COUNT, 
		EClassMetricName.OUT_DEGREE_COUNT,
		EClassMetricName.EXTERNAL_OUT_DEGREE_COUNT,
		EClassMetricName.METHOD_CALL_COUNT,
		EClassMetricName.TYPE_CONSTRUCTION_COUNT,
		EClassMetricName.EXCEPTION_COUNT,
		EClassMetricName.INNER_CLASS_COUNT,
		EClassMetricName.INTERNAL_METHOD_CALL_COUNT,
		EClassMetricName.EXTERNAL_METHOD_CALL_COUNT,
		EClassMetricName.EXTERNAL_LIB_METHOD_CALL_COUNT,
		EClassMetricName.INTERNAL_LIB_METHOD_CALL_COUNT,
		EClassMetricName.LOAD_COUNT,
		EClassMetricName.STORE_COUNT
	};
	
	private static EClassMetricName[] mmGini =
	{ 
		EClassMetricName.METHOD_COUNT,
		EClassMetricName.PUBLIC_METHOD_COUNT,
		EClassMetricName.FIELD_COUNT,
		EClassMetricName.BRANCH_COUNT,
		EClassMetricName.IN_DEGREE_COUNT,
		EClassMetricName.OUT_DEGREE_COUNT,
		EClassMetricName.METHOD_CALL_COUNT,
		EClassMetricName.TYPE_CONSTRUCTION_COUNT,
		EClassMetricName.EXCEPTION_COUNT,
		EClassMetricName.INNER_CLASS_COUNT,
		EClassMetricName.NUMBER_OF_DESCENDANTS,
		EClassMetricName.DEPTH_IN_INHERITANCE_TREE,
		EClassMetricName.NUMBER_OF_CHILDREN,
		EClassMetricName.LOAD_COUNT,
		EClassMetricName.STORE_COUNT
	};
	
	private static final EClassMetricName[] growthMetrics = 
	{ 
		EClassMetricName.METHOD_COUNT,
		EClassMetricName.PUBLIC_METHOD_COUNT,
		EClassMetricName.FIELD_COUNT,
		EClassMetricName.INSTRUCTION_COUNT,
		EClassMetricName.BRANCH_COUNT,
		EClassMetricName.IN_DEGREE_COUNT,
		EClassMetricName.OUT_DEGREE_COUNT,
		EClassMetricName.METHOD_CALL_COUNT,
		EClassMetricName.TYPE_CONSTRUCTION_COUNT,
		EClassMetricName.SUPER_CLASS_COUNT
	};
	
	public static EClassMetricName[] getMMLongMetrics()
	{
		return mmLong;
	}
	
	public static EClassMetricName[] getMMGiniMetrics()
	{
		return mmGini;
	}
	
	public static EClassMetricName[] getGrowthMetrics()
	{
		return growthMetrics;
	}
}