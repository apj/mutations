package report.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
public class MetricHistoryReportBuilder extends TabularReportBuilder
{
	@Override
	protected String getHeader()
	{
		return "Name" + config.getEntry(EReportConfigOption.SEPARATOR) + "Metric";
	}
	
	@Override
	protected List<Row> getRows()
	{
		//The metric whose history of values is to be displayed
		EClassMetricName metric = MetricNameMappingUtil.classMetricFromCamelString(config.getEntry(EReportConfigOption.METRIC));
		
		//Extract the metric history for classes
		Map<String, Integer[]> historyMap = extractClassMetricHistoryMap(metric);
		
		List<Row> reportRows = new ArrayList<Row>();
		
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		
		if (historyMap == null)
		{
			//TODO: Log error
			System.err.println("History map was null");
			return reportRows;
		}
		
		int releases = history.getReleaseCount();
		
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
	public Map<String, Integer[]> extractClassMetricHistoryMap(EClassMetricName metric)
	{
		// TODO: This is not smart enough, as it will not work well when we have
		// classes that are removed for a version, then come back again.
		Map<String, Integer[]> historyMap = new HashMap<String, Integer[]>();
		
		String shortName = history.getShortName();
		int releaseCount = history.getReleaseCount();
		
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		for(Integer rsn : history.getVersions().keySet())
		{
			Version version = versionFactory.getVersion(shortName, rsn);
			Map<String, Integer> classNameMetricMap = getClassNameMetricMap(version, metric);
			
			for (Entry<String, Integer> classMetricEntry : classNameMetricMap.entrySet())
			{
				String className = classMetricEntry.getKey();
				
				// first time, create the array object
				if (!historyMap.containsKey(className))
					historyMap.put(className, new Integer[releaseCount]);
				
				// Store the metric value against the version in array
				historyMap.get(className)[rsn - 1] = classMetricEntry.getValue();
			}
		}
		return historyMap;
	}

	/* Tabular-specific code */
	
	/**
	 * Extracts a single table row containing the metric values history for a given history
	 * @param className The name of the class that the row will present
	 * @param historyMap The map of class names to their history of metric values
	 * @param releaseCount The number of releases for the system
	 * @param separator The separator to use for the columns in the row
	 * @param metric 
	 * @return A Row object containing the metric values history for the given class
	 */
	private Row extractMetricValueRow(String className, Map<String, Integer[]> historyMap, int releaseCount, String separator, EClassMetricName metric)
	{
		//Create an array of columns for the row, which will contain
		//[0]: Class name
		//[1]: Metric name
		//[2] - [2 + n]: Metric value for each version of the class
		Column[] columns = new Column[2 + releaseCount];
		
		//Class name
		columns[0] = new StringColumn(className);
		//Metric name
		columns[1] = new StringColumn(MetricNameMappingUtil.toCamelString(metric));
		
		//Get the metric history for the class
		Integer[] values = historyMap.get(className);
		
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
	private Map<String, Integer> getClassNameMetricMap(Version version, EClassMetricName metric)
	{
		Map<String, Integer> metricMap = new HashMap<String, Integer>();
		
		//For each class, map the class to the value of the metric that has been specified
		for (ClassMetricData classMetricData : version.getClasses().values())
			metricMap.put(classMetricData.getClassName(), classMetricData.getMetricValue(metric));
		
		return metricMap;
	}
}
