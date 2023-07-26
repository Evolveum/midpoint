/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.factory.module;

import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

import org.apache.commons.lang3.StringUtils;

public class ModuleConfigurationCreator<MC extends ModuleWebSecurityConfiguration, MT extends AbstractAuthenticationModuleType> {

    private MC configuration;
    private MT moduleType;

    private String sequenceSuffix;

    public ModuleConfigurationCreator(MC configuration, MT moduleType) {
        this.configuration = configuration;
        this.moduleType = moduleType;
    }

    public ModuleConfigurationCreator<MC, MT> sequenceSuffix(String sequenceSuffix) {
        this.sequenceSuffix = sequenceSuffix;
        return this;
    }


    public MC create() {
        configuration.setSequenceSuffix(sequenceSuffix);
        configuration.setModuleIdentifier(getAuthenticationModuleIdentifier(moduleType));

        return configuration;
    }

    protected static String getAuthenticationModuleIdentifier(AbstractAuthenticationModuleType module) {
        return StringUtils.isNotEmpty(module.getIdentifier()) ? module.getIdentifier() : module.getName();
    }
}
