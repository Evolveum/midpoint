/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
