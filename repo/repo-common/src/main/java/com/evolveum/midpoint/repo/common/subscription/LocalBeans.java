/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

@Component
public class LocalBeans {

    private static LocalBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static LocalBeans get() {
        return instance;
    }

    @Autowired public SystemObjectCache systemObjectCache;
    @Autowired public SubscriptionStateCache subscriptionStateHolder;
    @Autowired public SystemFeaturesEnquirer systemFeaturesEnquirer;
}
