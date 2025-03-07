/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller.transformer;

import com.evolveum.midpoint.model.impl.controller.SchemaTransformer;
import com.evolveum.midpoint.model.impl.schema.transform.TransformableItemDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.ComplexTypeDefinition.ComplexTypeDefinitionMutator;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DefinitionUpdateOption;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ItemRefinedDefinitionTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType.AUTOMATIC;

/**
 * Parts of {@link SchemaTransformer} devoted to modifying prism data and definitions according to policies
 * in object templates or archetypes.
 */
@Component
public class DataPolicyProcessor {

    /** Using this logger for compatibility reasons. */
    private static final Trace LOGGER = TraceManager.getTrace(SchemaTransformer.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    public <O extends ObjectType> void applyObjectTemplateToDefinition(
            PrismObjectDefinition<O> objectDefinition, ObjectTemplateType objectTemplate, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        if (objectTemplate == null) {
            return;
        }
        if (!SimulationUtil.isVisible(objectTemplate, task.getExecutionMode())) {
            LOGGER.trace("Ignoring template {} as it is not visible for the current task", objectTemplate);
            return;
        }
        for (ObjectReferenceType includeRef: objectTemplate.getIncludeRef()) {
            PrismObject<ObjectTemplateType> subTemplate = repositoryService.getObject(
                    ObjectTemplateType.class, includeRef.getOid(), readOnly(), result);
            applyObjectTemplateToDefinition(objectDefinition, subTemplate.asObjectable(), task, result);
        }
        for (ObjectTemplateItemDefinitionType templateItemDef: objectTemplate.getItem()) {
            ItemPath itemPath = ItemRefinedDefinitionTypeUtil.getRef(templateItemDef);
            ItemDefinition<?> itemDef = objectDefinition.findItemDefinition(itemPath);
            if (itemDef != null) {
                applyObjectTemplateItem(itemDef, templateItemDef, "item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplate);
            } else {
                OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToDefinition");
                subResult.recordPartialError("No definition for item " + itemPath + " in object type " + objectDefinition.getTypeName() + " as specified in item definition in " + objectTemplate);
            }
        }
    }

    public <O extends ObjectType> void applyObjectTemplateToObject(
            PrismObject<O> object, ObjectTemplateType objectTemplate, DefinitionUpdateOption option,
            Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        if (objectTemplate == null) {
            return;
        }
        if (!SimulationUtil.isVisible(objectTemplate, task.getExecutionMode())) {
            LOGGER.trace("Ignoring template {} as it is not visible for the current task", objectTemplate);
            return;
        }
        for (ObjectReferenceType includeRef: objectTemplate.getIncludeRef()) {
            PrismObject<ObjectTemplateType> subTemplate = repositoryService.getObject(
                    ObjectTemplateType.class, includeRef.getOid(), readOnly(), result);
            applyObjectTemplateToObject(object, subTemplate.asObjectable(), option, task, result);
        }
        for (ObjectTemplateItemDefinitionType templateItemDef: objectTemplate.getItem()) {
            ItemPath itemPath = ItemRefinedDefinitionTypeUtil.getRef(templateItemDef);
            ItemDefinition<?> itemDefFromObject = object.getDefinition().findItemDefinition(itemPath);
            if (itemDefFromObject != null) {
                applyObjectTemplateItem(itemDefFromObject, templateItemDef, "item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplate);
            } else {
                OperationResult subResult = result.createMinorSubresult(SchemaTransformer.class.getName() + ".applyObjectTemplateToObject");
                subResult.recordPartialError("No definition for item " + itemPath + " in " + object
                        + " as specified in item definition in " + objectTemplate);
                continue;
            }
            if (option == DefinitionUpdateOption.DEEP) {
                Collection<Item<?, ?>> items = object.getAllItems(itemPath);
                for (Item<?, ?> item : items) {
                    ItemDefinition<?> itemDef = item.getDefinition();
                    if (itemDef != itemDefFromObject) {
                        applyObjectTemplateItem(
                                itemDef,
                                templateItemDef,
                                "item " + itemPath + " in " + object +
                                        " as specified in item definition in " + objectTemplate);
                    }
                }
            }
        }
    }

    private <ID extends ItemDefinition<?>> void applyObjectTemplateItem(
            ID itemDef, ObjectTemplateItemDefinitionType templateItemDefType, String desc) throws SchemaException {
        if (itemDef == null) {
            throw new SchemaException("No definition for "+desc);
        }

        TransformableItemDefinition<?,?> mutableDef = TransformableItemDefinition.access(itemDef);

        mutableDef.applyTemplate(templateItemDefType);

        List<PropertyLimitationsType> limitations = templateItemDefType.getLimitations();
        if (limitations != null) {
            // TODO review as part of MID-7929 resolution
            PropertyLimitationsType limitationsType = MiscSchemaUtil.getLimitationsLabeled(limitations, LayerType.PRESENTATION);
            if (limitationsType != null) {
                if (limitationsType.getMinOccurs() != null) {
                    mutableDef.setMinOccurs(XsdTypeMapper.multiplicityToInteger(limitationsType.getMinOccurs()));
                }
                if (limitationsType.getMaxOccurs() != null) {
                    mutableDef.setMaxOccurs(XsdTypeMapper.multiplicityToInteger(limitationsType.getMaxOccurs()));
                }
                if (limitationsType.getProcessing() != null) {
                    mutableDef.setProcessing(MiscSchemaUtil.toItemProcessing(limitationsType.getProcessing()));
                }
                PropertyAccessType accessType = limitationsType.getAccess();
                if (accessType != null) {
                    if (accessType.isAdd() != null) {
                        mutableDef.setCanAdd(accessType.isAdd());
                    }
                    if (accessType.isModify() != null) {
                        mutableDef.setCanModify(accessType.isModify());
                    }
                    if (accessType.isRead() != null) {
                        mutableDef.setCanRead(accessType.isRead());
                    }
                }
            }
        }

        ObjectReferenceType valueEnumerationRef = templateItemDefType.getValueEnumerationRef();
        if (valueEnumerationRef != null) {
            PrismReferenceValue valueEnumerationRVal = MiscSchemaUtil.objectReferenceTypeToReferenceValue(valueEnumerationRef, prismContext);
            mutableDef.setValueEnumerationRef(valueEnumerationRVal);
        }

        FormItemValidationType templateValidation = templateItemDefType.getValidation();
        if (templateValidation != null) {
            itemDef.mutator().setAnnotation(ItemRefinedDefinitionType.F_VALIDATION, templateValidation.clone());
        }
    }

    public <O extends ObjectType> void applyItemsConstraints(
            @NotNull PrismContainerDefinition<O> objectDefinition,
            @NotNull ArchetypePolicyType archetypePolicy) throws SchemaException {
        List<VisibilityPolicyEntry> visibilityPolicy = getVisibilityPolicy(archetypePolicy, objectDefinition);
        if (!visibilityPolicy.isEmpty()) {
            // UniformItemPath.EMPTY_PATH is null here. WHY?!?
            reduceItems(objectDefinition, prismContext.emptyPath(), visibilityPolicy);
        }
    }

    @NotNull
    private List<VisibilityPolicyEntry> getVisibilityPolicy(
            ArchetypePolicyType archetypePolicy, Object contextDesc) throws SchemaException {
        List<VisibilityPolicyEntry> visibilityPolicy = new ArrayList<>();
        for (ItemConstraintType itemConstraint: archetypePolicy.getItemConstraint()) {
            UserInterfaceElementVisibilityType visibility = itemConstraint.getVisibility();
            if (visibility != null) {
                ItemPathType itemPathType = itemConstraint.getPath();
                if (itemPathType == null) {
                    throw new SchemaException("No 'path' in item definition in archetype policy for " + contextDesc);
                }
                UniformItemPath itemPath = prismContext.toUniformPath(itemPathType);
                visibilityPolicy.add(new VisibilityPolicyEntry(itemPath, visibility));
            }
        }
        return visibilityPolicy;
    }

    @NotNull
    private UserInterfaceElementVisibilityType reduceItems(PrismContainerDefinition<?> containerDefinition,
            UniformItemPath containerPath, List<VisibilityPolicyEntry> visibilityPolicy) {
        UserInterfaceElementVisibilityType containerVisibility = determineVisibility(visibilityPolicy, containerPath);
        if (containerDefinition.isElaborate()) {
            return containerVisibility;
        }

        Collection<ItemName> itemsToDelete;
        if (containerVisibility == HIDDEN) {
            // Delete everything
            itemsToDelete = containerDefinition.getItemNames();
        } else {
            // Use item visibility to select individual items
            itemsToDelete = selectItemsToDelete(containerDefinition, containerPath, visibilityPolicy);
        }
        ComplexTypeDefinitionMutator mutableContainerCtDef = containerDefinition.getComplexTypeDefinition().mutator();
        for (ItemName itemName : itemsToDelete) {
            LOGGER.trace("Removing item {}/{} due to visibility constraint", containerPath, itemName.getLocalPart());
            mutableContainerCtDef.delete(itemName);
        }
        return containerVisibility;
    }

    @NotNull
    private List<ItemName> selectItemsToDelete(PrismContainerDefinition<?> containerDefinition,
            UniformItemPath containerPath, List<VisibilityPolicyEntry> visibilityPolicy) {
        List<ItemName> itemsToDelete = new ArrayList<>();
        for (ItemDefinition<?> subDefinition : containerDefinition.getDefinitions()) {
            UniformItemPath itemPath = containerPath.append(subDefinition.getItemName());
            if (subDefinition instanceof PrismContainerDefinition<?> subContainerDef) {
                UserInterfaceElementVisibilityType itemVisibility = reduceItems(subContainerDef, itemPath, visibilityPolicy);
                if (subContainerDef.isEmpty()) {
                    /*
                     * Empty sub-containers are treated in this way:
                     * - "completely defined" ones (no xsd:any) are hidden, unless explicitly set
                     *    to VISIBLE i.e. if VACANT, HIDDEN, or AUTOMATIC
                     * - "open" ones (xsd:any) are dealt with just like properties: hidden if VACANT or HIDDEN
                     *
                     * Primary motivation for this behavior is the fact that we need to keep assignment/extension definition
                     * in the object. It is required for normal model operation, specifically for the construction of "magic
                     * assignment".
                     *
                     * Note that this somehow mixes presentation requirements (hiding/showing items) with the requirements of
                     * business logic. This is because the current solution is a temporary one, to be replaced by something
                     * more serious.
                     */
                    if (itemVisibility == VACANT || itemVisibility == HIDDEN ||
                            itemVisibility == AUTOMATIC && subContainerDef.isCompletelyDefined()) {
                        itemsToDelete.add(subDefinition.getItemName());
                    }
                }
            } else {
                UserInterfaceElementVisibilityType itemVisibility = determineVisibility(visibilityPolicy, itemPath);
                if (itemVisibility == VACANT || itemVisibility == HIDDEN) {
                    itemsToDelete.add(subDefinition.getItemName());
                }
            }
        }
        return itemsToDelete;
    }

    @NotNull
    private static UserInterfaceElementVisibilityType determineVisibility(
            List<VisibilityPolicyEntry> visibilityPolicy, UniformItemPath itemPath) {
        if (itemPath == null || itemPath.isEmpty()) {
            return AUTOMATIC;
        }
        UserInterfaceElementVisibilityType visibility = getVisibilityPolicy(visibilityPolicy, itemPath);
        if (visibility != null) {
            return visibility;
        }
        return determineVisibility(visibilityPolicy, itemPath.allExceptLast());
    }

    private static UserInterfaceElementVisibilityType getVisibilityPolicy(
            List<VisibilityPolicyEntry> visibilityPolicy, UniformItemPath itemPath) {
        for (VisibilityPolicyEntry entry : visibilityPolicy) {
            if (itemPath.equivalent(entry.path)) {
                return entry.visibility;
            }
        }
        return null;
    }

    private record VisibilityPolicyEntry(UniformItemPath path, UserInterfaceElementVisibilityType visibility) { }
}
