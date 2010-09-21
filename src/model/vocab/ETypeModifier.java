package model.vocab;

/**
 * Enum type representing type modifiers
 * 
 * @author Allan Jones
 */
public enum ETypeModifier
{
	PUBLIC(1),
	PRIVATE(2),
	PROTECTED(3),
	STATIC,
	SYCHNRONIZED,
	FINAL,
	ABSTRACT,
	
	UNKNOWN;
	
	private int value = -1;
	
	private ETypeModifier()
	{
	}
	
	private ETypeModifier(int value)
	{
		this.value = value;
	}
	
	public int value()
	{
		return value;
	}
	
	public static ETypeModifier fromValue(int value)
	{
		ETypeModifier[] modifiers = values();
		
		for(ETypeModifier modifier : modifiers)
			if(modifier.value == value)
				return modifier;
			
		return UNKNOWN;
	}
}
