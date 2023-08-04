/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionReturnMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;

/**
 * Represents an {@link ExpressionType} that is part of a {@link FunctionLibraryType} as a custom function.
 *
 * Intentionally differs from the standard naming convention. The embedded value is {@link ExpressionType} but it is used
 * in a specialized way.
 */
public class FunctionConfigItem extends ExpressionConfigItem {

    // called dynamically
    public FunctionConfigItem(@NotNull ConfigurationItem<ExpressionType> original) {
        super(original);
    }

    private FunctionConfigItem(@NotNull ExpressionType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static FunctionConfigItem of(@NotNull ExpressionType bean, @NotNull ConfigurationItemOrigin origin) {
        return new FunctionConfigItem(bean, origin);
    }

    /** Unlike general {@link ExpressionConfigItem}, the function must have a name. */
    public @NotNull String getName() throws ConfigurationException {
        return MiscUtil.configNonNull(
                value().getName(),
                () -> "No name in " + fullDescription());
    }

    public @NotNull String getCommaDelimitedParameterNames() {
        return value().getParameter().stream()
                .map(p -> "'" + p.getName() + "'")
                .collect(Collectors.joining(", "));
    }

    /** Matching parameter names, ignoring all the other information (e.g., types). */
    public boolean doesMatchArguments(Collection<String> argumentNames) {
        var parameters = value().getParameter();
        if (argumentNames.size() != parameters.size()) {
            return false;
        }
        for (String argumentName : argumentNames) {
            if (parameters.stream().noneMatch(
                    param -> param.getName().equals(argumentName))) {
                return false;
            }
        }
        return true;
    }

    public @NotNull ExpressionParameterConfigItem getParameter(@NotNull String paramName) throws ConfigurationException {
        var matching = value().getParameter().stream()
                .filter(param -> param.getName().equals(paramName))
                .toList();
        var paramBean = MiscUtil.extractSingletonRequired(
                matching,
                () -> new ConfigurationException(
                        "More than one parameter named '%s' in function '%s' found".formatted(
                                paramName, value().getName())),
                () -> new ConfigurationException(
                        "No parameter named '%s' in function '%s' found. Known parameters are: %s.".formatted(
                                paramName, value().getName(), getCommaDelimitedParameterNames())));
        return ExpressionParameterConfigItem.of(paramBean, origin());
    }

    public @NotNull QName getReturnTypeName() {
        return defaultIfNull(value().getReturnType(), DOMUtil.XSD_STRING);
    }

    // FIXME it is interesting that the default for multiplicity is not clear, see uses of these methods
    //  Maybe it has no default?

    public boolean isReturnMultiValue() {
        return value().getReturnMultiplicity() == ExpressionReturnMultiplicityType.MULTI;
    }

    public boolean isReturnSingleValue() {
        return value().getReturnMultiplicity() == ExpressionReturnMultiplicityType.SINGLE;
    }
}
