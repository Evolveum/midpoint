package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerWithFullObject;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.querydsl.core.types.Predicate;

import java.util.List;
import java.util.UUID;

public interface QSeparatelySerializedItem<Q extends FlexibleRelationalPathBase<R>, R> {
    Predicate allOwnedBy(Q q, List<UUID> oidList);
    ItemPath getItemPath();

    Q createAlias();

    boolean hasFullObject(R row);

    UUID getOwner(R row);
    PrismValue toSchemaObjectEmbedded(R row) throws SchemaException;
    Class<? extends Item<? extends PrismValue,?>> getPrismItemType();
}
