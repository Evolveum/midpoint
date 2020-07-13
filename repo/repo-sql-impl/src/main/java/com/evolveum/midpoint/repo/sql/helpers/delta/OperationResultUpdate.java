/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.OperationResult;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.helpers.mapper.Mapper;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * Handles operation result updates.
 */
class OperationResultUpdate extends BaseUpdate {

    OperationResultUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx) {
        super(object, delta, ctx);
    }

    void handleItemDelta() {
        if (!(object instanceof OperationResult)) {
            throw new SystemException("Bean is not instance of " + OperationResult.class + ", shouldn't happen");
        }

        PrismValue value;
        if (delta.isDelete()) {
            value = null;
        } else {
            value = delta.getAnyValue();
        }

        MapperContext context = new MapperContext();
        context.setRepositoryContext(beans.createRepositoryContext());
        context.setDelta(delta);
        context.setOwner(object);

        if (value != null) {
            beans.prismEntityMapper.mapPrismValue(value, OperationResult.class, context);
        } else {
            // todo clean this up
            // we know that mapper supports mapping null value, but still this code smells
            Mapper mapper = beans.prismEntityMapper.getMapper(OperationResultType.class, OperationResult.class);
            //noinspection unchecked
            mapper.map(null, context);
        }
    }

}
