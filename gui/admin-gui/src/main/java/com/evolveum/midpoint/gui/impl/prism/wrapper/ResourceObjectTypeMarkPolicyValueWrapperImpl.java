/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.RelationUtil;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

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

/**
 * Wrapper for ResourceType/schemaHandling/objectType/focus/archetypeRef.
 */
public class ResourceObjectTypeMarkPolicyValueWrapperImpl<T extends Referencable> extends CreateObjectForReferenceValueWrapper<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeMarkPolicyValueWrapperImpl.class);

    public ResourceObjectTypeMarkPolicyValueWrapperImpl(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    @Override
    public ContainerPanelConfigurationType createContainerConfiguration() {
        return new ContainerPanelConfigurationType()
                .applicableForOperation(OperationTypeType.WIZARD)
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark")
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(MarkType.F_NAME))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE))
                        .item(new VirtualContainerItemSpecificationType()
                                .path(new ItemPathType(MarkType.F_DESCRIPTION))
                                .visibility(UserInterfaceElementVisibilityType.VISIBLE)))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-synchronization-inbound")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_SYNCHRONIZE,
                                        SynchronizeOperationPolicyConfigurationType.F_INBOUND)))
                        .display(new DisplayType()
                                .label("SynchronizeOperationPolicyConfigurationType.inbound"))
                        .expanded(false))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-synchronization-outbound")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_SYNCHRONIZE,
                                        SynchronizeOperationPolicyConfigurationType.F_OUTBOUND)))
                        .display(new DisplayType()
                                .label("SynchronizeOperationPolicyConfigurationType.outbound"))
                        .expanded(false))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-synchronization-membership")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_SYNCHRONIZE,
                                        SynchronizeOperationPolicyConfigurationType.F_MEMBERSHIP)))
                        .display(new DisplayType()
                                .label("SynchronizeOperationPolicyConfigurationType.membership"))
                        .expanded(false))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-synchronization-membership-inbound")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_SYNCHRONIZE,
                                        SynchronizeOperationPolicyConfigurationType.F_MEMBERSHIP,
                                        SynchronizeMembershipOperationPolicyConfigurationType.F_INBOUND)))
                        .display(new DisplayType()
                                .label("SynchronizeMembershipOperationPolicyConfigurationType.inbound"))
                        .expanded(false))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-synchronization-membership-outbound")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_SYNCHRONIZE,
                                        SynchronizeOperationPolicyConfigurationType.F_MEMBERSHIP,
                                        SynchronizeMembershipOperationPolicyConfigurationType.F_OUTBOUND)))
                        .display(new DisplayType()
                                .label("SynchronizeMembershipOperationPolicyConfigurationType.outbound"))
                        .expanded(false))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-add")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_ADD)))
                        .display(new DisplayType()
                                .label("ObjectOperationPolicyType.add"))
                        .expanded(false))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-modify")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_MODIFY)))
                        .display(new DisplayType()
                                .label("ObjectOperationPolicyType.modify"))
                        .expanded(false))
                .container(new VirtualContainersSpecificationType()
                        .identifier("new-mark-delete")
                        .path(new ItemPathType(
                                ItemPath.create(
                                        MarkType.F_OBJECT_OPERATION_POLICY,
                                        ObjectOperationPolicyType.F_DELETE)))
                        .display(new DisplayType()
                                .label("ObjectOperationPolicyType.delete"))
                        .expanded(false));
    }

    @Override
    protected <O extends ObjectType> void processBeforeCreatingPreconditionDelta(
            ObjectDetailsModels<O> newObjectModel, ModelServiceLocator serviceLocator) {
        super.processBeforeCreatingPreconditionDelta(newObjectModel, serviceLocator);

        try {
            PrismContainerWrapper<AssignmentType> assignmentContainer =
                    newObjectModel.getObjectWrapper().findContainer(ArchetypeType.F_ASSIGNMENT);

            PrismContainerValue<AssignmentType> newAssignment = new AssignmentType().asPrismContainerValue();
            newAssignment.asContainerable()
                    .targetRef(
                            SystemObjectsType.ARCHETYPE_SHADOW_POLICY_MARK.value(),
                            ArchetypeType.COMPLEX_TYPE,
                            SchemaConstants.ORG_DEFAULT);

            PrismValueWrapper<AssignmentType> newWrapper =
                    WebPrismUtil.createNewValueWrapper(assignmentContainer, newAssignment, serviceLocator);

            assignmentContainer.getValues().add((PrismContainerValueWrapper<AssignmentType>) newWrapper);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't find assignment container in " + newObjectModel.getObjectWrapper());
        }
    }

    @Override
    public boolean isHeaderOfCreateObjectVisible() {
        return true;
    }
}
