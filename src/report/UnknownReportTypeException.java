package report;

public class UnknownReportTypeException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4946534575416830643L;

	public UnknownReportTypeException()
	{
	}
	
	public UnknownReportTypeException(String message)
	{
		super(message);
	}
}