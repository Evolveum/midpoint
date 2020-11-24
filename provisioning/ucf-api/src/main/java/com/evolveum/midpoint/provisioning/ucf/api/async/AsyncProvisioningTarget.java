/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
public interface AsyncProvisioningTarget {

    /**
     * Tests this target for reachability.
     */
    void test(OperationResult result);

    /**
     * Sends out a request to this target.
     * Throws an exception if the operation is not successful.
     * @return Asynchronous operation reference
     */
    String send(AsyncProvisioningRequest request, OperationResult result);
}
