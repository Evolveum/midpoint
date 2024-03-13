/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProvidersType;

import java.util.List;

public class CustomSecretProvidersCounter extends SecretProvidersCounter {

    public CustomSecretProvidersCounter() {
        super();
    }

    @Override
    protected List getListOfSecretProviders(SecretsProvidersType bean) {
        return bean.getCustom();
    }
}

