/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryProfileType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LibraryFunctionProfileType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

/**
 * Specifies limitations on the use of a particular function library methods.
 */
public record FunctionLibraryProfile(
        @NotNull String libraryOid,
        @NotNull AccessDecision defaultDecision,
        @NotNull Map<String, AccessDecision> functionProfileMap)
        implements Serializable {

    public static FunctionLibraryProfile of(@NotNull FunctionLibraryProfileType bean) throws ConfigurationException {
        // TODO error locations

        Map<String, AccessDecision> functionProfileMap = new HashMap<>();
        for (LibraryFunctionProfileType functionProfileBean : bean.getFunction()) {
            var name = configNonNull(functionProfileBean.getName(), "Unnamed function profile in %s", bean);
            var decision = configNonNull(
                    functionProfileBean.getDecision(), "No decision in function profile '%s' in %s", name, bean);
            functionProfileMap.put(name, AccessDecision.translate(decision));
        }

        return new FunctionLibraryProfile(
                configNonNull(
                        getOid(bean.getRef()),
                        () -> "No library OID in function library profile at " + bean.asPrismContainerValue().getPath()),
                AccessDecision.translate(
                        configNonNull(
                                bean.getDecision(),
                                () -> "No action decision in scripting profile at " + bean.asPrismContainerValue().getPath())),
                Collections.unmodifiableMap(functionProfileMap));
    }

    @NotNull AccessDecision decideFunctionAccess(@NotNull String functionName) {
        return Objects.requireNonNullElse(
                functionProfileMap.get(functionName),
                defaultDecision);
    }
}
