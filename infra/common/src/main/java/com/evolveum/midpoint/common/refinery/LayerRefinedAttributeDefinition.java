/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;

/**
 * @author mederly
 */
public interface LayerRefinedAttributeDefinition<T> extends RefinedAttributeDefinition<T> {
    LayerType getLayer();

    Boolean getOverrideCanRead();

    Boolean getOverrideCanAdd();

    Boolean getOverrideCanModify();

    PropertyLimitations getLimitations();
}
