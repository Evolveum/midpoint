package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

@Experimental
public interface ValueMetadata extends PrismContainer<Containerable>, ShortDumpable {

    ValueMetadata clone();

    default void addMetadataValue(PrismContainerValue<?> metadataValue) throws SchemaException {
        //noinspection unchecked
        add((PrismContainerValue) metadataValue);
    }
}
