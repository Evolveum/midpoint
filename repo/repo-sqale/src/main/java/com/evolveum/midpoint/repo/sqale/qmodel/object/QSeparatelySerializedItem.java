package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerWithFullObject;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QSeparatelySerializedItem<Q extends FlexibleRelationalPathBase<R>, R> {
    Predicate allOwnedBy(Q q, Collection<UUID> oidList);
    ItemPath getItemPath();

    Q createAlias();

    boolean hasFullObject(Tuple row, Q path);

    UUID getOwner(Tuple row, Q path);
    PrismValue toSchemaObjectEmbedded(Tuple row, Q base) throws SchemaException;
    Class<? extends Item<? extends PrismValue,?>> getPrismItemType();
    String tableName();
    Path<?>[] fullObjectExpressions(Q base);

    OrderSpecifier<?> orderSpecifier(Q q);
}
