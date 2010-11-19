package report.builder.vocab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import report.builder.TabularReportBuilder;
import report.table.Column;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;

public class VocabularyModificationReportBuilder extends TabularReportBuilder
{
	private Map<Integer, Integer> daysSinceBirth;
	
	@Override
	protected String getHeader()
	{
		String separator = config.getSeparator();
		
		StringBuilder header = new StringBuilder();
		
		header.append("Name").append(separator);
		header.append("ID").append(separator);
		header.append("RSN").append(separator);
		header.append("Age").append(separator);
		header.append("Total").append(separator);
		header.append("Retained").append(separator);
		header.append("New").append(separator);
		header.append("Deleted");
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		Map<Integer, Map<String, Map<String, Integer>>> versionMethodTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Map<String, Integer>>> versionFieldTokenCountMap = new TreeMap<Integer, Map<String, Map<String, Integer>>>();
		Map<Integer, Map<String, Integer>> versionClassNameTokenMap = new TreeMap<Integer, Map<String, Integer>>();
		VocabularyReportUtil.extractVocabulary(history, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap);
		
		Map<Integer, Map<String, Integer>> versionTokenCountMap = new TreeMap<Integer, Map<String,Integer>>();
		VocabularyReportUtil.calculateTotalTermCounts(versionTokenCountMap, versionMethodTokenCountMap, versionFieldTokenCountMap, versionClassNameTokenMap);
		
		Map<Integer, Map<String, Map<String, Integer>>> versionClassTokenMap = new TreeMap<Integer, Map<String,Map<String,Integer>>>();
		VocabularyReportUtil.extractVersionClassTokenMap(versionClassTokenMap, versionMethodTokenCountMap, versionFieldTokenCountMap);
		
		Map<Integer, List<String>> versionNewTokensMap = new TreeMap<Integer, List<String>>();
		Map<String, Integer> tokenFirstAppearanceMap = new LinkedHashMap<String, Integer>();
		extractTokenFirstAppearances(versionTokenCountMap, versionNewTokensMap, tokenFirstAppearanceMap);
		
		Map<Integer, List<String>> versionDeletedTokensMap = new TreeMap<Integer, List<String>>();
		Map<String, Integer> tokenDeletionMap = new LinkedHashMap<String, Integer>();
		extractDeletedTokens(versionTokenCountMap, versionDeletedTokensMap, tokenDeletionMap);
		
		String name = history.getName();
		
		List<Row> rows = new ArrayList<Row>(history.getVersions().size());
		
		for(Entry<Integer, String> versionEntry : history.getVersions().entrySet())
		{
			int rsn = versionEntry.getKey();
			String id = versionEntry.getValue();

			int totalCount = versionTokenCountMap.get(rsn).size();
			int newCount = versionNewTokensMap.containsKey(rsn) ? versionNewTokensMap.get(rsn).size() : -1;
			int deletedCount = versionDeletedTokensMap.containsKey(rsn) ? versionDeletedTokensMap.get(rsn).size() : -1;
			int retainedCount = totalCount - (newCount + deletedCount);

			rows.add(extractTokenModificationRow(name, id, rsn, daysSinceBirth.get(rsn), totalCount, newCount, deletedCount, retainedCount));
		}
		
		return rows;
	}

	private void extractTokenFirstAppearances(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, List<String>> versionNewTokensMap, Map<String, Integer> tokenFirstAppearanceMap)
	{
		for(Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			List<String> versionNewTokens = new ArrayList<String>();
			
			for(String token : versionTokenCountEntry.getValue().keySet())
			{
				if(!tokenFirstAppearanceMap.containsKey(token))
				{
					tokenFirstAppearanceMap.put(token, versionTokenCountEntry.getKey());
					versionNewTokens.add(token);
				}
			}
			
			versionNewTokensMap.put(versionTokenCountEntry.getKey(), versionNewTokens);
		}
	}
	
	private void extractDeletedTokens(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, List<String>> versionDeletedTokensMap, Map<String, Integer> tokenDeletionMap)
	{
		for(Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			int rsn = versionTokenCountEntry.getKey();
			
			//Skip last release since we don't know about deleted tokens
			if(rsn == versionTokenCountMap.size()) continue;
			
			List<String> deletedTokens = new ArrayList<String>();
			
			for(String token : versionTokenCountEntry.getValue().keySet())
			{
				//If the token doesn't not exist in the next version, it means it has been deleted
				if(!versionTokenCountMap.get(rsn + 1).containsKey(token))
				{
					tokenDeletionMap.put(token, rsn + 1);
					deletedTokens.add(token);
				}
			}
			
			versionDeletedTokensMap.put(rsn + 1, deletedTokens);
		}
	}
	
	private Row extractTokenModificationRow(String name, String id, int rsn, int age, int totalCount,
			int newCount, int deletedCount, int retainedCount)
	{
		Column[] columns = new Column[8];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(id);
		columns[2] = new IntegerColumn(rsn);
		columns[3] = new IntegerColumn(age);
		columns[4] = new IntegerColumn(totalCount);
		columns[5] = new IntegerColumn(retainedCount);
		columns[6] = new IntegerColumn(newCount);
		columns[7] = new IntegerColumn(deletedCount);
		
		return new Row(columns);
	}
}
