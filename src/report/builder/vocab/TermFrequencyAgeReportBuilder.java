package report.builder.vocab;

import java.util.ArrayList;
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

public class TermFrequencyAgeReportBuilder extends TabularReportBuilder
{

	@Override
	protected String getHeader()
	{
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		return new StringBuilder().append("Name").append(separator)
								  .append("Last RSN").append(separator)
								  .append("Term").append(separator)
								  .append("Freq").append(separator)
								  .append("Age").toString();
	}

	@Override
	protected List<Row> getRows()
	{
		
		Map<Integer, Integer> daysSinceBirth = new TreeMap<Integer, Integer>();
		Map<Integer, Map<String, Integer>> tokenHistory = VocabularyReportUtil.getVocabularyUsageHistory(history, daysSinceBirth);

		Map<Integer, List<String>> versionNewTokensMap = new TreeMap<Integer, List<String>>();
		Map<String, Integer> tokenFirstAppearanceMap = new LinkedHashMap<String, Integer>();
		extractTermFirstAppearances(tokenHistory, versionNewTokensMap, tokenFirstAppearanceMap);

		int latestVersionRSN = daysSinceBirth.size();
		Map<String, Integer> finalVersionTokenMap = tokenHistory.get(latestVersionRSN);
		
		String name = history.getName();
		int currentVersionAge = daysSinceBirth.get(latestVersionRSN);
		
		List<Row> rows = new ArrayList<Row>();
		
		for (Entry<String, Integer> tokenFirstAppearancesEntry : tokenFirstAppearanceMap.entrySet())
		{
			String term = tokenFirstAppearancesEntry.getKey();
			int firstAppearance = tokenFirstAppearancesEntry.getValue();
			int age = currentVersionAge - daysSinceBirth.get(firstAppearance);
			int occurrences = finalVersionTokenMap.containsKey(term) ? finalVersionTokenMap.get(term) : -1;

			if (occurrences > 0)
			{
				Column[] columns = new Column[5];
				columns[0] = new StringColumn(name);
				columns[1] = new IntegerColumn(latestVersionRSN);
				columns[2] = new StringColumn(term);
				columns[3] = new IntegerColumn(occurrences);
				columns[4] = new IntegerColumn(age);
				
				rows.add(new Row(columns));
			}
		}
		
		return rows;
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
}