/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.util.exception.ConfigurationException;

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

import java.util.Objects;

public class ConstructionValueWrapper extends PrismContainerValueWrapperImpl<ConstructionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionValueWrapper.class);

    private String resourceOid;
//    private transient ResourceSchema refinedSchema;

    public ConstructionValueWrapper(PrismContainerWrapper<ConstructionType> parent, PrismContainerValue<ConstructionType> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public ResourceSchema getRefinedSchema(PrismObject<ResourceType> resource) throws SchemaException, ConfigurationException {
        if (resource != null) {
            return ResourceSchemaFactory.getCompleteSchema(resource);
        }
        return null;
    }

    public ShadowKindType getKind() {
        ShadowKindType kind = getNewValue().asContainerable().getKind();
        return Objects.requireNonNullElse(kind, ShadowKindType.ACCOUNT);
    }

    public String getIntent() {
        return getNewValue().asContainerable().getIntent();
    }

    /**
     * The difference to {@link #getIntent()} is that this method tries to guess the intent by looking at default
     * type definition for given shadow kind.
     */
    public String determineIntent(PrismObject<ResourceType> resource) {
        String specifiedIntent = getIntent();
        if (StringUtils.isNotBlank(specifiedIntent)) {
            return specifiedIntent;
        }

        ResourceObjectDefinition def;
        try {
            def = findDefaultDefinitionForTheKind(resource);
            if (def instanceof ResourceObjectTypeDefinition) {
                return ((ResourceObjectTypeDefinition) def).getIntent();
            } else {
                return null;
            }
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Cannot get default object definition, {}", e.getMessage(), e);
            return SchemaConstants.INTENT_DEFAULT; // TODO is this OK?
        }
    }

    private ResourceObjectDefinition findDefaultDefinitionForTheKind(PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException {
        ResourceSchema schema = getRefinedSchema(resource);
        if (schema == null) {
            return null;
        }

        return schema.findDefaultDefinitionForKind(getKind());
    }

    public ResourceObjectDefinition getResourceObjectDefinition(PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException {
        ResourceSchema schema = getRefinedSchema(resource);
        if (schema == null) {
            return null;
        }
        ShadowKindType kind = getKind();
        String intent = getIntent();
        if (intent != null) {
            return schema.findObjectDefinition(kind, intent);
        } else {
            return schema.findDefaultDefinitionForKind(kind);
        }
    }
}
