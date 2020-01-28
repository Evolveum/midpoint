/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.MutablePrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaFactory;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class SchemaFactoryImpl implements SchemaFactory {

    @NotNull private final PrismContext prismContext;

    public SchemaFactoryImpl(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public MutablePrismSchema createPrismSchema(@NotNull String namespace) {
        return new PrismSchemaImpl(namespace, prismContext);
    }
}
