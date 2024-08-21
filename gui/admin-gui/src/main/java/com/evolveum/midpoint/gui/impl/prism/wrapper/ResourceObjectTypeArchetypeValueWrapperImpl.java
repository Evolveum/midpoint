/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Wrapper for ResourceType/schemaHandling/objectType/focus/archetypeRef.
 */
public class ResourceObjectTypeArchetypeValueWrapperImpl<T extends Referencable> extends CreateObjectForReferenceValueWrapper<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeArchetypeValueWrapperImpl.class);

    public ResourceObjectTypeArchetypeValueWrapperImpl(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    @Override
    public ContainerPanelConfigurationType createContainerConfiguration() {
        return new ContainerPanelConfigurationType()
                .applicableForOperation(OperationTypeType.WIZARD)
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-archetype")
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(ArchetypeType.F_SUPER_ARCHETYPE_REF))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(ArchetypeType.F_NAME))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(ArchetypeType.F_DESCRIPTION))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_ICON,
                                                IconType.F_CSS_CLASS)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_ICON,
                                                IconType.F_COLOR)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                );
    }

    @Override
    protected <O extends ObjectType> PrismObject<? extends ObjectType> createNewPrismObject(OperationResult result) throws SchemaException {
        PrismObject<? extends ObjectType> newObject = super.createNewPrismObject(result);
        PrismObjectDefinition<? extends ObjectType> def = newObject.getDefinition();
        PrismContainerDefinition<Containerable> iconContainerDefinition = def.findContainerDefinition(ItemPath.create(
                ArchetypeType.F_ARCHETYPE_POLICY,
                ArchetypePolicyType.F_DISPLAY,
                DisplayType.F_ICON));
        @NotNull PrismPropertyDefinition<Object> cssDef = iconContainerDefinition.findPropertyDefinition(IconType.F_CSS_CLASS).clone();
        cssDef.mutator().setDisplayName("ResourceObjectTypeArchetypeValueWrapperImpl.icon");
        iconContainerDefinition.replaceDefinition(IconType.F_CSS_CLASS, cssDef);

        @NotNull PrismReferenceDefinition superArchetype = def.findReferenceDefinition(ArchetypeType.F_SUPER_ARCHETYPE_REF).clone();
        superArchetype.mutator().setDisplayName("ResourceObjectTypeArchetypeValueWrapperImpl.superArchetype");
        superArchetype.mutator().setDisplayOrder(0);
        def.replaceDefinition(ArchetypeType.F_SUPER_ARCHETYPE_REF, superArchetype);

        PrismObjectWrapper<ObjectType> objectWrapper = getParent().findObjectWrapper();
        StringBuilder name = new StringBuilder(WebComponentUtil.getDisplayNameOrName(objectWrapper.getObject()));

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> resourceObjectType =
                getParent().getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (resourceObjectType != null && StringUtils.isNotEmpty(resourceObjectType.getRealValue().getIntent())) {
            name.append(" - ").append(resourceObjectType.getRealValue().getIntent());
        }

        if (!name.isEmpty()) {
            newObject.asObjectable().name(name.toString());
        }

        return newObject;
    }

    @Override
    protected void processBeforeCreatingPreconditionDelta(
            ObjectDetailsModels<? extends ObjectType> newObjectModel, ModelServiceLocator serviceLocator) {
        super.processBeforeCreatingPreconditionDelta(newObjectModel, serviceLocator);

        PrismContainerValueWrapper resourceFocusValue = getParentContainerValue(ResourceObjectFocusSpecificationType.class);
        if (resourceFocusValue != null) {
            try {
                PrismPropertyWrapper focusType = resourceFocusValue.findProperty(
                        ResourceObjectFocusSpecificationType.F_TYPE);
                if (focusType != null) {
                    PrismValueWrapper focusTypeValue = focusType.getValue();
                    if (focusTypeValue != null) {
                        QName focusTypeBean = (QName) focusTypeValue.getRealValue();
                        if (focusTypeBean != null) {

                            PrismContainerWrapper<AssignmentType> assignmentContainer =
                                    newObjectModel.getObjectWrapper().findContainer(ArchetypeType.F_ASSIGNMENT);

                            PrismContainerValue<AssignmentType> newAssignment = new AssignmentType().asPrismContainerValue();
                            newAssignment.asContainerable()
                                    .assignmentRelation(
                                            new AssignmentRelationType()
                                                    .holderType(focusTypeBean));

                            PrismValueWrapper<AssignmentType> newWrapper =
                                    WebPrismUtil.createNewValueWrapper(assignmentContainer, newAssignment, serviceLocator);

                            assignmentContainer.getValues().add((PrismContainerValueWrapper<AssignmentType>) newWrapper);
                        }
                    }
                }
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't find type in " + resourceFocusValue);
            }
        }
    }
}
