/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.range;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Tells the panels which ranges make sense for a mapping and which one to start with.
 *
 *
 * @author jjarabinec
 */
public class MappingRangeUtils {

    private static final Trace LOGGER = TraceManager.getTrace(MappingRangeUtils.class);

    /**
     * Options to offer for the mapping. Matching the provenance needs a midPoint property to read the
     * provenance from, so it is offered for a multi valued target only.
     *
     * @param mappingValue value of the mapping.
     * @return options to offer, in the order they are shown.
     */
    public static List<MappingRangeOption> optionsFor(PrismContainerValueWrapper<MappingType> mappingValue) {
        boolean multiValueTarget = isMultiValueTarget(mappingValue);

        List<MappingRangeOption> options = new ArrayList<>();
        options.add(MappingRangeOption.ALL);
        if (multiValueTarget) {
            options.add(MappingRangeOption.MATCHING_PROVENANCE);
        }
        if (multiValueTarget || isMultiValueAttribute(mappingValue)) {
            options.add(MappingRangeOption.CONDITION);
        }
        options.add(MappingRangeOption.NONE);
        return options;
    }

    /**
     * Makes the range of a new mapping explicit, so that the administrator sees what the mapping is
     * in charge of instead of relying on what midPoint assumes.
     *
     * @param mappingValue value of the new mapping.
     */
    public static void initializeRange(PrismContainerValueWrapper<MappingType> mappingValue) {
        MappingRangeModel rangeModel = new MappingRangeModel(() -> mappingValue);
        if (rangeModel.getObject() == null) {
            rangeModel.setObject(defaultRange(mappingValue));
        }
    }

    /**
     * Range a mapping should be created with. Matching the provenance is preferred wherever it can be
     * used, everything else is left in charge of all the values.
     *
     * @param mappingValue value of the mapping.
     * @return the range to store, never null.
     */
    public static ValueSetDefinitionType defaultRange(PrismContainerValueWrapper<MappingType> mappingValue) {
        MappingRangeOption option = isMultiValueTarget(mappingValue)
                ? MappingRangeOption.MATCHING_PROVENANCE
                : MappingRangeOption.ALL;

        return new ValueSetDefinitionType().predefined(option.getPredefined());
    }

    /**
     * Tells whether the target of the mapping can hold more than one value.
     *
     * @param mappingValue value of the mapping.
     * @return true when the target may hold more than one value.
     */
    public static boolean isMultiValueTarget(PrismContainerValueWrapper<MappingType> mappingValue) {
        ItemPath targetPath = getTargetPath(mappingValue);
        if (targetPath == null) {
            return false;
        }

        PrismContainerDefinition<? extends Containerable> focusDefinition = findFocusDefinition(mappingValue);
        if (focusDefinition == null) {
            return true;
        }

        ItemDefinition<?> targetDefinition = focusDefinition.findItemDefinition(targetPath);
        return targetDefinition == null || targetDefinition.isMultiValue();
    }

    /**
     * Tells whether the resource attribute the mapping is written under can hold more than one value.
     *
     * @param mappingValue value of the mapping.
     * @return true when the attribute may hold more than one value.
     */
    public static boolean isMultiValueAttribute(PrismContainerValueWrapper<MappingType> mappingValue) {
        ItemPath attributePath = getAttributePath(mappingValue);
        if (attributePath == null) {
            return false;
        }

        ResourceSchema schema = findResourceSchema(mappingValue);
        if (schema == null) {
            return true;
        }

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectTypeValue =
                mappingValue.getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        for (ShadowAttributeDefinition attribute : WebPrismUtil.searchAttributeDefinitions(
                schema, objectTypeValue != null ? objectTypeValue.getRealValue() : null)) {
            if (attributePath.equivalent(attribute.getItemName())) {
                return attribute.isMultiValue();
            }
        }
        return true;
    }

    private static ItemPath getAttributePath(PrismContainerValueWrapper<MappingType> mappingValue) {
        if (mappingValue == null) {
            return null;
        }

        PrismContainerValueWrapper<ResourceAttributeDefinitionType> attributeValue =
                mappingValue.getParentContainerValue(ResourceAttributeDefinitionType.class);
        ResourceAttributeDefinitionType attribute =
                attributeValue != null ? attributeValue.getRealValue() : null;
        ItemPathType ref = attribute != null ? attribute.getRef() : null;

        return ref != null ? ref.getItemPath() : null;
    }

    private static ResourceSchema findResourceSchema(PrismContainerValueWrapper<MappingType> mappingValue) {
        try {
            Object object = mappingValue.getParent().findObjectWrapper().getObjectOld().asObjectable();
            if (!(object instanceof ResourceType resource)) {
                return null;
            }
            return ResourceSchemaFactory.getCompleteSchema(resource);
        } catch (Exception ex) {
            LOGGER.debug("Couldn't get the schema of the resource of the mapping: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private static ItemPath getTargetPath(PrismContainerValueWrapper<MappingType> mappingValue) {
        MappingType mapping = mappingValue != null ? mappingValue.getRealValue() : null;
        VariableBindingDefinitionType target = mapping != null ? mapping.getTarget() : null;
        ItemPathType path = target != null ? target.getPath() : null;
        if (path == null) {
            return null;
        }

        ItemPath itemPath = path.getItemPath().stripVariableSegment();
        return itemPath.isEmpty() ? null : itemPath;
    }

    private static PrismContainerDefinition<? extends Containerable> findFocusDefinition(
            PrismContainerValueWrapper<MappingType> mappingValue) {
        if (mappingValue == null) {
            return null;
        }

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectTypeValue =
                mappingValue.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        ResourceObjectTypeDefinitionType objectType =
                objectTypeValue != null ? objectTypeValue.getRealValue() : null;

        QName focusType = objectType != null && objectType.getFocus() != null
                ? objectType.getFocus().getType()
                : null;

        return PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByType(focusType != null ? focusType : UserType.COMPLEX_TYPE);
    }
}
