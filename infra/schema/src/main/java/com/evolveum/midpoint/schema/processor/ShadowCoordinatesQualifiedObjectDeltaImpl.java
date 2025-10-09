/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.delta.ObjectDeltaImpl;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;

import java.io.Serial;

/**
 * An {@link ObjectDelta} enriched by {@link ResourceShadowCoordinates} (pointing to a resource object type).
 *
 * @author semancik
 */
public class ShadowCoordinatesQualifiedObjectDeltaImpl<T extends Objectable>
        extends ObjectDeltaImpl<T>
        implements ShadowCoordinatesQualifiedObjectDelta<T> {

    @Serial private static final long serialVersionUID = 1L;

    private ResourceShadowCoordinates coordinates;

    ShadowCoordinatesQualifiedObjectDeltaImpl(Class<T> objectTypeClass, ChangeType changeType) {
        super(objectTypeClass, changeType);
    }

    @Override
    public ResourceShadowCoordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(ResourceShadowCoordinates coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    protected void checkIdentifierConsistence(boolean requireOid) {
        if (requireOid && coordinates.getResourceOid() == null) {
            throw new IllegalStateException("Null resource oid in delta "+this);
        }
    }

    @Override
    protected String debugName() {
        return "ShadowCoordinatesQualifiedObjectDelta";
    }

    @Override
    protected String debugIdentifiers() {
        return coordinates == null ? "null" : coordinates.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ShadowCoordinatesQualifiedObjectDeltaImpl<T> clone() {
        ShadowCoordinatesQualifiedObjectDeltaImpl<T> clone = new ShadowCoordinatesQualifiedObjectDeltaImpl<>(
                this.getObjectTypeClass(), this.getChangeType());
        copyValues(clone);
        return clone;
    }

    private void copyValues(ShadowCoordinatesQualifiedObjectDeltaImpl<T> clone) {
        super.copyValues(clone);
        clone.coordinates = this.coordinates;
    }
}
