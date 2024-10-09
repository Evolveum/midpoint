/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.Metadata;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.helpers.mapper.Mapper;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * Contrary to other updaters, this class is concerned only with handling whole container deltas
 * for metadata (of object or assignment).
 */
class MetadataUpdate extends BaseUpdate {

    private final Metadata<?> metadataHolder;

    MetadataUpdate(RObject object, Metadata<?> metadataHolder, ItemDelta<?, ?> delta, UpdateContext ctx) {
        super(object, delta, ctx);
        this.metadataHolder = metadataHolder;
    }

    void handleWholeContainerDelta() {
        PrismValue value = isDelete() ? null : getSingleValue();

        MapperContext context = new MapperContext();
        context.setRepositoryContext(beans.createRepositoryContext());
        context.setDelta(delta);
        context.setOwner(metadataHolder);

        if (value != null) {
            beans.prismEntityMapper.mapPrismValue(value, Metadata.class, context);
        } else {
            // todo clean this up
            // we know that mapper supports mapping null value, but still this code smells
            Mapper mapper = beans.prismEntityMapper.getMapper(MetadataType.class, Metadata.class);
            //noinspection unchecked
            mapper.map(null, context);
        }
    }
}
