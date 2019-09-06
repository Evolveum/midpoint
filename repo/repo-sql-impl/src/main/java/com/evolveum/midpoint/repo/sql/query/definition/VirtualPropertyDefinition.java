/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class VirtualPropertyDefinition extends PropertyDefinition {

    private VirtualQueryParam[] additionalParams;

    public VirtualPropertyDefinition(QName jaxbName, Class jaxbType, String propertyName, Class propertyType) {
        super(jaxbName, jaxbType, propertyName, propertyType);
    }

    public VirtualQueryParam[] getAdditionalParams() {
        return additionalParams;
    }

    void setAdditionalParams(VirtualQueryParam[] additionalParams) {
        this.additionalParams = additionalParams;
    }

    @Override
    protected void toStringExtended(StringBuilder builder) {
        super.toStringExtended(builder);

        builder.append(", params=").append(additionalParams.length);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "VirtualProp";
    }
}
