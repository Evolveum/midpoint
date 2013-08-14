package com.evolveum.midpoint.schema;

public enum RetrieveOption {
	
	/**
	 * Return the item "as ususal". If the item would be returned by default then return it.
	 * If the item would not be returned by default then it may not be returned.
	 */
	DEFAULT,
	
	/**
	 * Include the item in the result. The item will be returned (even if it would not be returned by default).
	 */
	INCLUDE,
	
	/**
	 * Exclude the item from the result.
	 */
	EXCLUDE;

}
