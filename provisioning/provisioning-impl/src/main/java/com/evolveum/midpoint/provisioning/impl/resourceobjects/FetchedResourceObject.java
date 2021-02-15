/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.InitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.SkipProcessingException;
import com.evolveum.midpoint.provisioning.ucf.api.FetchedUcfObject;
import com.evolveum.midpoint.provisioning.util.ProcessingState;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Represents a resource object (e.g. an account) fetched by an UCF operation like
 * {@link ResourceObjectConverter#searchResourceObjects(ProvisioningContext, ResultHandler, ObjectQuery, boolean, FetchErrorReportingMethodType, OperationResult)}
 * or (in future) {@link ResourceObjectConverter#getResourceObject(ProvisioningContext, Collection, boolean, OperationResult)}.
 */
@SuppressWarnings("JavadocReference")
@Experimental
public class FetchedResourceObject implements InitializableMixin {

    private static final Trace LOGGER = TraceManager.getTrace(FetchedResourceObject.class);

    /**
     * Resource object as it is known at this point.
     *
     * TODO what properties does it have?
     */
    @NotNull private final PrismObject<ShadowType> resourceObject;

    /**
     * Real value of the object primary identifier (e.g. ConnId UID).
     * Usually not null (e.g. in ConnId 1.x), but this can change in the future.
     *
     * See {@link FetchedUcfObject#primaryIdentifierValue}.
     */
    private final Object primaryIdentifierValue;

    @NotNull private final ProcessingState processingState;

    private final InitializationContext ictx;

    private final ResourceObjectsLocalBeans localBeans;

    public FetchedResourceObject(FetchedUcfObject ucfObject, ResourceObjectConverter converter,
            ProvisioningContext ctx, boolean fetchAssociations) {
        this.resourceObject = ucfObject.getResourceObject().clone();
        this.primaryIdentifierValue = ucfObject.getPrimaryIdentifierValue();
        this.processingState = ProcessingState.fromUcfErrorState(ucfObject.getErrorState());
        this.ictx = new InitializationContext(ctx, fetchAssociations);
        this.localBeans = converter.getLocalBeans();
    }

    public void initializeInternal(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        localBeans.resourceObjectConverter
                .postProcessResourceObjectRead(ictx.ctx, resourceObject, ictx.fetchAssociations, result);
    }

    @Override
    public void skipInitialization(Task task, OperationResult result) throws CommonException, SkipProcessingException, EncryptionException {
        addFakePrimaryIdentifierIfNeeded();
    }

    private void addFakePrimaryIdentifierIfNeeded() throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        RefinedObjectClassDefinition objectClassDef = ictx.ctx.getObjectClassDefinition();
        ResourceAttributeContainer attrContainer = ShadowUtil.getOrCreateAttributesContainer(resourceObject, objectClassDef);
        localBeans.fakeIdentifierGenerator
                .addFakePrimaryIdentifierIfNeeded(attrContainer, primaryIdentifierValue, objectClassDef);
    }

    public @NotNull PrismObject<ShadowType> getResourceObject() {
        return resourceObject;
    }

    public Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public @NotNull ProcessingState getProcessingState() {
        return processingState;
    }

    @Override
    public void checkConsistence() {
        // TODO
    }

    @Override
    public String toString() {
        return "FetchedResourceObject{" +
                "resourceObject=" + resourceObject +
                ", primaryIdentifierValue=" + primaryIdentifierValue +
                ", processingState=" + processingState +
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
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierValue), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "processingState", String.valueOf(processingState), indent + 1);
        return sb.toString();
    }

    private static class InitializationContext {
        private final ProvisioningContext ctx;
        private final boolean fetchAssociations;

        private InitializationContext(ProvisioningContext ctx, boolean fetchAssociations) {
            this.ctx = ctx;
            this.fetchAssociations = fetchAssociations;
        }
    }
}
