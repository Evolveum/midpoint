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

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

/**
 * http://www.devx.com/Java/Article/30396/1954
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class GeneratedEntityInterceptor extends EmptyInterceptor {

    public static final String code_id = "$Id$";

    /**
     * Determines if a SimpleDomain entity is transient.
     * @param entity   the object to test
     * @return   the value of the generated entity's transient property
     *          or null if the object is not a generated entity
     */
    @Override
    public Boolean isTransient(Object entity) {
        if (entity instanceof SimpleDomainObject) {
            return ((SimpleDomainObject) entity).isTransient(); //? Boolean.TRUE : Boolean.FALSE;
        } else {
            return super.isTransient(entity);
        }
    }

    /**
     * Changes the persistent status of the generated entity upon saving.
     * This method simply sets the generated entity's transient property to
     * false.
     *
     * @see SimpleDomainObject#setTransient(boolean)
     */
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof SimpleDomainObject) {
            ((SimpleDomainObject) entity).setTransient(Boolean.FALSE);
        }
        return super.onSave(entity, id, state, propertyNames, types);
    }

    /**
     * Changes the persistent status of the generated entity on load.
     * This method simply sets the generated entity's transient property to
     * false.
     *
     * @see SimpleDomainObject#setTransient(boolean)
     */
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof SimpleDomainObject) {
            ((SimpleDomainObject) entity).setTransient(Boolean.FALSE);
        }

        return super.onLoad(entity, id, state, propertyNames, types);
    }

    /**
     * Changes the persistent status of the generated entity upon deletion.
     * This method simply sets the generated entity's transient property to
     * true.
     *
     * @see SimpleDomainObject#setTransient(boolean)
     */
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof SimpleDomainObject) {
            ((SimpleDomainObject) entity).setTransient(Boolean.TRUE);
        }
        super.onDelete(entity, id, state, propertyNames, types);
    }
}
