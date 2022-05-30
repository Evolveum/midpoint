/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;

/**
 * An {@link ObjectDelta} enriched by {@link ResourceShadowCoordinates} (pointing to a resource object type).
 *
 * Currently used only in tests. Consider removal.
 */
public interface ShadowCoordinatesQualifiedObjectDelta<T extends Objectable>
        extends ObjectDelta<T> {

    ResourceShadowCoordinates getCoordinates();

    void setCoordinates(ResourceShadowCoordinates coordinates);
}
