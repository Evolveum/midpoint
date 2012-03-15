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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.sql.data.common.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RRole;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContainerIdGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        if (object instanceof RObject) {
            return 0L;
        } else if (object instanceof RAssignment) {
            RAssignment assignment = (RAssignment) object;
            RObject o = assignment.getOwner();
            if (o instanceof RUser) {
                RUser user = (RUser) o;
                return getNextId(user.getAssignments());
            } else if (o instanceof RRole) {
                RRole role = (RRole) o;
                return getNextId(role.getAssignments());
            }
        }

        return null;
    }

    private Long getNextId(Set<RAssignment> set) {
        Long id = 0L;
        for (RAssignment assignment : set) {
            if (assignment.getId() != null && assignment.getId() > id) {
                id = assignment.getId();
            }
        }

        return id + 1;
    }
}
