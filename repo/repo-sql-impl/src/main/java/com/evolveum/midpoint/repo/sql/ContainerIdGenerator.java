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

import com.evolveum.midpoint.repo.sql.data.a1.Container;
import com.evolveum.midpoint.repo.sql.data.a1.O;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

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
        if (object instanceof O)   {
            return 0L;
        } else if (object instanceof Container) {
            //todo assignment get parent also extesion, resource swhadow attributes
        }
        
        //a0 test
//        if (object instanceof Assignment) {
//            Assignment assignment = (Assignment) object;
//            if (assignment.getId() != null && assignment.getOwner() == null) {
//                return assignment.getId();
//            }
//
//            Set<Assignment> assignments = null;
//            if (assignment.getOwner() instanceof User) {
//                assignments = ((User) assignment.getOwner()).getAssignments();
//            } else if (assignment.getOwner() instanceof Role) {
//                assignments = (((Role) assignment.getOwner()).getAssignments());
//            }
//            if (assignments == null) {
//                return null;
//            }
//            Long id = 0L;
//            for (Assignment item : assignments) {
//                if (item.getId() != null && item.getId() > id) {
//                    id = item.getId();
//                }
//            }
//            return id + 1;
//        }

        return null;

        //todo remove
//        if (object instanceof A) {
//            A a = (A)object;
//            if (a.getId() != null) {
//                return a.getId();
//            }
//            if (a.getR() == null) {
//                return null;
//            }
//            Set<A> set = a.getR().getAset();
//            Long id = 0L;
//            for (A aa : set) {
//                if (aa.getId() != null && aa.getId() > id) {
//                    id = aa.getId();
//                }
//            }
//
//            return id + 1;
//        }
//        
//        if (object instanceof IdentifiableContainer) {
//            IdentifiableContainer container = (IdentifiableContainer) object;
//            if (container.getId() != null) {
//                return container.getId();
//            }
//
//            O owner = container.getOwner();
//            if (owner == null) {
//                return null;
//            }
//
//            //todo fix qname
//            Collection<IdentifiableContainer> containers = owner.getContainers(null);
//            Long id = 0L;
//            for (IdentifiableContainer iContainer : containers) {
//                if (iContainer.getId() != null && iContainer.getId() > id) {
//                    id = iContainer.getId();
//                }
//            }
//
//            return id + 1;
//        }
//
//        return null;
    }
}
