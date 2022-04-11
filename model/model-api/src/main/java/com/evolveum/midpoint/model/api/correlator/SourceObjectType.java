/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

@Experimental
public enum SourceObjectType {

    FOCUS, PROJECTION;

    public static SourceObjectType fromVariable(@NotNull QName variableName) throws ConfigurationException {
        switch (variableName.getLocalPart()) {
            case ExpressionConstants.VAR_FOCUS:
            case ExpressionConstants.VAR_USER:
                return FOCUS;
            case ExpressionConstants.VAR_PROJECTION:
            case ExpressionConstants.VAR_SHADOW:
            case ExpressionConstants.VAR_ACCOUNT:
                return PROJECTION;
            default:
                throw new ConfigurationException("Unsupported variable name: " + variableName);
        }
    }
}
