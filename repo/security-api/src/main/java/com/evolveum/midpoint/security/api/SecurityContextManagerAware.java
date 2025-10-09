/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.api;

/**
 *  Needs to know about the SecurityContextManager implementation.
 */
public interface SecurityContextManagerAware {

    void setSecurityContextManager(SecurityContextManager manager);

    SecurityContextManager getSecurityContextManager();

}
