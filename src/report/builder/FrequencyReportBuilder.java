package report.builder;

import java.util.ArrayList;
import java.util.List;

import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.MetricNameMappingUtil;
import report.EReportConfigOption;
import report.table.Column;
import report.table.DecimalColumn;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.VersionFactory;

/**
 * Builds a report that displays the frequency distribution of a given metric
 * for each of the versions in the systems history
 * 
 * This report takes the following arguments:
 * - Metric: The metric to show the frequency distribution for
 * - Max Value: The max frequency value
 * - Relative: Whether the frequency distribution should be relative to the total number of classes
 * 
 * @author Allan Jones
 */
public class FrequencyReportBuilder extends TabularReportBuilder
{	
	@Override
	protected String getHeader()
	{
		String separator = config.getSeparator();

		StringBuilder header = new StringBuilder();

		header.append("Name").append(separator);
		header.append("RSN").append(separator);
		header.append("Age").append(separator);
		header.append("DaysSinceLast").append(separator);
		header.append("ID").append(separator);
		header.append("Classes").append(separator);
		
		EClassMetricName metric = MetricNameMappingUtil.classMetricFromCamelString(config.getEntry(EReportConfigOption.METRIC));
		
		//If the metric is Evolution Status of Next Version Status, it means we want
		//to display Unchanged/Modifed/Deleted/New distribution
		if(metric == EClassMetricName.EVOLUTION_STATUS || metric == EClassMetricName.NEXT_VERSION_STATUS)
		{
			header.append("Unchanged").append(separator);
			header.append("Modified").append(separator);
			header.append("Deleted").append(separator);
			header.append("New");
		}
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		List<Row> rows = new ArrayList<Row>(history.getReleaseCount());
		
		String system = history.getShortName();		
		//Get the metric to display the distribution for
		EClassMetricName metric = MetricNameMappingUtil.classMetricFromCamelString(config.getEntry(EReportConfigOption.METRIC));
		//Get the max frequency value
		int maxValue = Integer.parseInt(config.getEntry(EReportConfigOption.MAX_VALUE));
		//Get the flag indicating whether the frequency distribution should be relative
		boolean relative = config.getEntry(EReportConfigOption.RELATIVE).equalsIgnoreCase("Y");
		
		//For each version, get the frequency distribution row
		for(Integer rsn : history.getVersions().keySet())
			rows.add(getVersionRow(system, versionFactory.getVersion(system, rsn.intValue()), metric, maxValue, relative));
		
		return rows;
	}

	/**
	 * Builds the frequency distribution row for a given version and metric
	 * 
	 * @param system The system whose versions are being display
	 * @param version The version whose frequency distribution is being displayed
	 * @param metric The metric whose frequency distribution is being displayed
	 * @param maxValue The max value for frequency
	 * @param relative Whether the frequency distribution should be relative to the total number of classes
	 * @return The frequency distribution row that ws built
	 */
	private Row getVersionRow(String system, Version version, EClassMetricName metric, int maxValue, boolean relative)
	{		
		//Get the absolute frequency value for the given version/metric combination
		int[] freqTable = ReportBuilderUtil.createFreqTable(version, metric, maxValue);
		
		Column[] columns = new Column[6 + freqTable.length];
		
		columns[0] = new StringColumn(system);
		columns[1] = new IntegerColumn(version.getRSN());
		columns[2] = new IntegerColumn(version.getDaysSinceBirth());
		columns[3] = new IntegerColumn(version.getDaysSinceLastVersion());
		columns[4] = new StringColumn(version.getId());
		columns[5] = new IntegerColumn(version.getClassCount());
		
		//If the relative flag was specified, we want to show values relative to the total class count,
		//else we just want to show the absolute values 
		if(relative)
		{
			//Get the relative frequency table using the absolute table
			double[] relFreqTable = ReportBuilderUtil.createRelFreqTable(freqTable);
			
			//Add a new column for each of the values in the relative frequency table
			for(int i = 0; i < relFreqTable.length; i++)
				columns[6 + i] = new DecimalColumn(relFreqTable[i]);
		}
		else
		{
			//Add a new column for each of the values in the frequency table
			for(int i = 0; i < freqTable.length; i++)
				columns[6 + i] = new DecimalColumn(freqTable[i]);
		}
		
		return new Row(columns);
	}
}
