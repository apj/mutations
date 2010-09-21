package report.builder;

import java.util.ArrayList;
import java.util.List;

import model.Version;
import model.vocab.EClassMetricName;
import model.vocab.MetricNameMappingUtil;
import report.table.Column;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.VersionFactory;

/**
 * Builds a report that contains the cumulative total of a number of growth-related class metrics (i.e. all classes within the version)
 * for each version in the systems history
 * 
 * The metrics displayed include:
 * - Method Count
 * - Public Method Count
 * - Field Count
 * - Instruction Count
 * - Branch Count
 * - In Degree Count
 * - Out Degree Count
 * - Method Call Count
 * - Type Construction Count
 * - Super Class Count
 * 
 * @author Allan Jones
 */
public class RawCountReportBuilder extends TabularReportBuilder
{
	@Override
	protected String getHeader()
	{
		String separator = config.getSeparator();

		StringBuilder builder = new StringBuilder();

		builder.append("Name" + separator);
		builder.append("Type" + separator);
		builder.append("ID" + separator);
		builder.append("RSN" + separator);
		builder.append("Age" + separator);
		builder.append("Classes" + separator);
		builder.append("Packages" + separator);

		EClassMetricName[] metrics = ReportBuilderUtil.getGrowthMetrics();

		for (EClassMetricName metric : metrics)
			builder.append(MetricNameMappingUtil.toCamelString(metric) + separator);

		return builder.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		List<Row> rows = new ArrayList<Row>(history.getReleaseCount());
		
		String separator = config.getSeparator();
		
		String shortName = history.getShortName(); 
		String appType = history.getSystemType();
		
		//Get the growth-related metrics
		EClassMetricName[] metrics = ReportBuilderUtil.getGrowthMetrics();
		
		VersionFactory versionFactory = VersionFactory.getInstance();
		
		//For each version
		for (Integer rsn : history.getVersions().keySet())
        {
			Version version = versionFactory.getVersion(shortName, rsn);
			
			Column[] columns = new Column[7 + metrics.length];
            
			//Short name
            columns[0] = new StringColumn(shortName);
            //App type
            columns[1] = new StringColumn(appType);
            //ID
            columns[2] = new StringColumn(version.getId());
            //RSN
            columns[3] = new IntegerColumn(version.getRSN());
            //Days since birth
            columns[4] = new IntegerColumn(version.getDaysSinceBirth());
            //Class count
            columns[5] = new IntegerColumn(version.getClassCount());
            //Package count
            columns[6] = new IntegerColumn(version.getPackageCount());
            
            //Add a column for each metric
            for (int j = 0; j < metrics.length; j++) 
            	columns[j + 7] = new IntegerColumn(ReportBuilderUtil.getMetricValueSum(version, metrics[j]));

            
            rows.add(new Row(columns, separator));
        }
		
		return rows;
	}
}
