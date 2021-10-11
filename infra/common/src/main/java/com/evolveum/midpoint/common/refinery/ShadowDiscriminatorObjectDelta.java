/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;

/**
 *
 */
public interface ShadowDiscriminatorObjectDelta<T extends Objectable> extends ObjectDelta<T> {

    ResourceShadowDiscriminator getDiscriminator();

    void setDiscriminator(ResourceShadowDiscriminator discriminator);
}
