/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
public interface ResourceObjectAsyncChangeListener {

    /**
     * Called when given change has to be processed.
     */
    void onChange(ResourceObjectAsyncChange change, OperationResult result);
}
