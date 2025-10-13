/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;

public class BareResourceSchemaImpl extends ResourceSchemaImpl implements BareResourceSchema {

    BareResourceSchemaImpl(@NotNull NativeResourceSchema nativeSchema) {
        super(nativeSchema);
    }
}
