/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.config;

/**
 * Interface for authentication module which works with internal midpoint credentials
 *
 * @author skublik
 */

public interface CredentialModuleAuthentication extends ModuleAuthentication {

    /**
     * @return name of used credential for authentication
     */
    String getCredentialName();
}
