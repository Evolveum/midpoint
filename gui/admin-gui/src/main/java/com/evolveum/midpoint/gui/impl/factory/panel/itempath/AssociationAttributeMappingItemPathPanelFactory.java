/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.itempath;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author lskublik
 */
@Component
public class AssociationAttributeMappingItemPathPanelFactory extends AttributeMappingItemPathPanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationAttributeMappingItemPathPanelFactory.class);

    private static final long serialVersionUID = 1L;

    private static final List<ItemPath> KINDS_OF_MAPPING = Arrays.asList(
            ItemPath.create(
                    ShadowAssociationDefinitionType.F_INBOUND,
                    SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION),
            ItemPath.create(
                    ShadowAssociationDefinitionType.F_OUTBOUND,
                    SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION));

    @Autowired private transient GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!ItemPathType.COMPLEX_TYPE.equals(wrapper.getTypeName())) {
            return false;
        }

        for (ItemPath kindOfMappingPath : getKindsOfMapping()) {
            if (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
//                            ResourceType.F_SCHEMA_HANDLING,
//                            SchemaHandlingType.F_ASSOCIATION_TYPE,
//                            ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                            ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION)
                    kindOfMappingPath,
                    getItemNameForContainerOfAttributes(),
                    ResourceAttributeDefinitionType.F_REF))) {
                return true;
            }
        }

        return isVirtualPropertyOfMapping(wrapper);
    }

    private List<ItemPath> getKindsOfMapping() {
        return KINDS_OF_MAPPING;
    }

    protected ItemName getItemNameForContainerOfAttributes() {
        return AssociationSynchronizationExpressionEvaluatorType.F_ATTRIBUTE;
    }

    private <IW extends ItemWrapper<?, ?>> boolean isVirtualPropertyOfMapping(IW wrapper) {
        return QNameUtil.match(wrapper.getItemName(), ResourceAttributeDefinitionType.F_REF)
                && (wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
//                ResourceType.F_SCHEMA_HANDLING,
//                SchemaHandlingType.F_ASSOCIATION_TYPE,
//                ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
//                ShadowAssociationDefinitionType.F_INBOUND,
                SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION,
                getItemNameForContainerOfAttributes(),
                AttributeInboundMappingsDefinitionType.F_MAPPING))
                || wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
//                ResourceType.F_SCHEMA_HANDLING,
//                SchemaHandlingType.F_ASSOCIATION_TYPE,
//                ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
//                ShadowAssociationDefinitionType.F_OUTBOUND,
                SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION,
                getItemNameForContainerOfAttributes(),
                AttributeOutboundMappingsDefinitionType.F_MAPPING)));
    }

    @Override
    protected List<DisplayableValue<ItemPathType>> getAttributes(ResourceSchema schema, PrismValueWrapper<ItemPathType> propertyWrapper) {
        ShadowAssociationDefinition assocDef = AssociationChildWrapperUtil.getShadowAssociationDefinition(schema, propertyWrapper);
        if (assocDef == null) {
            return Collections.emptyList();
        }

        List<DisplayableValue<ItemPathType>> attributes = new ArrayList<>();
        if (!assocDef.isComplex()){
            return attributes;
        }
        assocDef.getAssociationDataObjectDefinition().getSimpleAttributeDefinitions()
                .forEach(attr -> attributes.add(createDisplayValue(attr)));

        return attributes;
    }

    @Override
    public Integer getOrder() {
        return 990;
    }
}
