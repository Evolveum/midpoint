/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.AlreadyInitializedObject;
import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.UcfObjectFound;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;

/**
 * Represents a *lazily-initializable* resource object (e.g. an account) found by
 * {@link ResourceObjectConverter#searchResourceObjects(ProvisioningContext, ResourceObjectHandler, ObjectQuery,
 * boolean, FetchErrorReportingMethodType, OperationResult)}.
 *
 * @see ResourceObjectChange
 * @see CompleteResourceObject
 */
@Experimental
public class ResourceObjectFound extends AbstractLazilyInitializableResourceEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectFound.class);

    /** The source information. */
    @NotNull private final UcfObjectFound ucfObjectFound;

    /**
     * If success:
     *
     * - Resource object that has been found and properly initialized by
     * {@link AbstractLazilyInitializableResourceEntity#completeResourceObject(ProvisioningContext, ResourceObject,
     * boolean, OperationResult)}.
     *
     * If error (either in lower layers or in this one):
     *
     * - anything that was available (may be even almost empty object)
     *
     * Filled-in at the start of the initialization, right after {@link #globalCtx}.
     */
    private ExistingResourceObject resourceObject;

    /** State of the initialization of this object. */
    @NotNull private final InitializationState initializationState = InitializationState.created();

    /** Should the associations be fetched during initialization? Probably will be replaced by "retrieve" options. */
    private final boolean fetchAssociations;

    /** Useful beans from Resource Objects layer. */
    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    ResourceObjectFound(
            @NotNull UcfObjectFound ucfObjectFound,
            @NotNull ProvisioningContext ctx,
            boolean fetchAssociations) {
        super(ctx);
        this.ucfObjectFound = ucfObjectFound;
        this.fetchAssociations = fetchAssociations;
    }

    @Override
    public @NotNull LazilyInitializableMixin getPrerequisite() {
        return AlreadyInitializedObject.fromUcfErrorState(
                ucfObjectFound.getErrorState());
    }

    @Override
    public void initializeInternalCommon(Task task, OperationResult result) throws SchemaException, ConfigurationException {
        super.initializeInternalCommon(task, result);
        try {
            resourceObject = globalCtx.adoptUcfResourceObject(
                    ucfObjectFound.getResourceObject());
        } catch (Throwable t) {
            resourceObject = getMinimalResourceObject();
            throw t;
        }
    }

    @Override
    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result) throws CommonException {
        completeResourceObject(
                globalCtx, resourceObject, fetchAssociations, result);
    }

    @Override
    public void initializeInternalForPrerequisiteError(Task task, OperationResult result) throws CommonException {
        ResourceObjectDefinition definition = globalCtx.getObjectDefinitionRequired();
        ResourceAttributeContainer attrContainer = ShadowUtil.getOrCreateAttributesContainer(resourceObject.bean, definition);
        b.fakeIdentifierGenerator.addFakePrimaryIdentifierIfNeeded(attrContainer, getPrimaryIdentifierValue(), definition);
    }

    @Override
    public void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result) {
        throw new IllegalStateException("UCF does not signal 'not applicable' state");
    }

    public ExistingResourceObject getResourceObject() {
        return resourceObject;
    }

    @Override
    public ResourceObjectDefinition getResourceObjectDefinition() {
        if (resourceObject != null) {
            return resourceObject.getObjectDefinition();
        } else {
            return getEffectiveCtx().getObjectDefinition();
        }
    }

    public Object getPrimaryIdentifierValue() {
        return ucfObjectFound.getPrimaryIdentifierValue();
    }

//    public @NotNull ResourceObjectIdentification.WithPrimary getPrimaryIdentification() throws SchemaException {
//        return resourceObject.getPrimaryIdentification();
//    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public void checkConsistence() {
        // nothing here now
    }

    @Override
    boolean objectDoesExist() {
        // We assume that the object exists. The call to doesExist here is just for 100% certainty.
        return resourceObject == null || resourceObject.doesExist();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + resourceObject +
                ", initializationState=" + initializationState +
                '}';
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        if (resourceObject != null) {
            DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "UCF object error state", ucfObjectFound.getErrorState(), indent + 1);
        } else {
            DebugUtil.debugDumpWithLabelLn(sb, "ucfObjectFound", ucfObjectFound, indent + 1);
        }
        DebugUtil.debugDumpWithLabel(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }
}
