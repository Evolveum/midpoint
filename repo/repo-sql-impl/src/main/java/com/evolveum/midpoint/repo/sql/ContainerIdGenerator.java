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

import com.evolveum.midpoint.repo.sql.data.atest.IdentifiableContainer;
import com.evolveum.midpoint.repo.sql.data.atest.O;
import com.evolveum.midpoint.repo.sql.data.common.RObjectType;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContainerIdGenerator implements IdentifierGenerator {
    
    private static final Trace LOGGER = TraceManager.getTrace(ContainerIdGenerator.class);

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        LOGGER.info("aaaaaaaaaaaaaaaaaaaaaaaa");
        if (object instanceof IdentifiableContainer) {
            IdentifiableContainer container = (IdentifiableContainer)object;
            if (container.getId() != null) {
                return container.getId();
            }
            
            O owner = container.getOwner();
            if (owner == null) {
                return null;
            }

            //todo fix qname
            Collection<IdentifiableContainer> containers = owner.getContainers(null);
            Long id = 0L;
            for (IdentifiableContainer iContainer : containers) {
                if (iContainer.getId() != null && iContainer.getId() > id) {
                    id = iContainer.getId();
                }
            }   
            
            return id+1;
        }
        
        return null;
    }
}
