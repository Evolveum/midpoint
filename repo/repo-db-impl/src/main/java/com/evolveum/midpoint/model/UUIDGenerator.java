/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class UUIDGenerator implements IdentifierGenerator {

    public static final String code_id = "$Id$";

    //@Override
    public Serializable generate(SessionImplementor session, Object parent)
            throws HibernateException {
        //OPENIDM-137 - import of objects has to preserve oids
        if (parent instanceof IdentifiableBase) {
            IdentifiableBase ib = (IdentifiableBase) parent;
            if (null != ib.getOid()) {
                return ib.getOid();
            }
        }
        UUID u = UUID.randomUUID();
        return u;
    }
}
