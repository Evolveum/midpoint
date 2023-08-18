/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.io.Serializable;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BulkActionProfileType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.AccessDecision;

import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * Specifies limitations on the use of a particular bulk action (e.g. assign, unassign, etc).
 */
public record BulkActionProfile(@NotNull String action, @NotNull AccessDecision decision)
        implements Serializable {

    public static BulkActionProfile of(@NotNull BulkActionProfileType bean) throws ConfigurationException {
        // TODO error locations
        return new BulkActionProfile(
                configNonNull(
                        bean.getName(), () -> "No action name in bulk action profile at " + bean.asPrismContainerValue().getPath()),
                AccessDecision.translate(
                        configNonNull(
                                bean.getDecision(),
                                () -> "No action decision in bulk action profile at " + bean.asPrismContainerValue().getPath())));
    }
}
