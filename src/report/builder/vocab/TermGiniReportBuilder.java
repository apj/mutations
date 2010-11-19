package report.builder.vocab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import report.EReportConfigOption;
import report.builder.TabularReportBuilder;
import report.table.Column;
import report.table.DecimalColumn;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;
import extraction.VersionFactory;

public class TermGiniReportBuilder extends TabularReportBuilder
{
	private Map<Integer, Integer> daysSinceBirth;
	
	public void setDaysSinceBirth(Map<Integer, Integer> daysSinceBirth)
	{
		this.daysSinceBirth = daysSinceBirth;
	}
	
	private Map<Integer, Map<String, Integer>> versionTokenCountMap;
	
	public void setVersionTokenCountMap(Map<Integer, Map<String, Integer>> versionTokenCountMap)
	{
		this.versionTokenCountMap = versionTokenCountMap;
	}
	
	@Override
	protected String getHeader()
	{	
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		return new StringBuilder().append("Name").append(separator)
								  .append("ID").append(separator)
								  .append("RSN").append(separator)
								  .append("Age").append(separator)
								  .append("Gini").toString();
	}

	@Override
	protected List<Row> getRows()
	{
		if(versionTokenCountMap == null)
			versionTokenCountMap = VocabularyReportUtil.getVocabularyUsageHistory(history);
		
		Map<Integer, Double> versionGiniMap = new TreeMap<Integer, Double>();
		extractTokenGinis(versionTokenCountMap, versionGiniMap);
		
		String name = history.getShortName();
		List<Row> rows = new ArrayList<Row>(history.getReleaseCount());
		
		if(daysSinceBirth == null)
		{
			daysSinceBirth = new TreeMap<Integer, Integer>();
			VersionFactory versionFactory = VersionFactory.getInstance();
			for(Integer rsn : history.getVersions().keySet())
				daysSinceBirth.put(rsn, versionFactory.getVersion(history.getShortName(), rsn).getDaysSinceBirth());
		}
		
		for(Entry<Integer, String> versionEntry : history.getVersions().entrySet())
		{
			int rsn = versionEntry.getKey();
			String id = versionEntry.getValue();
			rows.add(extractGiniRow(name, id, rsn, daysSinceBirth.get(rsn), versionGiniMap.get(rsn)));
		}
		
		return rows;
	}

	private Row extractGiniRow(String name, String id, int rsn, int age, double gini)
	{
		Column[] columns = new Column[5];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(id);
		columns[2] = new IntegerColumn(rsn);
		columns[3] = new IntegerColumn(age);
		columns[4] = new DecimalColumn(gini);
		
		return new Row(columns);
	}
	
	private void extractTokenGinis(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, Double> versionGiniMap)
	{
		for(Integer rsn : history.getVersions().keySet())
		{
			Map<String, Integer> versionTokenCounts = versionTokenCountMap.get(rsn);
			versionGiniMap.put(rsn, VocabularyReportUtil.calculateTokenGini(versionTokenCounts));
		}
	}
}