package report.table;

/**
 * Column type that stores string values
 * @author Allan Jones
 */
public class StringColumn extends Column
{
	private String value;
	
	public StringColumn()
	{
		this.value = "";
	}
	
	public StringColumn(String value)
	{
		this.value = value;
	}
	
	public void setValue(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public String toString()
	{
		return value;
	}
}
