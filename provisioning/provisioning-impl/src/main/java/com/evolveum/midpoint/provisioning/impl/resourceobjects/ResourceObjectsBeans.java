/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;

/** Beans useful for non-Spring components within this package. */
@Component
class ResourceObjectsBeans {

    private static ResourceObjectsBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static ResourceObjectsBeans get() {
        return instance;
    }

    // Local ones
    @Autowired ResourceObjectConverter resourceObjectConverter;
    @Autowired FakeIdentifierGenerator fakeIdentifierGenerator;
    @Autowired DelineationProcessor delineationProcessor;

    // From other parts of the code
    @Autowired CacheConfigurationManager cacheConfigurationManager;

}
