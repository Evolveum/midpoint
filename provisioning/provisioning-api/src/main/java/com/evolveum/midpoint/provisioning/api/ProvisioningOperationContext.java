/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import java.util.function.Supplier;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ProvisioningOperationContext {

    private String requestIdentifier;

    private Supplier<ExpressionEnvironment> expressionEnvironmentSupplier;

    private ExpressionProfile expressionProfile;

    /**
     * This reference can be used if shadow is not available when trying to record audit event.
     * Reference is used mainly when modification of shadow object also invokes modification of different object on target system.
     * E.g. modification of user group membership (when real modification doesn't happen on user shadow but on group on target system).
     */
    private ObjectReferenceType shadowRef;

    public ObjectReferenceType shadowRef() {
        return shadowRef;
    }

    public ProvisioningOperationContext shadowRef(ObjectReferenceType shadowRef) {
        this.shadowRef = shadowRef;
        return this;
    }

    public String requestIdentifier() {
        return requestIdentifier;
    }

    public ProvisioningOperationContext requestIdentifier(String requestIdentifier) {
        this.requestIdentifier = requestIdentifier;
        return this;
    }

    public ExpressionProfile expressionProfile() {
        return expressionProfile;
    }

    public ProvisioningOperationContext expressionProfile(ExpressionProfile expressionProfile) {
        this.expressionProfile = expressionProfile;
        return this;
    }

    public Supplier<ExpressionEnvironment> expressionEnvironmentSupplier() {
        return expressionEnvironmentSupplier;
    }

    public ProvisioningOperationContext expressionEnvironmentSupplier(Supplier<ExpressionEnvironment> expressionEnvironment) {
        this.expressionEnvironmentSupplier = expressionEnvironment;
        return this;
    }
}
