/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProvidersType;

import java.util.List;

public class EnvironmentVariablesSecretProvidersCounter extends SecretProvidersCounter {

    public EnvironmentVariablesSecretProvidersCounter() {
        super();
    }

    @Override
    protected List getListOfSecretProviders(SecretsProvidersType bean) {
        return bean.getEnvironmentVariables();
    }
}

