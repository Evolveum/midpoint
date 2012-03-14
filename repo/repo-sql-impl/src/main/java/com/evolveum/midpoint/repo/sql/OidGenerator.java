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

import com.evolveum.midpoint.repo.sql.data.a1.RContainer;
import com.evolveum.midpoint.repo.sql.data.a1.ROwnable;
import com.evolveum.midpoint.repo.sql.data.a1.RObjectType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author lazyman
 */
public class OidGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
        RContainer container = null;
        if (object instanceof ROwnable) {
            container = ((ROwnable) object).getContainerOwner();
        } else if (object instanceof RObjectType) {
            container = (RContainer) object;
        }

        if (container == null) {
            return null;
        }

        if (StringUtils.isNotEmpty(container.getOid())) {
            return container.getOid();
        }

        return UUID.randomUUID().toString();
    }
}
