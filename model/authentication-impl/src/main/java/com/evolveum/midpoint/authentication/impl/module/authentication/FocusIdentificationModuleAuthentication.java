/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

public class FocusIdentificationModuleAuthentication extends CredentialModuleAuthenticationImpl {

    public FocusIdentificationModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION, sequenceModule);
    }

    public ModuleAuthenticationImpl clone() {
        FocusIdentificationModuleAuthentication module = new FocusIdentificationModuleAuthentication(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
        return module;
    }
}
