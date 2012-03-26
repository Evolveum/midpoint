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

package com.evolveum.midpoint.repo.sql.query;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class AttributeDefinition extends Definition {

    private boolean indexed;
    private boolean reference;

    public boolean isReference() {
        return reference;
    }

    public void setReference(boolean reference) {
        this.reference = reference;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    @Override
    public Definition findDefinition(QName qname) {
        return null;
    }

    @Override
    public <T extends Definition> T findDefinition(QName qname, Class<T> type) {
        return null;
    }

    @Override
    public boolean isEntity() {
        return false;
    }

    @Override
    public String toString() {
        return getType().toString();
    }
}
