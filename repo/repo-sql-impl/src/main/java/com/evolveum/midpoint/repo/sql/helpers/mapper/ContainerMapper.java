/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;

import java.util.Collection;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ContainerMapper<I extends Containerable, O extends Container> implements Mapper<I, O> {

    @Deprecated
    protected void lookForContainerIdInOldValues(O output, MapperContext context) {
        if (output == null || output.getId() != null) {
            return;
        }

        ItemDelta delta = context.getDelta();
        if (delta == null) {
            return;
        }

        Collection oldValues = delta.getEstimatedOldValues();
        if (oldValues == null) {
            return;
        }

        for (Object object : oldValues) {
            PrismContainerValue val = null;
            if (object instanceof Containerable) {
                Containerable c = (Containerable)object;
                val = c.asPrismContainerValue();
            } else if (object instanceof PrismContainerValue) {
                val = (PrismContainerValue) object;
            }

            if (val != null && val.getId() != null) {
                Long id = val.getId();
                output.setId(id.intValue());
                break;
            }
        }
    }
}
