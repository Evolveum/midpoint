/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;

/**
 * Wrapper for ResourceType/schemaHandling/objectType/focus/archetypeRef.
 */
public class ResourceObjectTypeArchetypeValueWrapperImpl<T extends Referencable> extends CreateObjectForReferenceValueWrapper<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeArchetypeValueWrapperImpl.class);
    private static final ItemName F_CREATE_INDUCEMENT = ItemName.interned(ObjectFactory.NAMESPACE, "createInducement");
    private static final ItemName F_CREATE_MEMBERSHIP_FOR = ItemName.interned(ObjectFactory.NAMESPACE, "createMembershipFor");

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
                                .path(new ItemPathType(F_CREATE_INDUCEMENT))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(F_CREATE_MEMBERSHIP_FOR))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE)))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-archetype-display")
                        .display(new DisplayType()
                                .label("ArchetypePolicyType.display"))
                        .expanded(false)
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_LABEL)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_SINGULAR_LABEL)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_PLURAL_LABEL)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_TOOLTIP)))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(
                                        ItemPath.create(
                                                ArchetypeType.F_ARCHETYPE_POLICY,
                                                ArchetypePolicyType.F_DISPLAY,
                                                DisplayType.F_HELP)))
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
    protected <O extends ObjectType> PrismObject<O> createNewPrismObject(OperationResult result) throws SchemaException {
        PrismObject<O> newObject = super.createNewPrismObject(result);
        PrismContainerDefinition<O> def = newObject.getDefinition().clone();
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

        PrismPropertyDefinitionImpl<Boolean> createInducementDef = new PrismPropertyDefinitionImpl<>(
                F_CREATE_INDUCEMENT,
                DOMUtil.XSD_BOOLEAN,
                false);
        createInducementDef.mutator().setDisplayName("ResourceObjectTypeArchetypeValueWrapperImpl.createInducement");
        createInducementDef.mutator().setMinOccurs(0);
        createInducementDef.mutator().setHelp("ResourceObjectTypeArchetypeValueWrapperImpl.createInducement.help");
        createInducementDef.mutator().setOperational(true);
        def.getComplexTypeDefinition().mutator().add(createInducementDef);

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (objectType != null && objectType.getRealValue() != null && ShadowKindType.ENTITLEMENT == objectType.getRealValue().getKind()) {
            PrismPropertyDefinitionImpl<String> createMembershipForDef = new PrismPropertyDefinitionImpl<>(
                    F_CREATE_MEMBERSHIP_FOR,
                    DOMUtil.XSD_STRING);
            createMembershipForDef.mutator().setDisplayName("ResourceObjectTypeArchetypeValueWrapperImpl.createMembershipFor");
            createMembershipForDef.mutator().setMinOccurs(0);
            createMembershipForDef.mutator().setHelp("ResourceObjectTypeArchetypeValueWrapperImpl.createMembershipFor.help");
            createMembershipForDef.mutator().setAllowedValues(createSuggestedValuesForObjectTypes(objectType));
            createMembershipForDef.mutator().setOperational(true);
            def.getComplexTypeDefinition().mutator().add(createMembershipForDef);
        }

        newObject.setDefinition(def);

        return newObject;
    }

    private Collection<? extends DisplayableValue<String>> createSuggestedValuesForObjectTypes(
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType) {
        return ((PrismContainerWrapper<ResourceObjectTypeDefinitionType>) objectType.getParent()).getValues().stream()
                .filter(value -> value.getRealValue().getKind() != objectType.getRealValue().getKind()
                        || !StringUtils.equals(
                        ResourceTypeUtil.fillDefault(objectType.getRealValue().getIntent()),
                        ResourceTypeUtil.fillDefault(value.getRealValue().getIntent())))
                .map(value -> new ObjectTypeDisplayableValue(value.getRealValue()))
                .toList();
    }

    @Override
    protected <O extends ObjectType> void processBeforeCreatingPreconditionDelta(
            ObjectDetailsModels<O> newObjectModel, ModelServiceLocator serviceLocator) {
        try {
            super.processBeforeCreatingPreconditionDelta(newObjectModel, serviceLocator);

            QName focusTypeBean = null;

            PrismContainerValueWrapper resourceFocusValue = getParentContainerValue(ResourceObjectFocusSpecificationType.class);

            if (resourceFocusValue != null) {
                try {
                    PrismPropertyWrapper focusType = resourceFocusValue.findProperty(
                            ResourceObjectFocusSpecificationType.F_TYPE);
                    if (focusType != null) {
                        PrismValueWrapper focusTypeValue = focusType.getValue();
                        if (focusTypeValue != null) {
                            focusTypeBean = (QName) focusTypeValue.getRealValue();
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

            try {
                PrismPropertyWrapper<Boolean> createInducement = newObjectModel.getObjectWrapper().findProperty(F_CREATE_INDUCEMENT);
                if (Boolean.TRUE.equals(createInducement.getValue().getRealValue())) {

                    PrismObjectWrapper<ResourceType> resource = getParent().findObjectWrapper();
                    PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = getParentContainerValue(ResourceObjectTypeDefinitionType.class);

                    if (resource != null && objectType != null && objectType.getRealValue() != null) {

                        @NotNull String intent = ResourceTypeUtil.fillDefault(objectType.getRealValue().getIntent());

                        PrismContainerWrapper<AssignmentType> inducementContainer =
                                newObjectModel.getObjectWrapper().findContainer(ArchetypeType.F_INDUCEMENT);

                        PrismContainerValue<AssignmentType> newInducement = new AssignmentType().asPrismContainerValue();

                        newInducement.asContainerable()
                                .construction(
                                        new ConstructionType()
                                                .resourceRef(resource.getOid(), ResourceType.COMPLEX_TYPE)
                                                .kind(objectType.getRealValue().getKind())
                                                .intent(intent));

                        if (focusTypeBean != null) {
                            newInducement.asContainerable().focusType(focusTypeBean);
                        }

                        PrismValueWrapper<AssignmentType> newWrapper =
                                WebPrismUtil.createNewValueWrapper(inducementContainer, newInducement, serviceLocator);

                        inducementContainer.getValues().add((PrismContainerValueWrapper<AssignmentType>) newWrapper);
                    }
                }
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't find property " + F_CREATE_INDUCEMENT.getLocalPart() + " in " + resourceFocusValue);
            }

            try {
                PrismPropertyWrapper<String> createMembershipFor = newObjectModel.getObjectWrapper().findProperty(F_CREATE_MEMBERSHIP_FOR);
                if (createMembershipFor != null && createMembershipFor.getValue().getRealValue() != null) {

                    PrismObjectWrapper<ResourceType> resource = getParent().findObjectWrapper();

                    if (resource != null) {

                        PrismContainerWrapper<AssignmentType> inducementContainer =
                                newObjectModel.getObjectWrapper().findContainer(ArchetypeType.F_INDUCEMENT);

                        PrismContainerValue<AssignmentType> newInducement = new AssignmentType().asPrismContainerValue();

                        ShadowKindType kind = parseKind(createMembershipFor.getValue().getRealValue());
                        String intent = parseIntent(createMembershipFor.getValue().getRealValue());

                        newInducement.asContainerable()
                                .construction(
                                        new ConstructionType()
                                                .resourceRef(resource.getOid(), ResourceType.COMPLEX_TYPE)
                                                .kind(kind)
                                                .intent(intent))
                                .order(2);

                        ResourceObjectTypeDefinitionType objectType = ResourceTypeUtil.findObjectTypeDefinition(resource.getObject(), kind, intent);
                        if (objectType != null && objectType.getFocus() != null) {
                            newInducement.asContainerable().focusType(objectType.getFocus().getType());
                        }

                        PrismValueWrapper<AssignmentType> newWrapper =
                                WebPrismUtil.createNewValueWrapper(inducementContainer, newInducement, serviceLocator);

                        inducementContainer.getValues().add((PrismContainerValueWrapper<AssignmentType>) newWrapper);
                    }
                }
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't find property " + F_CREATE_INDUCEMENT.getLocalPart() + " in " + resourceFocusValue);
            }

        } finally {

            PrismObjectDefinition<O> originalDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(
                    newObjectModel.getObjectWrapper().getTypeName());
            newObjectModel.getObjectWrapper().getObjectOld().setDefinition(originalDef);
            newObjectModel.getObjectWrapper().getObject().setDefinition(originalDef);
        }
    }

    private ShadowKindType parseKind(String value) {
        String kind = value.substring(0, value.indexOf(":"));
        return ShadowKindType.valueOf(kind);
    }

    private String parseIntent(String value) {
        return value.substring(value.indexOf(":") + 1);
    }

    @Override
    public boolean isHeaderOfCreateObjectVisible() {
        return true;
    }

    private class ObjectTypeDisplayableValue implements DisplayableValue<String>, Serializable {

        private final String displayName;
        private final String value;

        private ObjectTypeDisplayableValue(ResourceObjectTypeDefinitionType objectType) {
            this.displayName = GuiDisplayNameUtil.getDisplayName(objectType);
            this.value = objectType.getKind().name() + ":" +
                    ResourceTypeUtil.fillDefault(objectType.getIntent());
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getLabel() {
            return displayName;
        }

        @Override
        public String getDescription() {
            return null;
        }
    }

    @Override
    protected WrapperContext createWrapperContextForNewObject(WrapperContext wrapperContext) {
        wrapperContext.setCreateOperational(true);
        return wrapperContext;
    }
}
