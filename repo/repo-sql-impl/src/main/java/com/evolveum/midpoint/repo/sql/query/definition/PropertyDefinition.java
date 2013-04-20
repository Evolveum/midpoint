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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class PropertyDefinition extends Definition {

    //jpa special types
    private boolean lob;
    private boolean enumerated;
    //jpa special things
    private boolean indexed;

    public PropertyDefinition(QName jaxbName, Class jaxbType, String propertyName, Class propertyType) {
        super(jaxbName, jaxbType, propertyName, propertyType);
    }

    public boolean isLob() {
        return lob;
    }

    public boolean isPolyString() {
        return RPolyString.class.equals(getJpaType());
    }

    public boolean isEnumerated() {
        return enumerated;
    }

    public boolean isIndexed() {
        return indexed;
    }

    void setLob(boolean lob) {
        this.lob = lob;
    }

    void setEnumerated(boolean enumerated) {
        this.enumerated = enumerated;
    }

    void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    @Override
    protected void toStringExtended(StringBuilder builder) {
        builder.append(", lob=").append(isLob());
        builder.append(", enumerated=").append(isEnumerated());
        builder.append(", indexed=").append(isIndexed());
    }

    @Override
    protected String getDebugDumpClassName() {
        return "Prop";
    }
}
