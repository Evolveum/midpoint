/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.commons.lang.StringUtils;

public class ConstructionValueWrapper extends PrismContainerValueWrapperImpl<ConstructionType> {

    private static final transient Trace LOGGER = TraceManager.getTrace(ConstructionValueWrapper.class);

    private PrismObject<ResourceType> resource;
    private transient RefinedResourceSchema resourceSchema;

    public ConstructionValueWrapper(PrismContainerWrapper<ConstructionType> parent, PrismContainerValue<ConstructionType> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }


    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public RefinedResourceSchema getResourceSchema() throws SchemaException {
        if (resourceSchema == null) {
            if (resource != null) {
                resourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
            }
        }

        return resourceSchema;
    }

    public ShadowKindType getKind() {
        ShadowKindType kind =  getNewValue().asContainerable().getKind();
        if (kind == null) {
            kind = ShadowKindType.ACCOUNT;
        }
        return kind;
    }

    public String getIntent() {
        String intent = getNewValue().asContainerable().getIntent();
        if (StringUtils.isBlank(intent)) {
            ObjectClassComplexTypeDefinition def;
            try {
                def = findDefaultObjectClassDefinition();
                if (def != null) {
                    intent = def.getIntent();
                }
            } catch (SchemaException e) {
                LOGGER.error("Cannot get default object class definition, {}", e.getMessage(), e);
                intent = "default";
            }

        }
        return intent;
    }

    private ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition() throws SchemaException{
        RefinedResourceSchema schema = getResourceSchema();
        if (schema == null) {
            return null;
        }

        return schema.findDefaultObjectClassDefinition(getKind());
    }

}
