/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

/**
 * @author skublik
 */

public class HttpHeaderModuleAuthentication extends ModuleAuthenticationImpl {

    public HttpHeaderModuleAuthentication() {
        super(AuthenticationModuleNameConstants.HTTP_HEADER);
        setType(ModuleType.LOCAL);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public ModuleAuthenticationImpl clone() {
        HttpHeaderModuleAuthentication module = new HttpHeaderModuleAuthentication();
        module.setAuthentication(this.getAuthentication());
        clone(module);
        return module;
    }
}
