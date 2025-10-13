/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

public class FileSecretProvidersCounter extends SecretProvidersCounter {

    public FileSecretProvidersCounter() {
        super();
    }

    @Override
    protected List getListOfSecretProviders(SecretsProvidersType bean) {
        return bean.getFile();
    }
}

