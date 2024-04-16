/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProvidersType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import java.util.List;

/**
 * Superclass for counter of secrets providers panel that contains table
 */
public abstract class SecretProvidersCounter extends SimpleCounter<AssignmentHolderDetailsModel<SystemConfigurationType>, SystemConfigurationType> {

    public SecretProvidersCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<SystemConfigurationType> model, PageBase pageBase) {
        SystemConfigurationType object = model.getObjectType();
        SecretsProvidersType bean = object.getSecretsProviders();
        if (bean == null) {
            return 0;
        }

        var providers = getListOfSecretProviders(bean);

        return providers != null ? providers.size() : 0;
    }

    protected abstract List getListOfSecretProviders(SecretsProvidersType bean);

}

