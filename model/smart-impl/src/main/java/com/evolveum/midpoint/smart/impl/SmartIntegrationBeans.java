/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.crypto.Protector;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelService;

/**
 * Commonly-used beans for smart-impl module.
 *
 * This class is intended to be used in classes that are not managed by Spring.
 * (To avoid massive transfer of references to individual beans from Spring-managed class
 * to the place where the beans are needed.)
 */
@Component
public class SmartIntegrationBeans {

    private static SmartIntegrationBeans instance;

    @Autowired public ModelService modelService;
    @Autowired public SmartIntegrationServiceImpl smartIntegrationService;
    @Autowired public Clock clock;
    @Autowired public LocalizationService localizationService;
    @Autowired public Protector protector;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static SmartIntegrationBeans get() {
        return instance;
    }
}
