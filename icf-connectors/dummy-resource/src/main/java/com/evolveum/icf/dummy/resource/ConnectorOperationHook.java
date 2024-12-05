/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import java.util.Collection;

/**
 * A piece of code that is called on specific places in the connector code.
 * It is defined here, because midPoint testing code has no direct access to the dummy connector, as it's loaded by ConnId.
 * The testing code must define these hooks right on a {@link DummyResource} instance.
 * And the connector looks them up there.
 */
public interface ConnectorOperationHook {

    /**
     * Called right after an object is successfully added to the dummy resource via connector `create` operation.
     * May be called multiple times within one call, if there are embedded objects being created.
     * (But once for each particular object.)
     */
    void afterCreateOperation(DummyObject object);

    /**
     * Called right after an object is successfully modified on the dummy resource via connector `update` operation.
     * Currently limited to "modern", delta-based modifications.
     * (Not called for "legacy" add/delete/replace attribute values operations.)
     *
     * The actual type of the `modifications` parameter depends on ConnId API, so it cannot be specified here.
     * Either we have to live with this, or we must move the hook registry to the `dummy-connector` project somehow.
     */
    void afterModifyOperation(DummyObject object, Collection<?> modifications);
}
