package report.builder.vocab;

import java.util.Comparator;
import java.util.Map.Entry;

public class TermPopularityComparator implements Comparator<Entry<String, Integer>>
{
	@Override
	public int compare(Entry<String, Integer> tokenEntry1, Entry<String, Integer> tokenEntry2)
	{
		if (tokenEntry1.getValue() == tokenEntry2.getValue())
			return 0;
		else
			return tokenEntry1.getValue() > tokenEntry2.getValue() ? -1 : 1;
	}
}