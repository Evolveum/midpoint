/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import com.evolveum.midpoint.util.LocalizableMessage;

public record CleanupMessage<D>(Type type, LocalizableMessage message, D data) {

    public enum Type {

        MISSING_REFERENCE,

        OPTIONAL_CLEANUP,

        PROTECTED_STRING,

        MISSING_MAPPING_NAME,

        MULTIVALUE_REF_WITHOUT_OID
    }
}
