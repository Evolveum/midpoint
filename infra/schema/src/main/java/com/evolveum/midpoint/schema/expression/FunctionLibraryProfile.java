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

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

/**
 * Specifies limitations on the use of a particular function library methods.
 */
public record FunctionLibraryProfile(
        @NotNull String libraryOid,
        @NotNull AccessDecision decision
)
        implements Serializable {

    public static FunctionLibraryProfile of(@NotNull FunctionLibraryProfileType bean) throws ConfigurationException {
        // TODO error locations
        return new FunctionLibraryProfile(
                configNonNull(
                        getOid(bean.getRef()),
                        () -> "No library OID in function library profile at " + bean.asPrismContainerValue().getPath()),
                AccessDecision.translate(
                        configNonNull(
                                bean.getDecision(),
                                () -> "No action decision in scripting profile at " + bean.asPrismContainerValue().getPath())));
    }

}
