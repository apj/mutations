package report.builder.vocab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import report.EReportConfigOption;
import report.builder.TabularReportBuilder;
import report.table.Column;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;

public class TermFrequencyReportBuilder extends TabularReportBuilder
{	
	@Override
	protected String getHeader()
	{
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		int maxValue = Integer.parseInt(config.getEntry(EReportConfigOption.MAX_VALUE));
		
		StringBuilder header = new StringBuilder();
		header.append("Name").append(separator);
		header.append("ID").append(separator);
		header.append("RSN").append(separator);
		header.append("Age").append(separator);
		
		for(int i = 0; i < maxValue; i++)
			header.append(i).append(separator);
		
		header.append(maxValue).append("+");
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		int maxValue = Integer.parseInt(config.getEntry(EReportConfigOption.MAX_VALUE));
		
		Map<Integer, Integer> daysSinceBirth = new TreeMap<Integer, Integer>();
		Map<Integer, Map<String, Integer>> versionTermCountMap = VocabularyReportUtil.getVocabularyUsageHistory(history, daysSinceBirth);
				
		Map<Integer, int[]> versionTermFreqDistMap = new TreeMap<Integer, int[]>();
		VocabularyReportUtil.extractTermFreqDists(versionTermCountMap, versionTermFreqDistMap, maxValue);
		
		List<Row> rows = new ArrayList<Row>();
		
		String name = history.getShortName();
		
		for(Entry<Integer, int[]> versionTermFreqDistEntry : versionTermFreqDistMap.entrySet())
		{
			int rsn = versionTermFreqDistEntry.getKey();
			String id = history.getVersions().get(rsn);
			int age = daysSinceBirth.get(rsn);
			
			rows.add(extractTermFreqDistRow(name, id, age, rsn, versionTermFreqDistEntry.getValue()));
		}
		
		return rows;
	}

	private Row extractTermFreqDistRow(String name, String id, int rsn, int age, int[] freqDist)
	{
		Column[] columns = new Column[4 + freqDist.length];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(id);
		columns[2] = new IntegerColumn(rsn);
		columns[3] = new IntegerColumn(age);
		
		for(int i = 0; i < freqDist.length; i++)
			columns[4 + i] = new IntegerColumn(freqDist[i]);
		
		return new Row(columns);
	}
}
