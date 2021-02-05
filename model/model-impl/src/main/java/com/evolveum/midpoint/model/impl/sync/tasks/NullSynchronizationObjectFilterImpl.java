/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Object filter that passes all shadows.
 */
public class NullSynchronizationObjectFilterImpl implements SynchronizationObjectsFilter {

    @Override
    public boolean matches(@NotNull PrismObject<ShadowType> shadow) {
        return true;
    }
}
