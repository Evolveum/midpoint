/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/** Represents an {@link FunctionExpressionEvaluatorType} i.e. a call to a library function. */
public class FunctionExpressionEvaluatorConfigItem extends ConfigurationItem<FunctionExpressionEvaluatorType> {

    // called dynamically
    public FunctionExpressionEvaluatorConfigItem(@NotNull ConfigurationItem<FunctionExpressionEvaluatorType> original) {
        super(original);
    }

    private FunctionExpressionEvaluatorConfigItem(@NotNull FunctionExpressionEvaluatorType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static FunctionExpressionEvaluatorConfigItem of(
            @NotNull FunctionExpressionEvaluatorType bean, @NotNull ConfigurationItemOrigin origin) {
        return new FunctionExpressionEvaluatorConfigItem(bean, origin);
    }

    public @NotNull String getLibraryOid() throws ConfigurationException {
        ObjectReferenceType functionLibraryRef = value().getLibraryRef();
        if (functionLibraryRef == null) {
            // Eventually, we could support a default value here: the current library where the expression is defined (if any)
            throw new ConfigurationException("No functions library defined in " + fullDescription());
        }
        return MiscUtil.configNonNull(
                functionLibraryRef.getOid(),
                () -> "No OID in function library reference in " + fullDescription());
    }

    public String getFunctionName() throws ConfigurationException {
        return MiscUtil.configNonNull(
                value().getName(),
                "No function name in %s", fullDescription());
    }

    public Collection<String> getArgumentNames() {
        return value().getParameter().stream()
                .map(p -> p.getName())
                .collect(Collectors.toSet());
    }

    public List<FunctionCallArgumentConfigItem> getArguments() {
        return value().getParameter().stream()
                .map(p -> FunctionCallArgumentConfigItem.of(p, origin()))
                .toList();
    }
}
