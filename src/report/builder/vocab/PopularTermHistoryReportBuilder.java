package report.builder.vocab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

public class PopularTermHistoryReportBuilder extends TabularReportBuilder
{
	@Override
	protected String getHeader()
	{
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		StringBuilder header = new StringBuilder();
		
		int releaseCount = history.getReleaseCount();
		
		header.append("Name").append(separator);
		header.append("Term");
		
		for(int i = 1; i <= releaseCount; i++)
			header.append(separator).append(i);
		
		return header.toString();
	}

	@Override
	protected List<Row> getRows()
	{
		Map<Integer, Integer> daysSinceBirth = new TreeMap<Integer, Integer>();
		Map<Integer, Map<String, Integer>> termHistory = VocabularyReportUtil.getVocabularyUsageHistory(history, daysSinceBirth);

		Map<Integer, List<String>> versionNewTokensMap = new TreeMap<Integer, List<String>>();
		Map<String, Integer> tokenFirstAppearanceMap = new LinkedHashMap<String, Integer>();
		extractTermFirstAppearances(termHistory, versionNewTokensMap, tokenFirstAppearanceMap);

		int latestVersionRSN = daysSinceBirth.size();
		Map<String, Integer> finalVersionTokenMap = termHistory.get(latestVersionRSN);
		
		// Create a list to house the entries for token -> usage count
		List<Entry<String, Integer>> sortedLastVersionTokens = new ArrayList<Entry<String, Integer>>(
				finalVersionTokenMap.size());
		// Add each of the entries present in the last version's token count map
		// to the list
		sortedLastVersionTokens.addAll(finalVersionTokenMap.entrySet());
		// Use a popularity comparator to sort the list by most popularly used
		// tokens
		Collections.sort(sortedLastVersionTokens, new TermPopularityComparator());
	
		// Create a map for the usage of the token's in the last version over time
		Map<String, int[]> termHistoryMap = new LinkedHashMap<String, int[]>(sortedLastVersionTokens.size());
		extractTokenHistoryMap(termHistoryMap, termHistory, sortedLastVersionTokens);
		
		
		List<Row> rows = new ArrayList<Row>();
		String name = history.getShortName();
		
		for(Entry<String, Integer> termEntry : sortedLastVersionTokens)		
		{
			String term = termEntry.getKey();
			rows.add(extractTermHistoryRow(name, termEntry.getKey(), termHistoryMap.get(term)));
		}
		
		return rows;
	}

	private Row extractTermHistoryRow(String name, String term, int[] occurrences)
	{
		Column[] columns = new Column[2 + occurrences.length];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(term);
		for(int i = 0; i < occurrences.length; i++)
			columns[2 + i] = occurrences[i] != -1 ? new IntegerColumn(occurrences[i]) : new StringColumn();
			
		return new Row(columns);
	}
	
	private static void extractTermFirstAppearances(Map<Integer, Map<String, Integer>> versionTokenCountMap, Map<Integer, List<String>> versionNewTokensMap, Map<String, Integer> tokenFirstAppearanceMap)
	{
		for (Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			List<String> versionNewTokens = new ArrayList<String>();

			for (String token : versionTokenCountEntry.getValue().keySet())
			{
				if (!tokenFirstAppearanceMap.containsKey(token))
				{
					tokenFirstAppearanceMap.put(token, versionTokenCountEntry.getKey());
					versionNewTokens.add(token);
				}
			}

			versionNewTokensMap.put(versionTokenCountEntry.getKey(), versionNewTokens);
		}
	}
	
	private static void extractTokenHistoryMap(Map<String, int[]> tokenHistoryMap, Map<Integer, Map<String, Integer>> versionTokenCountMap, List<Entry<String, Integer>> lastVersionTokens)
	{
		int versionCount = versionTokenCountMap.size();

		// For each of the tokens in the last version, allocate and initialise
		// the usage array
		for (Entry<String, Integer> tokenEntry : lastVersionTokens)
		{
			String token = tokenEntry.getKey();
			int[] tokenHistory = new int[versionCount];

			for (int i = 0; i < versionCount; i++)
				tokenHistory[i] = -1;

			tokenHistoryMap.put(token, tokenHistory);
		}

		// For each version -> token count entry
		for (Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			int rsn = versionTokenCountEntry.getKey();

			Map<String, Integer> tokenCountMap = versionTokenCountEntry.getValue();

			// For each token present in the last version tokens list
			for (Entry<String, Integer> tokenEntry : lastVersionTokens)
			{
				String token = tokenEntry.getKey();

				// If the token was present in the current version,
				// set it's usage count value in the array
				if (tokenCountMap.containsKey(token)) tokenHistoryMap.get(token)[rsn - 1] = tokenCountMap.get(token);
			}
		}
	}
}