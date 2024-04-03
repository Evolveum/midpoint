/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Useful Spring beans. */
@Component
public class ConnIdBeans {

    private static ConnIdBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ConnIdBeans get() {
        return instance;
    }

    @Autowired Protector protector;
    @Autowired LocalizationService localizationService;

}
