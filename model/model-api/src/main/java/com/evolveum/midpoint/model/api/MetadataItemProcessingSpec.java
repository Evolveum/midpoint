/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

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
}
