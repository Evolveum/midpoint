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

public class ProvisioningOperationContext {

    private String requestIdentifier;

    private Supplier<ExpressionEnvironment> expressionEnvironmentSupplier;

    private ExpressionProfile expressionProfile;

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

    public Supplier<ExpressionEnvironment> expressionEnvironment() {
        return expressionEnvironmentSupplier;
    }

    public ProvisioningOperationContext expressionEnvironment(Supplier<ExpressionEnvironment> expressionEnvironment) {
        this.expressionEnvironmentSupplier = expressionEnvironment;
        return this;
    }
}
