/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a connection to asynchronous provisioning target (e.g. JMS queue residing in a broker).
 */
@VisibleForTesting // just to provide mock implementations
public interface AsyncProvisioningTarget {

    /**
     * Prepares this object for use. This may mean connecting to the real target (e.g. JMS broker).
     * Another option is to defer connecting until there is a real need to do that (testing
     * connection or sending a message).
     */
    void connect();

    /**
     * Informs this object that it can disconnect from the real target (e.g. JMS broker);
     * after all pending operations are done.
     */
    void disconnect();

    /**
     * Creates a copy of the target - in the initial (unconnected) state.
     */
    @NotNull AsyncProvisioningTarget copy();

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
