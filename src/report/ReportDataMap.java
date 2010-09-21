package report;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper around a Map that sorts data for unstructured (i.e. non-tabular) reports
 * 
 * @author Allan Jones
 */
public class ReportDataMap implements IReportContent
{
	//Map of report data keys to their associated objects
	private Map<String, Object> reportDataMap;
	
	public ReportDataMap()
	{
		this(10);
	}
	
	public ReportDataMap(int size)
	{
		reportDataMap = new HashMap<String, Object>(size);
	}
	
	public void add(String key, Object value)
	{
		if(key == null)
			throw new NullPointerException("Could not store report data component, specified key was null");
		
		reportDataMap.put(key, value);
	}
	
	public Object get(String key)
	{
		if(key == null)
			throw new NullPointerException("Could not fetch report data component, specified key was null");
		
		return reportDataMap.get(key);
	}
}