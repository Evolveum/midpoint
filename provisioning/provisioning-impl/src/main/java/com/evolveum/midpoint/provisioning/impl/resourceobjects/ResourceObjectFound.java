/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.AlreadyInitializedObject;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.InitializableObjectMixin;
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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents a resource object (e.g. an account) found by the {@link ResourceObjectConverter#searchResourceObjects(
 * ProvisioningContext, ResourceObjectHandler, ObjectQuery, boolean, FetchErrorReportingMethodType, OperationResult)} method.
 *
 * See also {@link ResourceObjectChange}.
 *
 * In the future we might create also analogous data structure for objects retrieved by
 * {@link ResourceObjectConverter#getResourceObject(ProvisioningContext, Collection, ShadowType, boolean, OperationResult)}
 * method.
 */
@Experimental
public class ResourceObjectFound extends AbstractResourceEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectFound.class);

    /**
     * Resource object that has been found.
     *
     * 1. When created: Object as received from UCF.
     *
     * 2. When initialized-OK: The same object, with:
     *    a. protected flag set,
     *    b. exists flag not null,
     *    c. simulated activation done,
     *    d. associations fetched (if requested).
     *
     * 3. When initialized-error:
     *    a. has primary identifier present, assuming: object class known + primary identifier value known.
     *
     * 4. When initialized-not-applicable:
     *    a. Nothing guaranteed.
     *
     * 5. If initialization failed:
     *    a. Nothing guaranteed.
     */
    @NotNull private final ResourceObject resourceObject;

    /** State of the initialization of this object. */
    @NotNull private final InitializationState initializationState = InitializationState.created();

    /** Status of the UCF-level processing of the object. It serves as a prerequisite for the processing on this level. */
    @NotNull private final AlreadyInitializedObject ucfObjectStatus;

    /** Should the associations be fetched during initialization? Probably will be replaced by "retrieve" options. */
    private final boolean fetchAssociations;

    /** Useful beans from Resource Objects layer. */
    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    ResourceObjectFound(
            UcfObjectFound ucfObject,
            ProvisioningContext ctx,
            boolean fetchAssociations) {
        super(ctx);
        this.resourceObject = ResourceObject.from(ucfObject);
        this.ucfObjectStatus = AlreadyInitializedObject.fromUcfErrorState(ucfObject.getErrorState());
        this.fetchAssociations = fetchAssociations;
    }

    @Override
    public @NotNull InitializableObjectMixin getPrerequisite() {
        return ucfObjectStatus;
    }

    @Override
    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result) throws CommonException {
        b.resourceObjectConverter.postProcessResourceObjectRead(globalCtx, resourceObject, fetchAssociations, result);
    }

    @Override
    public void initializeInternalForPrerequisiteError(Task task, OperationResult result) throws CommonException {
        addFakePrimaryIdentifierIfNeeded();
    }

    @Override
    public void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result) {
        throw new IllegalStateException("UCF does not signal 'not applicable' state");
    }

    private void addFakePrimaryIdentifierIfNeeded() throws SchemaException {
        ResourceObjectDefinition definition = globalCtx.getObjectDefinitionRequired();
        ResourceAttributeContainer attrContainer =
                ShadowUtil.getOrCreateAttributesContainer(resourceObject.getBean(), definition);
        b.fakeIdentifierGenerator.addFakePrimaryIdentifierIfNeeded(attrContainer, getPrimaryIdentifierValue(), definition);
    }

    public @NotNull ResourceObject getResourceObject() {
        return resourceObject;
    }

    public Object getPrimaryIdentifierValue() {
        return resourceObject.getPrimaryIdentifierValue();
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public void checkConsistence() {
        // nothing here now
    }

    @Override
    public String toString() {
        return "FetchedResourceObject{" +
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
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "ucfProcessingStatus", String.valueOf(ucfObjectStatus), indent + 1);
        return sb.toString();
    }
}
