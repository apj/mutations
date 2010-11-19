package report.builder.vocab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import report.EReportConfigOption;
import report.builder.TabularReportBuilder;
import report.table.Column;
import report.table.IntegerColumn;
import report.table.Row;
import report.table.StringColumn;

public class PopularTermReportBuilder extends TabularReportBuilder
{
	@Override
	protected String getHeader()
	{
		String separator = config.getEntry(EReportConfigOption.SEPARATOR);
		return new StringBuilder().append("Name").append(separator)
								  .append("RSN").append(separator)
								  .append("ID").append(separator)
								  .append("Term").append(separator)
								  .append("Freq").append(separator).toString();
	}

	@Override
	protected List<Row> getRows()
	{
		Map<Integer, Map<String, Integer>> tokenHistory = VocabularyReportUtil.getVocabularyUsageHistory(history);

		int latestVersionRSN = history.getReleaseCount();
		String latestVersionID = history.getVersionID(latestVersionRSN);
		Map<String, Integer> finalVersionTokenMap = tokenHistory.get(latestVersionRSN);
		
		// Create a list to house the entries for token -> usage count
		List<Entry<String, Integer>> sortedLastVersionTokens = new ArrayList<Entry<String, Integer>>(finalVersionTokenMap.size());
		// Add each of the entries present in the last version's token count map
		// to the list
		sortedLastVersionTokens.addAll(finalVersionTokenMap.entrySet());
		// Use a popularity comparator to sort the list by most popularly used
		// tokens
		Collections.sort(sortedLastVersionTokens, new TermPopularityComparator());
		
		List<Row> rows = new ArrayList<Row>();
		
		String name = history.getShortName();
		
		for (Entry<String, Integer> sortedTokenEntry : sortedLastVersionTokens)
		{
			String term = sortedTokenEntry.getKey();
			int occurrences = sortedTokenEntry.getValue();

			rows.add(extractPopularTermRow(name, latestVersionID, latestVersionRSN, term, occurrences));
		}
		
		return rows;
	}
	
	private Row extractPopularTermRow(String name, String id, int rsn, String term, int occurrences)
	{
		Column[] columns = new Column[5];
		
		columns[0] = new StringColumn(name);
		columns[1] = new StringColumn(id);
		columns[2] = new IntegerColumn(rsn);
		columns[3] = new StringColumn(term);
		columns[4] = new IntegerColumn(occurrences);
		
		return new Row(columns);
	}
}