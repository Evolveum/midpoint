/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api.authentication;

import com.evolveum.midpoint.authentication.api.StateOfModule;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.springframework.security.core.Authentication;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */

public interface ModuleAuthentication {
    String getNameOfModule();

    String getNameOfModuleType();

    StateOfModule getState();

    void setState(StateOfModule state);

    Authentication getAuthentication();

    @Experimental
    void setAuthentication(Authentication authentication);

    String getPrefix();

    QName getFocusType();

    boolean isInternalLogout();
}
