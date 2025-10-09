/*
 * Copyright (C) 2020-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

/**
 * Created by Viliam Repan (lazyman).
 */
@Experimental
public class OtherModuleAuthentication extends ModuleAuthenticationImpl {

    public OtherModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.OTHER, sequenceModule);
        setType(ModuleType.LOCAL);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public ModuleAuthenticationImpl clone() {
        OtherModuleAuthentication module = new OtherModuleAuthentication(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        clone(module);
        return module;
    }
}
