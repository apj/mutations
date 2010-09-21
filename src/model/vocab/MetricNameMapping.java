package model.vocab;

/**
 * Representing a mapping between a metric name and various different representations
 * of the metric name, including:
 * - Acronym
 * - Camel case string representation
 *  
 * @author Allan Jones
 *
 * @param <E> Enum type that is a kind of metric name enum
 */
public class MetricNameMapping<E extends Enum<E> & IMetricName>
{
	//The metric name enum
	private E metricName;
	//The acronym representation of the metric name
	private String acronym;
	//The camel case variant of the metric name
	private String camelCaseRepresentation;

	public MetricNameMapping(E metricName, String acronym)
	{
		this.metricName = metricName;
		this.acronym = acronym;
		this.camelCaseRepresentation = extractCamelCaseRepresentation(metricName);
	}
	
	/**
	 * Extracts a camel case representation of the given metric name
	 * @param metricName The metric name to derive a camel case string from
	 * @return The camel case string representation of the given metric name
	 */
	//TODO: Extract to String Util
	private String extractCamelCaseRepresentation(E metricName)
	{
		//Lower case the metric name string and split it by underscore into tokens
		String[] tokens = metricName.name().toString().toLowerCase().split("_");
				
		StringBuilder builder = new StringBuilder();
		//Add the first token (lower case start) to the output string
		builder.append(tokens[0]);
		
		//For each subsequent token
		for(int i = 1; i < tokens.length; i++)
		{
			//Replace the first character in each token with it's upper case variant
			char firstChar = tokens[i].charAt(0);
			builder.append(tokens[i].replaceFirst(String.valueOf(firstChar),
												  String.valueOf(Character.toUpperCase(firstChar))));
		}
		
		return builder.toString();
	}
	
	public E getMetricName()
	{
		return metricName;
	}

	public String getAcronym()
	{
		return acronym;
	}

	public String getCamelCaseRepresentation()
	{
		return camelCaseRepresentation;
	}
}