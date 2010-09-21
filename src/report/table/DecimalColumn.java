package report.table;

import java.text.DecimalFormat;

/**
 * Column type that stores decimal values with an associated output formatting
 * @author Allan Jones
 */
public class DecimalColumn extends Column
{
	private double value;
	private String formatting = "#.####";
	
	//TODO: Decide how to handle columns with no value - cannot print 0
	public DecimalColumn()
	{
		this.value = 0;
		//TODO: Set formatting
	}
	
	public DecimalColumn(double value)
	{
		this.value = value;
		//TODO: Set formatting
	}
	
	public DecimalColumn(double value, String formatting)
	{
		this.value = value;
		this.formatting = formatting;
	}
	
	public void setValue(double value)
	{
		this.value = value;
	}

	public double getValue()
	{
		return value;
	}

	public String toString()
	{
		//TODO: Add formatting application
		return new DecimalFormat(formatting).format(value);
	}
}