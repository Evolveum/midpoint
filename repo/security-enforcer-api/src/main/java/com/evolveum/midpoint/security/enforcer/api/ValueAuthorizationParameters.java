/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.Nullable;

/**
 * Authorization parameters for sub-object operations.
 *
 * Temporary/experimental.
 */
public class ValueAuthorizationParameters<V extends PrismValue> implements AbstractAuthorizationParameters {

    @Nullable private final V value;

    public ValueAuthorizationParameters(@Nullable V value) {
        this.value = value;
    }

    public @Nullable V getValue() {
        return value;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("value='").append(MiscUtil.getDiagInfo(value, 100)).append('\'');
    }
}
