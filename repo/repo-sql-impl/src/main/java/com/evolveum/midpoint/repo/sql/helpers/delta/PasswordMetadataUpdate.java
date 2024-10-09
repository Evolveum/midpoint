/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.RFocus;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Handles credentials/password/metadata updates.
 */
class PasswordMetadataUpdate extends BaseUpdate {

    PasswordMetadataUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx) {
        super(object, delta, ctx);
    }

    public void handlePropertyDelta() throws SchemaException {
        if (!(object instanceof RFocus)) {
            throw new SystemException("Bean is not instance of " + RFocus.class + ", shouldn't happen");
        }

        RFocus focus = (RFocus) object;
        if (isDelete()) {
            focus.setPasswordCreateTimestamp(null);
            focus.setModifyTimestamp(null);
            return;
        }

        PrismValue value = getSingleValue();

        MapperContext context = new MapperContext();
        context.setRepositoryContext(beans.createRepositoryContext());
        context.setDelta(delta);
        context.setOwner(object);
        beans.prismEntityMapper.mapPrismValue(value, RFocus.class, context);
    }
}
