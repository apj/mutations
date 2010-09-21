package report.table;

/**
 * Column type that stores Integer values
 * 
 * @author Allan Jones
 */
public class IntegerColumn extends Column
{
	private int value;
	
	public IntegerColumn()
	{
		//Default value to indicate no value has been specified
		this.value = Integer.MAX_VALUE;
	}
	
	public IntegerColumn(int value)
	{
		this.value = value;
	}
	
	public void setValue(int value)
	{
		this.value = value;
	}

	public double getValue()
	{
		return value;
	}

	public String toString()
	{
		//In the case that no number has been specified,
		//return nothing instead,
		//else return the string value of the number
		if(value == Integer.MAX_VALUE)
			return "";
		else
			return String.valueOf(value);
	}
}
