package model.vocab;

/**
 * Enum type representing the valid metric types associated with a class
 * 
 * @author Allan Jones
 */
public enum EEvolutionCategory
{
	UNCHANGED(0),
    MODIFIED(1),
    DELETED(2),
	ADDED(3);
	
	private int value;
	
	private EEvolutionCategory(int value)
	{
		this.value = value;
	}
	
	public int getValue()
	{
		return value;
	}
	
	public String toString()
	{
		return name().toLowerCase();
	}
}
