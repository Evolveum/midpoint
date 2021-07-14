/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.mapping.ExtensionMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Delta processor for extension container stored as a single JSONB column.
 *
 * @param <T> type of the processed container (target)
 */
public class ExtensionContainerDeltaProcessor<T extends Containerable>
        extends ItemDeltaSingleValueProcessor<T> {

    private final JsonbPath jsonbPath;
    private final ExtensionMapping<?, ?> mapping;

    /**
     * Constructs the delta processor, here we care about type match for parameters, later we don't.
     *
     * @param <OS> schema type of the owner of the embedded container
     * @param <OQ> query type of the owner entity
     * @param <OR> type of the owner row
     */
    public <OS, OQ extends FlexibleRelationalPathBase<OR>, OR> ExtensionContainerDeltaProcessor(
            SqaleUpdateContext<OS, OQ, OR> context,
            @NotNull ExtensionMapping<OQ, OR> mapping,
            @NotNull Function<OQ, JsonbPath> rootToExtensionPath) {
        super(context);
        this.mapping = mapping;
        this.jsonbPath = rootToExtensionPath.apply(context.entityPath());
    }

    /**
     * Sets the values for items in the PCV that are mapped to database columns and nulls the rest.
     */
    public void setValue(@NotNull T value) throws SchemaException {
        context.set(jsonbPath,
                new ExtensionProcessor(context.repositoryContext())
                        .processExtensions(value, mapping.holderType()));
    }

    @Override
    public void delete() {
        context.setNull(jsonbPath);
    }
}
