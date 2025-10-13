/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.cleanup;

import com.evolveum.midpoint.util.LocalizableMessage;

public record CleanupItem<D>(
        CleanupItemType type,
        LocalizableMessage message,
        D data) {

}
