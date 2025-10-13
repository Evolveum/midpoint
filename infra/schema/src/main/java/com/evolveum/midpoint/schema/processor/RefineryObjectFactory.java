/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.impl.delta.ObjectDeltaFactoryImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
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
    public static <O extends Objectable, X> ShadowCoordinatesQualifiedObjectDelta<O> createShadowDiscriminatorModificationReplaceProperty(Class<O> type,
            String resourceOid, ShadowKindType kind, String intent, ItemPath propertyPath, X... propertyValues) {
        ShadowCoordinatesQualifiedObjectDelta<O> objectDelta = new ShadowCoordinatesQualifiedObjectDeltaImpl<>(type, ChangeType.MODIFY);
        objectDelta.setCoordinates(new ResourceShadowCoordinates(resourceOid, kind, intent, null));
        ObjectDeltaFactoryImpl.fillInModificationReplaceProperty(objectDelta, propertyPath, propertyValues);
        return objectDelta;
    }
}
