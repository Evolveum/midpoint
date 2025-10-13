/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Delta processor for multi-value poly-strings represented as array in JSONB column.
 */
public class JsonbPolysItemDeltaProcessor extends FinalValueDeltaProcessor<PolyString> {

    private final JsonbPath path;

    /**
     * @param <Q> entity query type from which the attribute is resolved
     * @param <R> row type related to {@link Q}
     */
    public <Q extends FlexibleRelationalPathBase<R>, R> JsonbPolysItemDeltaProcessor(
            SqaleUpdateContext<?, Q, R> context,
            Function<Q, JsonbPath> rootToQueryItem) {
        super(context);
        this.path = rootToQueryItem.apply(context.entityPath());
    }

    @Override
    public void setRealValues(Collection<?> values) {
        //noinspection unchecked
        context.set(path, JsonbUtils.polyStringsToJsonb((Collection<PolyString>) values));
    }

    @Override
    public void delete() {
        context.setNull(path);
    }
}
