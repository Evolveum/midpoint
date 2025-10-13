/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Object filter that passes all shadows.
 */
public class NullPostSearchFilterImpl implements PostSearchFilter {

    @Override
    public boolean matches(@NotNull PrismObject<ShadowType> shadow) {
        return true;
    }
}
