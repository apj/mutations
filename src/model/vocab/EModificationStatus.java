package model.vocab;

/**
 * Enum type representing a classes modification status (never modified, modified after birth or new born) 
 * @author Allan Jones
 */
public enum EModificationStatus
{
	NEVER_MODIFIED(0),
    MODIFIED_AFTER_BIRTH(1),
    NEW_BORN(2);
	
	private int value;
	
	private EModificationStatus(int value)
	{
		this.value = value;
	}
	
	public int getValue()
	{
		return value;
	}
}
