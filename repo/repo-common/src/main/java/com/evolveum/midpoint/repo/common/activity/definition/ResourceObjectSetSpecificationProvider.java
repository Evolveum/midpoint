/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedObjectsSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

/**
 * Work definition that can provide object set specification.
 */
public interface ResourceObjectSetSpecificationProvider extends FailedObjectsSelectorProvider {

    ResourceObjectSetType getResourceObjectSetSpecification();

    @Override
    default FailedObjectsSelectorType getFailedObjectsSelector() {
        ResourceObjectSetType set = getResourceObjectSetSpecification();
        return set != null ? set.getFailedObjectsSelector() : null;
    }
}
