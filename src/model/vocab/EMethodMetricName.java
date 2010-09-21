package model.vocab;

/**
 * Enum type representing method-level metrics
 * 
 * @author Allan Jones
 */
public enum EMethodMetricName implements IMetricName
{
	IS_FINAL,
	IS_STATIC,
	IS_ABSTRACT,
	IS_SYNCHRONIZED,
	
	BRANCH_COUNT,
	METHOD_CALL_COUNT,
	THROW_COUNT,
	TRY_CATCH_BLOCK_COUNT,
	
    TYPE_INSN_COUNT,
    ZERO_OP_INSN_COUNT,
    CONSTANT_LOAD_COUNT,
    INCREMENT_OP_COUNT,
    I_LOAD_COUNT,
    I_STORE_COUNT,
    REF_LOAD_OP_COUNT,
    REF_STORE_OP_COUNT,
    
    TYPE_CONSTRUCTION_COUNT,
    INSTANCE_OF_COUNT,
    CHECK_CAST_COUNT,
    NEW_COUNT,
    NEW_ARRAY_COUNT,
    
    INTERNAL_METHOD_CALL_COUNT,
    EXTERNAL_METHOD_CALL_COUNT,
    
    PARAM_COUNT,
    EXCEPTION_COUNT,
    IS_CONSTRUCTOR,
    
    STORE_FIELD_COUNT,
    LOAD_FIELD_COUNT,
    LOCAL_VAR_COUNT,
    
    SCOPE;
}