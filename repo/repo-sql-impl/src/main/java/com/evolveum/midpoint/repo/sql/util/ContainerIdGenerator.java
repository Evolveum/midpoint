/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.data.common.container.L2Container;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ContainerIdGenerator implements IdentifierGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerIdGenerator.class);

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        if (object instanceof Container) {
            return generate((Container) object);
        } else if (object instanceof L2Container) {
            return generate((L2Container) object);
        } else {
            throw new HibernateException("Couldn't create id for '"
                    + object.getClass().getSimpleName() + "' not instance of '" + Container.class.getName() + "'.");
        }
    }

    private Integer generate(Container container) {
        if (container.getId() != null && container.getId() != 0) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Created id='{}' for '{}'.", container.getId(), toString(container));
            }
            return container.getId();
        }

        throw new RuntimeException("Unknown id, should not happen.");
    }

    private String toString(Container object) {
        StringBuilder builder = new StringBuilder();
        builder.append(object.getClass().getSimpleName());
        builder.append("[");
        builder.append(object.getOwnerOid());
        builder.append(",");
        builder.append(object.getId());
        builder.append("]");

        return builder.toString();
    }

    private Serializable generate(L2Container container) {
        if (container.getId() != null && container.getId() != 0) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Created id='{}' for '{}'.", container.getId(), toString(container));
            }
            return container.getId();
        }

        throw new RuntimeException("Unknown id, should not happen: " + container);
    }

    private String toString(L2Container container) {
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
