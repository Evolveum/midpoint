/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.config.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;

import java.util.List;

public class CorrelationModuleAuthenticationImpl extends ModuleAuthenticationImpl implements CorrelationModuleAuthentication {

    private String correlatorIdentifier;

    public CorrelationModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.CORRELATION, sequenceModule);
        setSufficient(false);
    }

    public ModuleAuthenticationImpl clone() {
        CorrelationModuleAuthenticationImpl module = new CorrelationModuleAuthenticationImpl(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setCorrelatorIdentifier(this.correlatorIdentifier);
        super.clone(module);
        return module;
    }

    public void setCorrelatorIdentifier(String correlatorIdentifier) {
        this.correlatorIdentifier = correlatorIdentifier;
    }
}
