/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;

import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ConstructionValueWrapper extends PrismContainerValueWrapperImpl<ConstructionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionValueWrapper.class);

    private PrismObject<ResourceType> resource;
    private transient ResourceSchema refinedSchema;

    public ConstructionValueWrapper(PrismContainerWrapper<ConstructionType> parent, PrismContainerValue<ConstructionType> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public ResourceSchema getRefinedSchema() throws SchemaException {
        if (refinedSchema == null) {
            if (resource != null) {
                refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
            }
        }

        return refinedSchema;
    }

    public ShadowKindType getKind() {
        ShadowKindType kind = getNewValue().asContainerable().getKind();
        if (kind == null) {
            kind = ShadowKindType.ACCOUNT;
        }
        return kind;
    }

    public String getIntent() {
        String intent = getNewValue().asContainerable().getIntent();
        if (StringUtils.isBlank(intent)) {
            ResourceObjectDefinition def;
            try {
                def = findDefaultObjectClassDefinition();
                if (def instanceof ResourceObjectTypeDefinition) {
                    intent = ((ResourceObjectTypeDefinition) def).getIntent();
                }
            } catch (SchemaException e) {
                LOGGER.error("Cannot get default object class definition, {}", e.getMessage(), e);
                intent = "default";
            }

        }
        return intent;
    }

    private ResourceObjectDefinition findDefaultObjectClassDefinition() throws SchemaException {
        ResourceSchema schema = getRefinedSchema();
        if (schema == null) {
            return null;
        }

        return schema.findObjectDefinition(getKind(), null);
    }

}
