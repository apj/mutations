package report.builder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import model.Version;
import model.vocab.EClassMetricName;
import report.EReportConfigOption;
import report.table.Column;
import report.table.DecimalColumn;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.VersionFactory;

/**
 * ReportBuilder for a report that presents summative information about a system,
 * including the following:
 * 
 * - Name
 * - Type (application, library or framework)
 * - No. of releases
 * - Age (measured in weeks since the first release)
 * - Initial size (in terms of the no. of classes in the first version)
 * - Final size (in terms of the no. of classes in the latest version)
 * - Change (based upon min/max relative frequency histogram areas)
 * - Growth type (based upon growth of classes -- linear, sub-linear or super-linear)
 * - Description (purpose of the system)
 * - Last release (the ID of the last release, e.g. 1.7.1)
 * - Last modified (the month and year that the system was last modified, e.g. Jun-2008)
 * 
 * @author Allan Jones
 */
public class SummaryReportBuilder extends TabularReportBuilder
{
	private Map<Integer, Map<EClassMetricName, double[]>> versionFrequencyDistributionMap = new TreeMap<Integer, Map<EClassMetricName,double[]>>();
	private List<Integer> classCounts = new ArrayList<Integer>();
	
	@Override
	protected String getHeader()
	{
		if(!config.getEntry(EReportConfigOption.SHOW_HEADER).equals("y"))
			return null;
			
		String separator = config.getSeparator();

		StringBuilder header = new StringBuilder();

		header.append("Name" + separator);
		header.append("Type" + separator);
		header.append("Releases" + separator);
		header.append("Age(Weeks)" + separator);
		header.append("Initial Size" + separator);
		header.append("Final Size" + separator);
		header.append("Change" + separator);
		header.append("Growth Type" + separator);
		header.append("Description" + separator);
		header.append("Last Release" + separator);
		header.append("Last Updated");
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		extractHistoryInformation();
		
		List<Row> rows = new ArrayList<Row>(1);
		rows.add(extractSummaryValuesRow());
		
		return rows;
	}
	
	/**
	 * Iterates through the versions that form the systems history, extracting necessary information for post-processing
	 */
	private void extractHistoryInformation()
	{
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		//Retrieve the last version
		Version lastVersion = versionFactory.getVersion(history.getShortName(), history.getReleaseCount());
	
		//Get the metrics to be used in determining the amount of change
		EClassMetricName[] metrics = ReportBuilderUtil.getMMLongMetrics();
		
		//Initialise the map of metrics to their associated frequency table maximum values
		Map<EClassMetricName, Integer> metricLimits = new HashMap<EClassMetricName, Integer>(metrics.length);
		
		//Get the max value for each of the metrics using the value under the percentile from the last
		//version
		//TODO: Make percentile configurable
		for(EClassMetricName metric : metrics)
			metricLimits.put(metric, ReportBuilderUtil.findValueUnderPercentile(lastVersion, 0.9, metric));
		
		//For each version
		for(Integer rsn : history.getVersions().keySet())
		{
			//Retrieve the version
			Version version = versionFactory.getVersion(history.getShortName(), rsn);
			
			//Extract the frequency distributions for the version
			extractVersionFrequencyDistributions(version, metrics, metricLimits);
			//Store the versions class count
			classCounts.add(version.getClassCount());
		}
	}

	/**
	 * Extracts the relative frequency table for a given array of metrics for a given version
	 * @param version The version to extract the frequency tables from 
	 * @param metrics The metrics to extract frequency tables for
	 * @param metricLimits The limits associated with the metrics
	 */
	private void extractVersionFrequencyDistributions(Version version, EClassMetricName[] metrics, Map<EClassMetricName, Integer> metricLimits)
	{
		//Create the map to house the versions tables
		Map<EClassMetricName, double[]> versionFrequencyDistributions = new HashMap<EClassMetricName, double[]>(metrics.length);
		
		//For each metric, extract the relative frequency table for the version
		for(EClassMetricName metric : metrics)
			versionFrequencyDistributions.put(metric, ReportBuilderUtil.createRelFreqTable(version, metric, metricLimits.get(metric)));
		
		//Map the extracted tables to the versions RSN
		versionFrequencyDistributionMap.put(version.getRSN(), versionFrequencyDistributions);
	}
	
	/**
	 * Builds a Row object containing the summary values that make up the report
	 * @return
	 */
	private Row extractSummaryValuesRow()
	{
		VersionFactory versionFactory = VersionFactory.getInstance();
		String shortName = history.getShortName();
		
		int releaseCount = history.getReleaseCount();

		//Get the first and last versions
		Version firstVersion = versionFactory.getVersion(shortName, 1);
		Version lastVersion = versionFactory.getVersion(shortName, releaseCount);
		
		Column[] columns = new Column[11];
		
		//Name
		columns[0] = new StringColumn(shortName);
		//Type
		columns[1] = new StringColumn(history.getSystemType());
		//Releases
		columns[2] = new IntegerColumn(releaseCount);
		//Age (Weeks)
		columns[3] = new IntegerColumn(ReportBuilderUtil.getAgeInWeeks(firstVersion.getLastModifiedTime(),
																	lastVersion.getLastModifiedTime()));
		//Initial Size
		columns[4] = new IntegerColumn(classCounts.get(0));
		//Final Size
		columns[5] = new IntegerColumn(classCounts.get(classCounts.size() - 1));
		//Change
		columns[6] = new DecimalColumn(ReportBuilderUtil.computeMaxMinAreaChange(versionFrequencyDistributionMap));
		//Growth Type
		columns[7] = new StringColumn(ReportBuilderUtil.getGrowthRateType(classCounts));
		//Description
		columns[8] = new StringColumn(history.getAppDescription());
		//Last Release
		columns[9] = new StringColumn(lastVersion.getId());
		//Last Updated
		//TODO: Possibly introduce DateColumn
		columns[10] = new StringColumn(new SimpleDateFormat("MMM-yyyy").format(lastVersion.getLastModifiedDate()));
		
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		
		return new Row(columns, separator);
	}
}