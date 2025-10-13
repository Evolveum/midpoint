/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.api;

/**
 * @author Radovan Semancik
 *
 */
@FunctionalInterface
public interface ProvisioningListener {

    /**
     * Returns a short name of the listener for debugging purposes.
     * E.g. "model synchronization service". This name is used in log and error messages.
     */
    String getName();

}
