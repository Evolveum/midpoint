/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.ShortDumpable;

/** Temporary */
public interface AbstractAuthorizationParameters extends ShortDumpable {

    default boolean hasValue() {
        return getValue() != null;
    }

    PrismValue getValue();
}
