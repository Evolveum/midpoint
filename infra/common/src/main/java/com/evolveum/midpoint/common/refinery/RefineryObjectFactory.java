/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.impl.delta.ObjectDeltaFactoryImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 *
 */
public class RefineryObjectFactory {

    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method.
     */
    @SafeVarargs
    public static <O extends Objectable, X> ShadowDiscriminatorObjectDelta<O> createShadowDiscriminatorModificationReplaceProperty(Class<O> type,
            String resourceOid, ShadowKindType kind, String intent, ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
        ShadowDiscriminatorObjectDelta<O> objectDelta = new ShadowDiscriminatorObjectDeltaImpl<>(type, ChangeType.MODIFY, prismContext);
        objectDelta.setDiscriminator(new ResourceShadowDiscriminator(resourceOid, kind, intent, null, false));
        ObjectDeltaFactoryImpl.fillInModificationReplaceProperty(objectDelta, propertyPath, propertyValues);
        return objectDelta;
    }
}
