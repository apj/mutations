package model;

import java.util.Map;
import java.util.Set;

import model.vocab.EMethodMetricName;

public class MethodMetricData
{
	private Map<EMethodMetricName, Integer> metricMap;
	private Map<String, Integer> externalCalls;
	private Set<String> dependencies;
	private String methodName;
	private String shortMethodName;
	
	public String getShortMethodName()
	{
		return shortMethodName;
	}
	
	public MethodMetricData(Map<EMethodMetricName, Integer> metricMap,  Map<String, Integer> externalCalls, Set<String> dependencies, String methodName, String shortMethodName)
	{
		this.metricMap = metricMap;
		this.externalCalls = externalCalls;
		this.dependencies = dependencies;
		this.methodName = methodName;
		this.shortMethodName = shortMethodName;
	}
	
	public Map<EMethodMetricName, Integer> getMetricMap()
	{
		return metricMap;
	}
	
	public Map<String, Integer> getExternalCalls()
	{
		return externalCalls;
	}

	public String getMethodName()
	{
		return methodName;
	}

	public int getMetricValue(EMethodMetricName metric)
	{
		return metricMap.get(metric);
	}

	public Set<String> getDependencies()
	{
		return dependencies;
	}
}
