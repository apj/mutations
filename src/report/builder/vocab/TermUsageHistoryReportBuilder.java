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

public class TermUsageHistoryReportBuilder extends TabularReportBuilder
{	
	@Override
	protected String getHeader()
	{
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		StringBuilder header = new StringBuilder();
		
		int releaseCount = history.getReleaseCount();
		
		header.append("Name").append(separator);
		header.append("Token");
		
		for(int i = 1; i <= releaseCount; i++)
			header.append(separator).append(i);
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		Map<Integer, Map<String, Integer>> versionTermCountMap = VocabularyReportUtil.getVocabularyUsageHistory(history);
		
		Map<String, int[]> termHistoryMap = new TreeMap<String, int[]>(); 
		extractTermHistoryMap(termHistoryMap, versionTermCountMap);
		
		String name = history.getShortName();
		
		List<Row> rows = new ArrayList<Row>();
		
		for(Entry<String, int[]> termHistoryEntry : termHistoryMap.entrySet())
			rows.add(extractTermHistoryRow(name, termHistoryEntry.getKey(), termHistoryEntry.getValue()));
		
		return rows;
	}

	private Row extractTermHistoryRow(String name, String term, int[] counts)
	{
		Column[] columns = new Column[2 + counts.length];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(term);
		
		for(int i = 0; i < counts.length; i++)
			columns[2 + i] = counts[i] != -1 ? new IntegerColumn(counts[i]) : new StringColumn();
			
		return new Row(columns);
	}

	private void extractTermHistoryMap(Map<String, int[]> termHistoryMap, Map<Integer, Map<String, Integer>> versionTermCountMap)
	{
		for(Entry<Integer, Map<String, Integer>> versionTermCountEntry : versionTermCountMap.entrySet())
		{
			int rsn = versionTermCountEntry.getKey();
			Map<String, Integer> termCountMap = versionTermCountEntry.getValue();
			
			for(Entry<String, Integer> termCountEntry : termCountMap.entrySet())
			{
				String term = termCountEntry.getKey();
				
				int[] termHistory = termHistoryMap.get(term);
				
				if(termHistory == null)
				{
					termHistory = new int[history.getReleaseCount()];
					
					for(int i = 0; i < termHistory.length; i++)
						termHistory[i] = -1;
					
					termHistoryMap.put(term, termHistory);
				}
				
				termHistory[rsn - 1] = termCountEntry.getValue();
			}
		}
	}
}