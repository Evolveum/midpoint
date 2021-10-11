/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.impl.delta.ObjectDeltaImpl;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;

/**
 * @author semancik
 *
 */
public class ShadowDiscriminatorObjectDeltaImpl<T extends Objectable> extends ObjectDeltaImpl<T> implements ShadowDiscriminatorObjectDelta<T> {
    private static final long serialVersionUID = 1L;

    private ResourceShadowDiscriminator discriminator;

    ShadowDiscriminatorObjectDeltaImpl(Class<T> objectTypeClass, ChangeType changeType, PrismContext prismContext) {
        super(objectTypeClass, changeType, prismContext);
    }

    @Override
    public ResourceShadowDiscriminator getDiscriminator() {
        return discriminator;
    }

    @Override
    public void setDiscriminator(ResourceShadowDiscriminator discriminator) {
        this.discriminator = discriminator;
    }

    @Override
    protected void checkIdentifierConsistence(boolean requireOid) {
        if (requireOid && discriminator.getResourceOid() == null) {
            throw new IllegalStateException("Null resource oid in delta "+this);
        }
    }

    @Override
    protected String debugName() {
        return "ShadowDiscriminatorObjectDelta";
    }

    @Override
    protected String debugIdentifiers() {
        return discriminator == null ? "null" : discriminator.toString();
    }

    @Override
    public ShadowDiscriminatorObjectDeltaImpl<T> clone() {
        ShadowDiscriminatorObjectDeltaImpl<T> clone = new ShadowDiscriminatorObjectDeltaImpl<>(this.getObjectTypeClass(), this.getChangeType(), this.getPrismContext());
        copyValues(clone);
        return clone;
    }

    protected void copyValues(ShadowDiscriminatorObjectDeltaImpl<T> clone) {
        super.copyValues(clone);
        clone.discriminator = this.discriminator;
    }
}
