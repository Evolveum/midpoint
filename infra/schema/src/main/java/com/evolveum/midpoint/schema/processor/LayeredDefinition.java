/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;

import org.jetbrains.annotations.NotNull;

/**
 * A definition that can be viewed at from different layer's point of view.
 *
 * - see {@link ShadowSimpleAttributeDefinition#getLimitations(LayerType)} and similar layer-qualified methods
 * - see {@link ShadowItemLayeredDefinition}
 */
public interface LayeredDefinition {

    /**
     * Gets the current point-of-view: on which layer do we look at the data?
     */
    @NotNull LayerType getCurrentLayer();
}
