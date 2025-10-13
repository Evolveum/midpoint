/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Provides information about processing of given metadata item (e.g. provenance)
 * related to various data items (e.g. givenName, familyName, etc).
 */
@Experimental
public interface MetadataItemProcessingSpec extends DebugDumpable {

    /**
     * @return true if the given dataItem (e.g. fullName) supports full processing of the relevant metadata (e.g. provenance).
     */
    boolean isFullProcessing(ItemPath dataItem) throws SchemaException;

    /**
     * @return true if the given dataItem (e.g. fullName) supports full processing of the relevant metadata (e.g. provenance).
     */
    default boolean isFullProcessing(ItemPath dataItem, ItemDefinition<?> definition) throws SchemaException {
        return isFullProcessing(dataItem);
    }
}
