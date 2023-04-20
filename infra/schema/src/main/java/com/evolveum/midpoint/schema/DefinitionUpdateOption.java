/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Should be definitions updated after an object is retrieved via model API?
 * See `applySchemasAndSecurity` method.
 *
 * Applicable only on the root level.
 *
 * TEMPORARY, mainly for the purposes of experiments with various approaches during midPoint 4.8 development.
 */
@Experimental
public enum DefinitionUpdateOption {

    /**
     * The object or container definition is not to be updated at all.
     */
    NONE,

    /**
     * Only the definition at the root level (object or container) is updated.
     * Definitions of the child items (if any) are not touched.
     * For example, the client may apply the root definition if needed.
     */
    ROOT_ONLY,

    /**
     * Updated definition is propagated onto all items in the returned structure.
     */
    DEEP;
}
