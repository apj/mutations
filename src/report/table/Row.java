package report.table;

/**
 * Represents a basic row type that can store an arbitrary number of columns
 * @author Allan Jones
 */
public class Row
{
	private boolean useColumnSeparator = true;
	
	private String separator = ",";
	
	private Column[] columns;
	
	public Row(Column[] columns)
	{
		this.columns = columns;
	}
	
	public Row(Column[] columns, String separator)
	{
		this.columns = columns;
		this.separator = separator;
	}

	public Column[] getColumns()
	{
		return columns;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		for(Column column : columns)
		{
			builder.append(column);
			if(useColumnSeparator) builder.append(separator);
		}
		
		if(useColumnSeparator) builder.delete(builder.lastIndexOf(separator), builder.length()); //Remove trailing separation
			
		return builder.toString();
	}
}