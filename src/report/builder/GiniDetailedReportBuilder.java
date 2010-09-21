package report.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.EEvolutionCategory;
import model.vocab.EVersionMetricName;
import model.vocab.MetricNameMappingUtil;
import report.table.Column;
import report.table.DecimalColumn;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import util.MathUtil;
import extraction.VersionFactory;

/**
 * Builds a report that shows different Gini related calculations for a number of
 * different different metrics.
 * 
 * The values displayed include:
 * 
 * - Gini (Gini coefficient for a given metric)
 * - Gini95 (Gini coefficient of classes that fall below the 95 percentile)
 * - Gini80 (Gini coefficient of classes that fall below the 80 percentile)
 * - zGini (Gini coefficient, ignoring classes with a metric value of 0)
 * - zGini95 (Gini coefficient of classes that fall below the 95 percentile, ignoring classes with a metric value of 0)
 * - zGini80 (Gini coefficient of classes that fall below the 80 percentile, ignoring classes with a metric value of 0)
 * - modGini (Gini coefficient for only classes that have been modified in a given version)
 * - newGini (Gini coefficient for only classes that are new to a given version)
 * - noNewGini (Gini coefficient for only classes that are NOT new to a given version)
 * - noNewZGini (Gini coefficient for only classes that are NOT new to a given version, ignoring classes with a metric value of 0) 
 * 
 * @author Allan Jones
 */
public class GiniDetailedReportBuilder extends TabularReportBuilder
{
	//Map of rsn to metric map
	private Map<Integer, Map<EVersionMetricName, Integer>> versionMetricsMap = new TreeMap<Integer, Map<EVersionMetricName, Integer>>();
	//Map of rsn to metric to gini variants
	private Map<Integer, Map<EClassMetricName, Map<String, Double>>> giniHistory = new TreeMap<Integer, Map<EClassMetricName,Map<String,Double>>>();

	@Override
	protected String getHeader()
	{
		StringBuilder header = new StringBuilder(20);
		String separator = config.getSeparator();
		
		header.append("Name" + separator);
		header.append("Type" + separator);
		header.append("Metric" + separator);
		header.append("RSN" + separator);
		header.append("ID" + separator);
		header.append("Size" + separator);
		header.append("Modified" + separator);
		header.append("New" + separator);
		header.append("Gini" + separator);
		header.append("Gini95" + separator);
		header.append("Gini80" + separator);
		header.append("zGini" + separator);
		header.append("zGini95" + separator);
		header.append("zGini80" + separator);
		header.append("modGini" + separator);
		header.append("newGini" + separator);
		header.append("noNewGini" + separator);
		header.append("noNewZGini" + separator);
		header.append("Movement" + separator);
		header.append("giniTrigger" + separator);
		header.append("zGiniTrigger");
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		extractHistoryInformation();
		
		EClassMetricName[] metrics = ReportBuilderUtil.getMMGiniMetrics();
		List<Row> rows = new ArrayList<Row>();
		
		try
		{
			String shortName = history.getShortName();
			String appType = history.getSystemType();

			//For each metric
			for (EClassMetricName metric : metrics)
				//For each version
				for (Integer rsn : history.getVersions().keySet())
				{
					String id = history.getVersions().get(rsn);
					//Extract a row of Gini values for the given metric in the given version
					rows.add(extractGiniRow(rsn, id, metric, MetricNameMappingUtil.toCamelString(metric), shortName, appType));
				}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return rows;
	}
	
	/**
	 * Extracts information from the systems history for post-processing.
	 * In particular, version-level metrics and Gini values for a number of different metrics
	 * are extracted.
	 */
	private void extractHistoryInformation()
	{
		//Get the array of metrics to calculate Gini coefficients for
		EClassMetricName[] metrics = ReportBuilderUtil.getMMGiniMetrics();
		
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		//For each version
		for(Integer rsn : history.getVersions().keySet())
		{
			Version version = versionFactory.getVersion(history.getShortName(), rsn);
			
			//Extract version-level metrics
			extractVersionMetrics(version);
			//Extract Gini coefficients
			extractVersionGiniValues(version, metrics);
		}
	}
	
	/**
	 * Extracts version-level metrics from the specified version, including:
	 * 
	 * - Days since birth
	 * - Class count
	 * - Added class count
	 * - Modified class count
	 * 
	 * @param version The version to extract metrics from
	 */
	private void extractVersionMetrics(Version version)
	{
		Map<EVersionMetricName, Integer> versionMetrics = new HashMap<EVersionMetricName, Integer>();
		
		versionMetrics.put(EVersionMetricName.DAYS_SINCE_BIRTH, version.getDaysSinceBirth());
		versionMetrics.put(EVersionMetricName.CLASS_COUNT, version.getClassCount());
		versionMetrics.put(EVersionMetricName.ADDED_CLASS_COUNT, ReportBuilderUtil.getAddedClassCount(version));
		versionMetrics.put(EVersionMetricName.MODIFIED_CLASS_COUNT, ReportBuilderUtil.getModifiedClassCount(version));
		
		versionMetricsMap.put(version.getRSN(), versionMetrics);
	}

	/**
	 * Extracts a number of Gini coefficient related values from the given version for each of the specified metrics
	 * 
	 * @param version The version containing the class metrics that will be used in the calculation of the Gini values
	 * @param metrics The array of metrics to calculate Gini values for
	 */
	private void extractVersionGiniValues(Version version, EClassMetricName[] metrics)
	{
		Map<EClassMetricName, Map<String, Double>> versionGiniValuesMap = new HashMap<EClassMetricName, Map<String,Double>>(metrics.length);
		
		int rsn = version.getRSN();
		
		for(EClassMetricName metric : metrics)
		{
			Map<String, Double> metricGiniValues = new HashMap<String, Double>(15);
			
			try
			{
				double gini = ReportBuilderUtil.calcGiniCoefficient(version, metric, false);
				double zGini = ReportBuilderUtil.calcGiniCoefficient(version, metric, true);
				
				//Assume the first version is being processed
				double deltaGini = 0;
				double previousGini = 0;
				double deltaZGini = 0;
				double previousZGini = 0;
				
				metricGiniValues.put(GINI, gini);
				metricGiniValues.put(GINI_95, ReportBuilderUtil.calcGiniCoefficient(version, metric, false, .95)); 
				metricGiniValues.put(GINI_80, ReportBuilderUtil.calcGiniCoefficient(version, metric, false, .80));
		
				metricGiniValues.put(Z_GINI, zGini);
				metricGiniValues.put(Z_GINI_95, ReportBuilderUtil.calcGiniCoefficient(version, metric, true, .95));
				metricGiniValues.put(Z_GINI_80, ReportBuilderUtil.calcGiniCoefficient(version, metric, true, .80));
				
				metricGiniValues.put(MOD_GINI, ReportBuilderUtil.calcGiniCoefficient(version, metric, false, EEvolutionCategory.MODIFIED));
				metricGiniValues.put(NEW_GINI, ReportBuilderUtil.calcGiniCoefficient(version, metric, false, EEvolutionCategory.ADDED));
				
				metricGiniValues.put(NO_NEW_GINI, ReportBuilderUtil.calcGiniCoefficient(version, metric, false, true));
				metricGiniValues.put(NO_NEW_Z_GINI, ReportBuilderUtil.calcGiniCoefficient(version, metric, true, true));
				
				//If a comparison can be made between two version (i.e. we are not currently processing the first version)
				if(rsn > 1)
				{
					 previousGini = giniHistory.get(rsn - 1).get(metric).get(GINI);
					 previousZGini = giniHistory.get(rsn - 1).get(metric).get(Z_GINI);
					 deltaGini = gini - previousGini;
					 deltaZGini = zGini - previousZGini;
				}
				
				metricGiniValues.put(DELTA_GINI, deltaGini);
				metricGiniValues.put(PREVIOUS_GINI, previousGini);
				metricGiniValues.put(DELTA_Z_GINI, deltaZGini);
				metricGiniValues.put(PREVIOUS_Z_GINI, previousZGini);
				
				versionGiniValuesMap.put(metric, metricGiniValues);
			}
			catch (Exception e)
			{
				//TODO: Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		giniHistory.put(rsn, versionGiniValuesMap);
	}

	/**
	 * Extracts a row containing various Gini-related values for a given metric in a given version
	 * @param rsn The specified version
	 * @param rsn The specified version
	 * @param metric The metric to determine Gini values for
	 * @param metricString A string representation of the metric
	 * @param shortName The systems short name
	 * @param appType The systems type
	 * @return The extracted row containing pertinent Gini values for the given metric in the given version
	 */
	private Row extractGiniRow(Integer rsn, String id, EClassMetricName metric, String metricString, String shortName, String appType)
	{
		Column[] columns = null;
		
		Map<EVersionMetricName, Integer> versionMetrics = versionMetricsMap.get(rsn);
		Map<String, Double> metricGiniValues = giniHistory.get(rsn).get(metric);
		
		columns = new Column[9 + 10 + 3];

		// Name
		columns[0] = new StringColumn(shortName);
		// Typr
		columns[1] = new StringColumn(appType);
		// Metric
		columns[2] = new StringColumn(metricString);
		// RSN
		columns[3] = new IntegerColumn(rsn);
		// ID
		columns[4] = new StringColumn(id);
		// Days since birth
		columns[5] = new IntegerColumn(versionMetrics.get(EVersionMetricName.DAYS_SINCE_BIRTH));
		// Class count
		columns[6] = new IntegerColumn(versionMetrics.get(EVersionMetricName.CLASS_COUNT));
		// Modified class count
		columns[7] = new IntegerColumn(versionMetrics.get(EVersionMetricName.MODIFIED_CLASS_COUNT));
		// New class count
		columns[8] = new IntegerColumn(versionMetrics.get(EVersionMetricName.ADDED_CLASS_COUNT));
		
		String columnFormatting = "#.###";
		// Gini
		columns[9] = new DecimalColumn(metricGiniValues.get(GINI), columnFormatting);
		// Gini 95
		columns[10] = new DecimalColumn(metricGiniValues.get(GINI_95), columnFormatting);
		// Gini 80
		columns[11] = new DecimalColumn(metricGiniValues.get(GINI_80), columnFormatting);
		// zGini
		columns[12] = new DecimalColumn(metricGiniValues.get(Z_GINI), columnFormatting);
		// zGini95
		columns[13] = new DecimalColumn(metricGiniValues.get(Z_GINI_95), columnFormatting);
		// zGini80
		columns[14] = new DecimalColumn(metricGiniValues.get(Z_GINI_80), columnFormatting);
		// modGini
		columns[15] = new DecimalColumn(metricGiniValues.get(MOD_GINI), columnFormatting);
		// newGini
		columns[16] = new DecimalColumn(metricGiniValues.get(NEW_GINI), columnFormatting);
		// noNewGini
		columns[17] = new DecimalColumn(metricGiniValues.get(NO_NEW_GINI), columnFormatting);
		// noNewZGini
		columns[18] = new DecimalColumn(metricGiniValues.get(NO_NEW_Z_GINI), columnFormatting);

		double previousGini = metricGiniValues.get(PREVIOUS_GINI);
		double previousZGini = metricGiniValues.get(PREVIOUS_Z_GINI);
		double deltaGini = metricGiniValues.get(DELTA_GINI);
		double deltaZGini = metricGiniValues.get(DELTA_Z_GINI);
		double range = deltaGini - deltaZGini;
		
		// TODO: Rename 2 variables below...what are they?
		String opp = "";
		String tg = "";

		String giniOverLimit = " ";
		String zGiniOverLimit = " ";

		if (((deltaGini > 0) && (deltaZGini < 0)) || ((deltaGini < 0) && (deltaZGini > 0))) opp = "#";
		if (MathUtil.abs(range) > 0.02) tg = "*";

		if (deltaGini / previousGini > 0.05) giniOverLimit = "G";
		if (deltaZGini / previousZGini > 0.05) zGiniOverLimit = "Z";

		columns[19] = new StringColumn(tg + opp);
		columns[20] = new StringColumn(giniOverLimit);
		columns[21] = new StringColumn(zGiniOverLimit);

		return new Row(columns);
	}
	
	//Gini variant name constants
	private static final String GINI = "gini";
	private static final String GINI_95 = "gini95";
	private static final String GINI_80 = "gini80";
	private static final String Z_GINI = "zGini";
	private static final String Z_GINI_95 = "zGini95";
	private static final String Z_GINI_80 = "zGini80";
	private static final String MOD_GINI = "modGini";
	private static final String NEW_GINI = "newGini";
	private static final String NO_NEW_GINI = "noNewGini";
	private static final String NO_NEW_Z_GINI = "noNewZGini";
	private static final String DELTA_GINI = "deltaGini";
	private static final String PREVIOUS_GINI = "previousGini";
	private static final String DELTA_Z_GINI = "deltaZGini";
	private static final String PREVIOUS_Z_GINI = "previousZGini";
}