/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * How should be errors related to processing of fetched objects handled?
 *
 * (Currently applies only to `search` operation.)
 */
@Experimental
public enum UcfFetchErrorReportingMethod {

    /**
     * Errors are reported by throwing an exception. No object is passed to the handler (nor returned). This is the legacy way.
     */
    EXCEPTION,

    /**
     * Errors are reported within {@link FetchedUcfObject} instance passed to the handler or returned.
     * This allows more selective error handling in the client.
     */
    UCF_OBJECT
}
