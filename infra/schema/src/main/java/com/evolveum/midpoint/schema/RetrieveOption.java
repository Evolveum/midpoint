package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RetrieveOptionType;

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

    public static RetrieveOption fromRetrieveOptionType(RetrieveOptionType retrieveOptionType) {
        if (retrieveOptionType == null) {
            return DEFAULT;
        }
        switch(retrieveOptionType) {
            case DEFAULT: return DEFAULT;
            case INCLUDE: return INCLUDE;
            case EXCLUDE: return EXCLUDE;
            default: throw new IllegalStateException("Unsupported RetrieveOptionType: " + retrieveOptionType);
        }
    }
    
    public static RetrieveOptionType toRetrieveOptionType(RetrieveOption retrieveOption) {
        if (retrieveOption == null) {
            return RetrieveOptionType.DEFAULT;
        }
        switch(retrieveOption) {
            case DEFAULT: return RetrieveOptionType.DEFAULT;
            case INCLUDE: return RetrieveOptionType.INCLUDE;
            case EXCLUDE: return RetrieveOptionType.EXCLUDE;
            default: throw new IllegalStateException("Unsupported RetrieveOption: " + retrieveOption);
        }
    }

}
