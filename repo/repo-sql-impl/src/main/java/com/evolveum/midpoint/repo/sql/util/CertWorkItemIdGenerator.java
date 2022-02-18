/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationWorkItem;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CertWorkItemIdGenerator implements IdentifierGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(CertWorkItemIdGenerator.class);

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        if (object instanceof RAccessCertificationWorkItem) {
            return generate((RAccessCertificationWorkItem) object);
        } else {
            throw new HibernateException("Couldn't create id for '"
                    + object.getClass().getSimpleName() + "' not instance of '" + RAccessCertificationWorkItem.class.getName() + "'.");
        }
    }

    private Serializable generate(RAccessCertificationWorkItem container) {
        if (container.getId() != null && container.getId() != 0) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Created id='{}' for '{}'.", container.getId(), toString(container));
            }
            return container.getId();
        }

        throw new RuntimeException("Unknown id, should not happen.");
    }

    private String toString(RAccessCertificationWorkItem container) {
        StringBuilder builder = new StringBuilder();
        builder.append(container.getClass().getSimpleName());
        builder.append("[");
        builder.append(container.getOwnerOwnerOid());
        builder.append(",");
        builder.append(container.getOwnerId());
        builder.append(",");
        builder.append(container.getId());
        builder.append("]");
        return builder.toString();
    }
}
