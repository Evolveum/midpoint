/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VirtualAssignmentSpecification<R extends AbstractRoleType>(
        @NotNull Class<R> type,
        @Nullable ObjectFilter filter) {
}
