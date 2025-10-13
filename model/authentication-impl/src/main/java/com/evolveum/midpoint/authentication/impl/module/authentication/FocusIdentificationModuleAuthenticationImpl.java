/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.config.FocusIdentificationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;

import java.util.List;

public class FocusIdentificationModuleAuthenticationImpl extends CredentialModuleAuthenticationImpl implements FocusIdentificationModuleAuthentication {

    private List<ModuleItemConfigurationType> moduleConfiguration;

    public FocusIdentificationModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION, sequenceModule);
        setSufficient(false);
    }

    public ModuleAuthenticationImpl clone() {
        FocusIdentificationModuleAuthenticationImpl module = new FocusIdentificationModuleAuthenticationImpl(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setModuleConfiguration(this.getModuleConfiguration());
        super.clone(module);
        return module;
    }

    public void setModuleConfiguration(List<ModuleItemConfigurationType> moduleConfiguration) {
        this.moduleConfiguration = moduleConfiguration;
    }

    public List<ModuleItemConfigurationType> getModuleConfiguration() {
        return moduleConfiguration;
    }

}
