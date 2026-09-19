/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.schema.expression.MidPointTrustDescriptor;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionPrivilegesSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

@NullMarked
public class ExpressionConfigItem
        extends ConfigurationItem<ExpressionType>
        implements PrivilegesMixin<ExpressionType> {

    @SuppressWarnings("unused") // called dynamically
    public ExpressionConfigItem(ConfigurationItem<ExpressionType> original) {
        super(original);
    }

    protected ExpressionConfigItem(ExpressionType value, ConfigurationItemOrigin origin) {
        super(value, origin, null); // provide parent in the future
    }

    public static ExpressionConfigItem of(ExpressionType bean, ConfigurationItemOrigin origin) {
        return new ExpressionConfigItem(bean, origin);
    }

    public @Nullable ExecutionPrivilegesSpecificationType getPrivileges() throws ConfigurationException {
        return getPrivileges(
                value().getRunAsRef(),
                value().getPrivileges());
    }

    public boolean isAllowEmptyValues() {
        return Boolean.TRUE.equals(value().isAllowEmptyValues());
    }

    public boolean isTrace() {
        return Boolean.TRUE.equals(value().isTrace());
    }

    public MidPointTrustDescriptor getTrustDescriptorRequired() {
        var descriptor =
                MiscUtil.stateNonNull(
                        value().getTrustDescriptor(),
                        "TrustDescriptor is required but not set in %s", this);
        return (MidPointTrustDescriptor) descriptor; // safe because there are no other types of descriptors in the system
    }
}
