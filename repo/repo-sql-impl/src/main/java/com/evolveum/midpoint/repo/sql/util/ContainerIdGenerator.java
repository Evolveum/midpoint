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

import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
public class ContainerIdGenerator implements IdentifierGenerator {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerIdGenerator.class);

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        if (object instanceof RAnyContainer) {
            RAnyContainer any = (RAnyContainer) object;
            RContainer owner = any.getOwner();
            Short id = owner.getId();
            if (id == null) {
                id = generate(owner);
                owner.setId(id);
            }
            LOGGER.trace("Created id='{}' for any.", new Object[]{id});
            return id;
        }

        if (!(object instanceof RContainer)) {
            throw new HibernateException("Couldn't create id for '"
                    + object.getClass().getSimpleName() + "' not instance of RContainer.");
        }

        return generate((RContainer) object);
    }

    private Short generate(RContainer container) {
        if (container instanceof RObject) {
            LOGGER.trace("Created id='0' for '{}'.", new Object[]{toString(container)});
            return 0;
        }

        if (container.getId() != null && container.getId() != 0) {
            LOGGER.trace("Created id='{}' for '{}'.", new Object[]{container.getId(), toString(container)});
            return container.getId();
        }

        if (!(container instanceof ROwnable)) {
            throw new HibernateException("Couldn't create id for '"
                    + container.getClass().getSimpleName() + "' (should not happen).");
        }

        RContainer parent = ((ROwnable) container).getContainerOwner();
        Set<RContainer> containers = getChildrenContainers(parent);

        Short id = getNextId(containers);
        LOGGER.trace("Created id='{}' for '{}'.", new Object[]{id, toString(container)});
        return id;
    }

    private String toString(Object object) {
        RContainer container = (RContainer) object;

        StringBuilder builder = new StringBuilder();
        builder.append(object.getClass().getSimpleName());
        builder.append("[");
        builder.append(container.getOid());
        builder.append(",");
        builder.append(container.getId());
        builder.append("]");

        return builder.toString();
    }

    private Short getNextId(Set<? extends RContainer> set) {
        Short id = 0;
        if (set != null) {
            for (RContainer container : set) {
                Short contId = container.getId();
                if (contId != null && contId > id) {
                    id = contId;
                }
            }
        }

        return (short) (id + 1);
    }

    /**
     * This method provides simplyfied id generator (without DB selects)
     * Improve it later (fixes MID-1430).
     *
     * @param container
     */
    public void generateIdForObject(RObject container) {
        if (container == null) {
            return;
        }

        container.setId((short) 0);

        Set<RContainer> containers = getChildrenContainers(container);
        Set<Short> usedIds = new HashSet<>();
        for (RContainer c : containers) {
            if (c.getId() != null) {
                usedIds.add(c.getId());
            }
        }

        Short nextId = 1;
        for (RContainer c : containers) {
            if (c.getId() != null) {
                continue;
            }

            while (usedIds.contains(nextId)) {
                nextId++;
            }

            c.setId(nextId);
            usedIds.add(nextId);
        }
    }

    private Set<RContainer> getChildrenContainers(RContainer parent) {
        Set<RContainer> containers = new HashSet<>();
        if (parent instanceof RObject) {
            containers.addAll(((RObject) parent).getTrigger());
        }

        if (parent instanceof RFocus) {
            containers.addAll(((RFocus) parent).getAssignments());
        }

        if (parent instanceof RAbstractRole) {
            RAbstractRole role = (RAbstractRole) parent;
            containers.addAll(role.getExclusion());
            containers.addAll(role.getAuthorization());
        }

        return containers;
    }
}
