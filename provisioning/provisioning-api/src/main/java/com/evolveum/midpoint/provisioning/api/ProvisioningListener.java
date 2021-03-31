/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
