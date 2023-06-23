/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
public class ResourceAttributeMappingValueWrapper extends PrismContainerValueWrapperImpl<ResourceAttributeDefinitionType>{

    private static final long serialVersionUID = 1L;

    private List<MappingDirection> attributeMappingTypes = new ArrayList<>();

    public ResourceAttributeMappingValueWrapper(
            PrismContainerWrapper<ResourceAttributeDefinitionType> parent,
            PrismContainerValue<ResourceAttributeDefinitionType> pcv,
            ValueStatus status) {
        super(parent, pcv, status);
    }

    public void addAttributeMappingType(MappingDirection valueType) {
        if (!attributeMappingTypes.contains(valueType)) {
            attributeMappingTypes.add(valueType);
        }
    }

    public List<MappingDirection> getAttributeMappingTypes() {
        return attributeMappingTypes;
    }
}
