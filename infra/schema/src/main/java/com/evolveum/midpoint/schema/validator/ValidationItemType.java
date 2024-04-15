package com.evolveum.midpoint.schema.validator;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;

public enum ValidationItemType {

    /**
     * Protected string that contains {@link ProtectedDataType#getEncryptedDataType()},
     * {@link ProtectedDataType#getHashedDataType()} or {@link ProtectedDataType#getClearValue()}.
     *
     * Data type: {@link com.evolveum.midpoint.prism.PrismPropertyValue<ProtectedDataType>}
     */
    PROTECTED_DATA_NOT_EXTERNAL,

    /**
     * Used when natural key is not present in prism container value for multi-value containers.
     *
     * Data type: {@link com.evolveum.midpoint.prism.PrismContainerValue}
     */
    MISSING_NATURAL_KEY,

    /**
     * Multi-value reference where at least one value doesn't have OID defined (e.g. uses filter).
     *
     * Data type: {@link com.evolveum.midpoint.prism.PrismReferenceValue}
     */
    MULTIVALUE_REF_WITHOUT_OID,

    /**
     * Data type:  {@link com.evolveum.midpoint.prism.Item}
     */
    DEPRECATED_ITEM,

    /**
     * Data type:  {@link com.evolveum.midpoint.prism.Item}
     */
    REMOVED_ITEM,

    /**
     * Data type: {@link com.evolveum.midpoint.prism.Item}
     */
    PLANNED_REMOVAL_ITEM,

    /**
     * Summarized message used by {@link ObjectValidator} and tools that depends on it.
     *
     * Validation items of this type can be split to separate types {@link #DEPRECATED_ITEM}, {@link #REMOVED_ITEM},
     * {@link #PLANNED_REMOVAL_ITEM}. using {@link ObjectValidator#setSummarizeItemLifecycleState(boolean)}.
     *
     * Data type: item
     */
    DEPRECATED_REMOVED_PLANNED_REMOVAL_ITEM,

    /**
     * Used when OID is not in correct format (UUID).
     *
     * Data type: {@link com.evolveum.midpoint.prism.PrismObjectValue} or {@link com.evolveum.midpoint.prism.PrismReferenceValue}
     */
    INCORRECT_OID_FORMAT
}
