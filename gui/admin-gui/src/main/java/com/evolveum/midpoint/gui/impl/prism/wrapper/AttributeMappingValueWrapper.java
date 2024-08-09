/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
public class AttributeMappingValueWrapper<C extends Containerable> extends PrismContainerValueWrapperImpl<C>{

    private static final long serialVersionUID = 1L;

    private List<MappingDirection> attributeMappingTypes = new ArrayList<>();

    public AttributeMappingValueWrapper(
            PrismContainerWrapper<C> parent,
            PrismContainerValue<C> pcv,
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
