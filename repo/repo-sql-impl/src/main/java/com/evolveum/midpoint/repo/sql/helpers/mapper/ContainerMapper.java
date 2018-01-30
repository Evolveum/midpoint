/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    // todo remove this, we have to do full equal comparation in case of id=null
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
