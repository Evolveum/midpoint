/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.google.common.annotations.VisibleForTesting;

/**
 * Represents a source of asynchronous update messages.
 *
 * At technical level, such sources can be either passive ("pull" mode) or active ("push" mode). An example of the former
 * is a JMS queue that is accessed via MessageConsumer.receive() method. An example of the latter is a JMS queue that is accessed
 * via message listener, i.e. using MessageConsumer.setMessageListener() method. Or REST endpoint that receives messages as they
 * arrive.
 *
 * An instance of this interface is created by calling static method create(AsyncUpdateSourceType configuration)
 * on the implementing class.
 */
@VisibleForTesting // just to provide mock implementations
public interface AsyncUpdateSource {

    /**
     * Tests this async update source.
     */
    void test(OperationResult parentResult);

    /**
     * Closes this source and releases all resources it holds.
     */
    void close();

    // TODO consider adding other lifecycle methods like connect(), disconnect() here
    //  However, they are not really needed now, as the existing sources are stateless.

}
