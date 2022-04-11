/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Adapted from https://vladmihalcea.com/how-to-get-access-to-database-table-metadata-with-hibernate-5/
 */
public class MetadataExtractorIntegrator implements org.hibernate.integrator.spi.Integrator {

    private static Metadata metadata;

    public static Metadata getMetadata() {
        return metadata;
    }

    @Override
    public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
        MetadataExtractorIntegrator.metadata = metadata;
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
    }
}
