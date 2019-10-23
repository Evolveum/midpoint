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
public class VirtualEntityDefinition extends EntityDefinition {

    private VirtualQueryParam[] additionalParams;

    public VirtualEntityDefinition(QName jaxbName, Class jaxbType, String jpaName, Class jpaType) {
        super(jaxbName, jaxbType, jpaName, jpaType);
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
        return "VirtualEnt";
    }
}
