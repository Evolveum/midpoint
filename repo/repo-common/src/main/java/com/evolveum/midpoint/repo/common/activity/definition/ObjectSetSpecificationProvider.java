/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedObjectsSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;

import org.jetbrains.annotations.NotNull;

/**
 * Work definition that can provide object set specification.
 */
public interface ObjectSetSpecificationProvider extends FailedObjectsSelectorProvider {

    @NotNull ObjectSetType getObjectSetSpecification();

    @Override
    default FailedObjectsSelectorType getFailedObjectsSelector() {
        return getObjectSetSpecification().getFailedObjectsSelector();
    }
}
