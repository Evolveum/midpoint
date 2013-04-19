/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
            Long id = owner.getId();
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

    private Long generate(RContainer container) {
        if (container instanceof RObject) {
            LOGGER.trace("Created id='0' for '{}'.", new Object[]{toString(container)});
            return 0L;
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
        if (!(parent instanceof RFocus)) {
            return null;
        }

        Set<RContainer> containers = new HashSet<RContainer>();
        if (parent instanceof RFocus) {
            containers.addAll(((RFocus) parent).getAssignment());
        }

        if (parent instanceof RAbstractRole) {
            RAbstractRole role = (RAbstractRole) parent;
            containers.addAll(role.getAssignment());
            containers.addAll(role.getExclusions());
            containers.addAll(role.getAuthorizations());
        }

        Long id = getNextId(containers);
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

    private Long getNextId(Set<? extends RContainer> set) {
        Long id = 0L;
        if (set != null) {
            for (RContainer container : set) {
                Long contId = container.getId();
                if (contId != null && contId > id) {
                    id = contId;
                }
            }
        }

        return id + 1;
    }
}
