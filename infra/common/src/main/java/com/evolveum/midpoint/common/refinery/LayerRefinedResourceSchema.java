/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface LayerRefinedResourceSchema extends RefinedResourceSchema {
    LayerType getLayer();

    LayerRefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, ShadowType shadow);

    LayerRefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, String intent);

    LayerRefinedObjectClassDefinition getRefinedDefinition(QName typeName);

    LayerRefinedObjectClassDefinition getDefaultRefinedDefinition(ShadowKindType kind);
}
