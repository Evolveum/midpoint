/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

/**
 * @author skublik
 *
 * Handles general (the most common) case of item delta application.
 * Tries to find appropriate R-class member by whole attribute item path and update its value,
 * causing respective DB update to be scheduled by Hibernate.
 */

public class OverrideItemPathAttributeUpdate extends GeneralUpdate{

    <T extends ObjectType> OverrideItemPathAttributeUpdate(RObject object, ItemDelta<?, ?> delta, PrismObject<T> prismObject, ManagedType<T> mainEntityType, UpdateContext ctx) {
        super(object, delta, prismObject, mainEntityType, ctx);
    }

    @Override
    protected Attribute findAttributeForCurrentState() {
        return findAttributePathOverrideIfExists();
    }
}
