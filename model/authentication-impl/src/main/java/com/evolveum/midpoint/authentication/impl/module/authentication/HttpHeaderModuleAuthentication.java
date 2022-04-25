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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

/**
 * @author skublik
 */

public class HttpHeaderModuleAuthentication extends ModuleAuthenticationImpl {

    public HttpHeaderModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.HTTP_HEADER, sequenceModule);
        setType(ModuleType.LOCAL);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public ModuleAuthenticationImpl clone() {
        HttpHeaderModuleAuthentication module = new HttpHeaderModuleAuthentication(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        clone(module);
        return module;
    }
}
