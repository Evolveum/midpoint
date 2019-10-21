/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 *  An instance of this interface is created by calling static method create(AsyncUpdateSourceType configuration)
 *  on the implementing class.
 */
public interface AsyncUpdateSource {

    /**
     * Starts listening on this async update source.
     * Returns a ListeningActivity that is to be used to stop the listening.
     */
    ListeningActivity startListening(AsyncUpdateMessageListener listener) throws SchemaException;

    /**
     * Tests this async update source.
     */
    void test(OperationResult parentResult);

    // TODO consider adding lifecycle methods like connect(), disconnect(), dispose() here
    //  However, they are not really needed now, as the existing sources are stateless.

}
