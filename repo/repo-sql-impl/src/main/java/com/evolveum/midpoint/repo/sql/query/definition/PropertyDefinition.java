/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
