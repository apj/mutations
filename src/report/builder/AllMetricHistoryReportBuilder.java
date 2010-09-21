package report.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import model.ClassMetricData;
import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.MetricNameMappingUtil;
import report.EReportConfigOption;
import report.table.Column;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.VersionFactory;

/**
 * Builds a report that contains the history of values for a given metric for each of the classes in the system, for each version
 * 
 * @author Allan Jones
 */
public class AllMetricHistoryReportBuilder extends TabularReportBuilder
{
	private static EClassMetricName[] metrics;
	
	static
	{
		Set<EClassMetricName> ignoreMetrics = new HashSet<EClassMetricName>();
		
		ignoreMetrics.add(EClassMetricName.ACCESS);
		ignoreMetrics.add(EClassMetricName.SCOPE);
		ignoreMetrics.add(EClassMetricName.CLASS_NAME);
		ignoreMetrics.add(EClassMetricName.CLASS_TYPE);
		ignoreMetrics.add(EClassMetricName.SUPER_CLASS_NAME);
		ignoreMetrics.add(EClassMetricName.OUTER_CLASS_NAME);
		ignoreMetrics.add(EClassMetricName.PACKAGE_NAME);
		ignoreMetrics.add(EClassMetricName.SHORT_CLASS_NAME);
		
		ignoreMetrics.add(EClassMetricName.UNKNOWN);
		
		List<EClassMetricName> pertinentMetrics = new ArrayList<EClassMetricName>();
		
		for(EClassMetricName metric : EClassMetricName.values())
		{
			if(!ignoreMetrics.contains(metric))
				pertinentMetrics.add(metric);
		}
		
		metrics = new EClassMetricName[pertinentMetrics.size()];
		
		for(int i = 0; i < metrics.length; i++)
			metrics[i] = pertinentMetrics.get(i);
	}

	@Override
	protected String getHeader()
	{
		StringBuilder header = new StringBuilder();
		
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		
		header.append("Class Name").append(separator).append("Metric Name");
		
		for(String versionId : history.getVersions().values())
			header.append(separator).append(versionId);
		
		return header.toString();
	}
	
	@Override
	protected List<Row> getRows()
	{	
		Map<String, Map<EClassMetricName, Integer[]>> historyMap = new TreeMap<String, Map<EClassMetricName, Integer[]>>();
		extractClassMetricHistoryMap(historyMap);
		
		List<Row> reportRows = new ArrayList<Row>(historyMap.size());
		
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		
		int releases = history.getReleaseCount();
		
		//For each metric
		for(EClassMetricName metric : metrics)
			//For each class, extract a row containing the history of values for the given metric
			for (String className : historyMap.keySet())
				reportRows.add(extractMetricValueRow(className, historyMap, releases, separator, metric));
		
		return reportRows;
	}
	
	/**
	 * Creates a map of class names to their history of values for the specified metric
	 * @param metric The metric to extract the history for
	 * @return The map of class names to their history of values for the specified metrics
	 */
	public void extractClassMetricHistoryMap(Map<String, Map<EClassMetricName, Integer[]>> historyMap)
	{
		String shortName = history.getShortName();
		int releaseCount = history.getReleaseCount();
		
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		for(Integer rsn : history.getVersions().keySet())
		{
			Version version = versionFactory.getVersion(shortName, rsn);
			
			Map<String, Map<EClassMetricName, Integer>> classNameMetricMap = new HashMap<String, Map<EClassMetricName,Integer>>();
			getClassNameMetricMap(classNameMetricMap, version);
			
			Set<Entry<String, Map<EClassMetricName, Integer>>> classMetricEntries = classNameMetricMap.entrySet();
			
			for (Entry<String, Map<EClassMetricName, Integer>> classMetricEntry : classMetricEntries)
			{
				String className = classMetricEntry.getKey();

				Map<EClassMetricName, Integer[]> metricMap = historyMap.get(className);
				
				// first time, create the array object
				if (metricMap == null)
				{
					metricMap = new HashMap<EClassMetricName, Integer[]>(metrics.length);
					historyMap.put(className, metricMap);
				}

				Set<Entry<EClassMetricName, Integer>> metricEntries = classMetricEntry.getValue().entrySet(); 
				
				for(Entry<EClassMetricName, Integer> metricEntry : metricEntries)
				{
					EClassMetricName metric = metricEntry.getKey();
					Integer[] values = metricMap.get(metric);
					
					if(values == null)
					{
						values = new Integer[releaseCount];
						metricMap.put(metric, values);
					}
					
					values[rsn - 1] = metricEntry.getValue();
				}
			}
		}
	}

	/**
	 * Extracts a single table row containing the metric values history for a given history
	 * @param className The name of the class that the row will present
	 * @param historyMap The map of class names to their history of metric values
	 * @param releaseCount The number of releases for the system
	 * @param separator The separator to use for the columns in the row
	 * @return A Row object containing the metric values history for the given class
	 */
	private Row extractMetricValueRow(String className, Map<String, Map<EClassMetricName, Integer[]>> historyMap, int releaseCount, String separator, EClassMetricName metric)
	{
		//Create an array of columns for the row, which will contain
		//[0]: Class name
		//[1]: Metric name
		//[2] - [2 + n]: Metric value for each version of the class
		Column[] columns = new Column[2 + releaseCount];
		
		//Class name
		columns[0] = new StringColumn(className);
		//Metric name
		columns[1] = new StringColumn(MetricNameMappingUtil.getMetricAcronym(metric));
		
		//Get the metric history for the class
		Integer[] values = historyMap.get(className).get(metric);
		
		//For each version of the class
		for (int i = 0; i < values.length; i++)
		{
			//If the class existed in the release, display it's metric value,
			//else display an empty string
			if (values[i] != null)
				columns[i + 2] = new IntegerColumn(values[i]);
			else
				columns[i + 2] = new StringColumn();
		}
		
		Row row = new Row(columns, separator);
		return row;
	}

	/**
	 * Gets a map of class names to their respective metric value for the given metric for a specified version
	 * @param version The version containing the classes to get metric counts for
	 * @param metric The metric to get the values for
	 * @return A map of class names to values for the specified metric
	 */
	private void getClassNameMetricMap(Map<String, Map<EClassMetricName, Integer>> versionMetricMap, Version version)
	{
		Set<Entry<String, ClassMetricData>> classEntries = version.getClasses().entrySet();
		
		//For each class, map the class to the value of the metric that has been specified
		for (Entry<String, ClassMetricData> classEntry : classEntries)
		{
			Map<EClassMetricName, Integer> classMetricMap = new HashMap<EClassMetricName, Integer>();
			ClassMetricData classMetricData = classEntry.getValue();
			
			for(EClassMetricName metric : metrics)
				classMetricMap.put(metric, classMetricData.getMetricValue(metric));
			
			versionMetricMap.put(classEntry.getKey(), classMetricMap);
		}
	}
}
