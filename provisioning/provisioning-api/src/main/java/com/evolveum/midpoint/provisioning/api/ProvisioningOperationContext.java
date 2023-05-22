/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;

public class ProvisioningOperationContext {

    private String requestIdentifier;

    private ExpressionProfile expressionProfile;

    private ObjectDeltaSchemaLevelUtil.NameResolver nameResolver;

    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    public void setRequestIdentifier(String requestIdentifier) {
        this.requestIdentifier = requestIdentifier;
    }

    public ExpressionProfile getExpressionProfile() {
        return expressionProfile;
    }

    public void setExpressionProfile(ExpressionProfile expressionProfile) {
        this.expressionProfile = expressionProfile;
    }

    public ObjectDeltaSchemaLevelUtil.NameResolver getNameResolver() {
        return nameResolver;
    }

    public void setNameResolver(ObjectDeltaSchemaLevelUtil.NameResolver nameResolver) {
        this.nameResolver = nameResolver;
    }
}
