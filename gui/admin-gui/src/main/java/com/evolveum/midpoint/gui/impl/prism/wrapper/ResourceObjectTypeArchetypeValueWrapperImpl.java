/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.commons.lang3.StringUtils;

public class ResourceObjectTypeArchetypeValueWrapperImpl<T extends Referencable> extends PrismReferenceValueWrapperImpl<T> {

    public ResourceObjectTypeArchetypeValueWrapperImpl(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    @Override
    protected <O extends ObjectType> ObjectDetailsModels<O> createNewObjectModel(ContainerPanelConfigurationType config, PageBase pageBase) {
        ObjectDetailsModels<O> newObjectModel = super.createNewObjectModel(config, pageBase);
        try {
            PrismObjectWrapper<ObjectType> objectWrapper = getParent().findObjectWrapper();
            StringBuilder name = new StringBuilder(WebComponentUtil.getDisplayNameOrName(objectWrapper.getObject()));

            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> resourceObjectType =
                    getParent().getParentContainerValue(ResourceObjectTypeDefinitionType.class);
            if (resourceObjectType != null && StringUtils.isNotEmpty(resourceObjectType.getRealValue().getIntent())) {
                name.append(" (").append(resourceObjectType.getRealValue().getIntent()).append(")");
            }

            if (!name.isEmpty()) {
                PrismPropertyWrapper nameProperty = newObjectModel.getObjectWrapper().findProperty(ObjectType.F_NAME);
                nameProperty.getValue().setRealValue(new PolyString(name.toString()));
            }
        } catch (SchemaException e) {
            //ignore it and don't set initial name
        }
        return newObjectModel;
    }
}
