package model.vocab;

//TODO: Add more statuses?
/**
 * Enum type representing the extent to which a level of abstraction (history, version or class) has been processed
 */
public enum EProcessingStatus
{
	UNPROCESSED,
	BASE_EXTRACTED,
	DEPENDENCIES_EXTRACTED,
	POST_PROCESSED,
	FINALIZED
}