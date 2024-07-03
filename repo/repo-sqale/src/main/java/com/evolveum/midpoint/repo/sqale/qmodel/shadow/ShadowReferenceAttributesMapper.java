package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaProcessor;
import com.evolveum.midpoint.repo.sqale.mapping.UpdatableItemSqlMapper;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.RightHandProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemValueFilterProcessor;

import com.querydsl.core.types.Expression;
import org.jetbrains.annotations.Nullable;

public class ShadowReferenceAttributesMapper implements UpdatableItemSqlMapper<QShadow, MShadow> {

    @Override
    public ItemDeltaProcessor createItemDeltaProcessor(SqaleUpdateContext<?, ?, ?> sqlUpdateContext) {
        return null;
    }

    @Override
    public @Nullable Expression<?> primaryPath(QShadow entityPath, ItemDefinition<?> definition) throws QueryException {
        return null;
    }

    @Override
    public @Nullable <T extends ValueFilter<?, ?>> ItemValueFilterProcessor<T> createFilterProcessor(SqlQueryContext<?, ?, ?> sqlQueryContext) {
        return null;
    }

    @Override
    public @Nullable RightHandProcessor createRightHandProcessor(SqlQueryContext<?, ?, ?> sqlQueryContext) {
        return null;
    }
}
