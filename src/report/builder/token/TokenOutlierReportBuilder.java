package report.builder.token;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;

import report.Report;
import report.ReportDataMap;
import report.builder.ReportBuilder;

public class TokenOutlierReportBuilder extends ReportBuilder
{
	private final double relativeGrowthThreshold = 1.0;
//	private final double giniThreshold = 0.04;
	private final int newTokenCountThreshold = 5;
	
	//Flag:
	//Deleted
	//New
	//Reducing
	//Big increases
	//Changing Gini values
	//Print tokens with highest occurrence rate (top 10)
	
	@Override
	public Report buildReport()
	{
		ReportDataMap reportData = new ReportDataMap();
		
		Map<Integer, Map<String, Integer>> versionTokenCountMap = TokenReportUtil.getTokenHistory(history);
		
		StringBuilder reportString = new StringBuilder();
		
		double previousGini = -1;
		
		for(Entry<Integer, Map<String, Integer>> versionTokenCountEntry : versionTokenCountMap.entrySet())
		{
			int rsn = versionTokenCountEntry.getKey().intValue(); 
			
			reportString.append("===== RSN " + rsn + " =====").append("\r\n");
			
			Map<String, Integer> tokenCountMap = versionTokenCountEntry.getValue();
			int totalTokens = tokenCountMap.size();
			
			double gini = TokenReportUtil.calculateTokenGini(tokenCountMap);
			int[] freqDist = TokenReportUtil.extractTokenFreqDist(tokenCountMap, 10);
			
			reportString.append(getTotalTokensString(totalTokens));
			reportString.append(getGiniString(gini, previousGini));
			reportString.append(getOccurrenceRatesString(freqDist, totalTokens));
			
			if(rsn > 1) reportString.append(getRichNewTokensString(tokenCountMap, versionTokenCountMap.get(rsn - 1)));
			if(rsn < versionTokenCountMap.size()) reportString.append(getNextVersionDeletedTokensString(tokenCountMap, versionTokenCountMap.get(rsn + 1)));
			if(rsn > 1) reportString.append(getRelativeGrowthString(tokenCountMap, versionTokenCountMap.get(rsn - 1)));
			
			previousGini = gini;
			
			reportString.append("\r\n");
		}
		
		System.out.println(reportString);
		
		return getReport(reportData);
	}

	private String getRelativeGrowthString(Map<String, Integer> tokenCountMap, Map<String, Integer> previousTokenMap)
	{
		StringBuilder relativeGrowthString = new StringBuilder();
		DecimalFormat format = new DecimalFormat("#.##");
		
		relativeGrowthString.append("-- Large Growth Tokens (Relative Growth): ").append("\r\n");
		
		for(Entry<String, Integer> tokenCountEntry : tokenCountMap.entrySet())
		{
			String token = tokenCountEntry.getKey();
			
			if(previousTokenMap.containsKey(token))
			{
				int count = tokenCountEntry.getValue();
				int previousCount = previousTokenMap.get(token);
				
				if(previousCount < 5)
					continue;
				
				double relativeGrowth = ((double)(count - previousCount) / (double)previousCount);
			
				if(relativeGrowth > relativeGrowthThreshold)
					relativeGrowthString.append(token).append(": ").append(format.format(relativeGrowth * 100)).append("% (Count: ").append(count).append(", Previous: ").append(previousCount).append(")").append("\r\n");
			}
		}
		
		return relativeGrowthString.toString();
	}

	private String getNextVersionDeletedTokensString(Map<String, Integer> tokenCountMap, Map<String, Integer> previousTokenCountMap)
	{
		int deletedTokens = 0;
		
		StringBuilder deletedTokensString = new StringBuilder();
		
		deletedTokensString.append("-- Deleted Tokens (In Following Version): ").append("\r\n");
		
		for(Entry<String, Integer> tokenCountEntry : tokenCountMap.entrySet())
		{
			String token = tokenCountEntry.getKey();
			
			if(!previousTokenCountMap.containsKey(token))
			{
				deletedTokens++;
				deletedTokensString.append(token).append(": ").append(tokenCountEntry.getValue()).append("\r\n");
			}
		}
		
		if(deletedTokens == 0)
			deletedTokensString.append("(none)").append("\r\n");
		
		deletedTokensString.append("\r\n");
		
		return deletedTokensString.toString();
	}

	private Object getRichNewTokensString(Map<String, Integer> tokenCountMap, Map<String, Integer> previousTokenCountMap)
	{
		int newTokens = 0;
		StringBuilder richNewTokensString = new StringBuilder();
		
		richNewTokensString.append("-- New Tokens: ").append("\r\n");
		
		for(Entry<String, Integer> tokenCountEntry : tokenCountMap.entrySet())
		{
			String token = tokenCountEntry.getKey();
			
			if(!previousTokenCountMap.containsKey(token))
			{
				int tokenCount = tokenCountEntry.getValue();
				
				if(tokenCount > newTokenCountThreshold)
				{
					newTokens++;
					richNewTokensString.append(token).append(": ").append(tokenCount).append("\r\n");
				}
			}
		}
		
		if(newTokens == 0)
			richNewTokensString.append("(none)").append("\r\n");
		
		richNewTokensString.append("\r\n");
		
		return richNewTokensString.toString();
	}

	private String getOccurrenceRatesString(int[] freqDist, int totalTokens)
	{
		StringBuilder occurrenceRatesString = new StringBuilder();
		DecimalFormat format = new DecimalFormat("#.###");
		
		occurrenceRatesString.append("-- Occurrence Rates:").append("\r\n");
		
		for(int i = 0; i < freqDist.length; i++)
			occurrenceRatesString .append(i + ": " + freqDist[i]).append(" (").append(format.format(((double)freqDist[i] / (double)totalTokens) * 100)).append("%)").append("\r\n");
		
		occurrenceRatesString.append("\r\n");
		return occurrenceRatesString.toString();
	}

	private String getGiniString(double gini, double previousGini)
	{
		DecimalFormat format = new DecimalFormat("#.###");
		return new StringBuilder().append("-- Gini:").append("\r\n").append(format.format(gini)).append(previousGini != -1 ? " (Change: " + (format.format(gini - previousGini)) + ")" : "").append("\r\n\r\n").toString();
	}

	private String getTotalTokensString(int totalTokens)
	{
		return new StringBuilder().append("-- Total Tokens:").append("\r\n").append(totalTokens).append("\r\n\r\n").toString();
	}

	private Report getReport(ReportDataMap reportData)
	{
		return new Report(reportData)
		{
			public String toString()
			{
				StringBuilder reportString = new StringBuilder();
				return reportString.toString(); 
			}
		};
	}
}