/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Registers old generic repository custom persisters through Hibernate 7 service bootstrap.
 */
public class MidPointPersisterServiceContributor implements ServiceContributor {

    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addService(PersisterClassResolver.class, new MidPointPersisterClassResolver());
    }
}
